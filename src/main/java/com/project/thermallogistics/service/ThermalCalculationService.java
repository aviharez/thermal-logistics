package com.project.thermallogistics.service;

import com.project.thermallogistics.model.dto.ThermalCalculationResult;
import com.project.thermallogistics.model.entity.CoolingEquipment;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.VenueType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Core thermal physics engine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThermalCalculationService {

    private static final double LATENT_HEAT_FUSION_J_KG = 334_000.0;
    private static final double ICE_DENSITY_KG_M3 = 917.0;
    private static final double HEAT_TRANSFER_COEFF_W_M2_K = 10.0;
    private static final double LITERS_TO_M3 = 0.001;

    // at 40% mass loss the sculpture loses structural integrity
    static final double STRUCTURAL_FAILURE_MASS_LOSS_RATIO = 0.40;
    // first 10% of melt = peak visual clarity window
    static final double PEAK_CLARITY_MASS_LOSS_RATIO = 0.10;

    private final WeatherService weatherService;

    public ThermalCalculationResult calculate(IceProject project) {
        double ambientTemp = weatherService.getForecastTemperatureForEvent(project);
        String weatherDesc = weatherService.getForecastSummary(project);
        return calculateWithTemperature(project, ambientTemp, weatherDesc);
    }

    public ThermalCalculationResult calculateWithTemperature(IceProject project, double ambientTempCelsius, String weatherDescription) {
        log.debug("Calculating melt rate for project '{}' at ambient {} C", project.getName(), ambientTempCelsius);

        Map<String, Double> coolingBreakdown = buildCoolingBreakdown(project);
        double totalCoolingReduction = coolingBreakdown.values().stream().mapToDouble(Double::doubleValue).sum();
        double effectiveTemp = Math.max(0.1, ambientTempCelsius - totalCoolingReduction);

        double volumeM3 = project.getSculptureVolumeLiters() * LITERS_TO_M3;
        double surfaceAreaM2 = sphereSurfaceArea(volumeM3);
        double initialMassKg = volumeM3 * ICE_DENSITY_KG_M3;

        // Heat transfer rate [W] -> melt rate [kg/s] -> [kg/h]
        double heatTransferWatts = HEAT_TRANSFER_COEFF_W_M2_K * surfaceAreaM2 * effectiveTemp;
        double meltRateKgPerSecond = heatTransferWatts / LATENT_HEAT_FUSION_J_KG;
        double meltRateKgPerHour = meltRateKgPerSecond * 3600.0 * project.getIceType().getMeltRateMultiplier();

        double totalMeltTimeHours = initialMassKg / meltRateKgPerHour;
        double timeToStructuralFailureHours = (initialMassKg * STRUCTURAL_FAILURE_MASS_LOSS_RATIO) / meltRateKgPerHour;
        double peakClarityWindowHours = (initialMassKg * PEAK_CLARITY_MASS_LOSS_RATIO) / meltRateKgPerHour;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime projectedFailure = now.plusMinutes((long) (timeToStructuralFailureHours * 60));

        double eventDurationHours = Duration.between(project.getEventStartTime(), project.getEventEndTime()).toMinutes() / 60.0;
        boolean eventExceedsSafeWindow = eventDurationHours > timeToStructuralFailureHours;

        log.debug("Melt result: effectiveTemp={} C, meltRate={}kg/h, failureIn={}h",
                effectiveTemp, String.format("%.3f", meltRateKgPerHour), timeToStructuralFailureHours);

        return ThermalCalculationResult.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .ambientTemperatureCelsius(round2(ambientTempCelsius))
                .effectiveTemperatureCelsius(round2(effectiveTemp))
                .surfaceAreaM2(round4(surfaceAreaM2))
                .initialMassKg(round2(initialMassKg))
                .meltRateKgPerHour(round3(meltRateKgPerHour))
                .totalMeltTimeHours(round2(totalMeltTimeHours))
                .timeToStructuralFailureHours(round2(timeToStructuralFailureHours))
                .peakClarityWindowHours(round2(peakClarityWindowHours))
                .safeDisplayWindowHours(round2(timeToStructuralFailureHours))
                .coolingBreakdown(coolingBreakdown)
                .weatherDescription(weatherDescription)
                .calculatedAt(now)
                .projectedFailureTime(projectedFailure)
                .eventExceedsSafeWindow(eventExceedsSafeWindow)
                .build();
    }

    public double calculateEffectiveTemperature(IceProject project, double ambientTemp) {
        double reduction = buildCoolingBreakdown(project).values().stream().mapToDouble(Double::doubleValue).sum();
        return Math.max(0.1, ambientTemp - reduction);
    }

    public double calculateMeltRateKgPerHour(IceProject project, double effectiveTemp) {
        double volumeM3 = project.getSculptureVolumeLiters() * LITERS_TO_M3;
        double surfaceAreM2 = sphereSurfaceArea(volumeM3);
        double initialMassKg = volumeM3 * ICE_DENSITY_KG_M3;
        double heatTransferWatts = HEAT_TRANSFER_COEFF_W_M2_K * surfaceAreM2 * effectiveTemp;
        double meltRateKgPerSecond = heatTransferWatts / LATENT_HEAT_FUSION_J_KG;
        return meltRateKgPerSecond * 3600.0 * project.getIceType().getMeltRateMultiplier();
    }

    public double calculateTimeToStructuralFailureHours(IceProject project, double ambientTemp) {
        double effectiveTemp = calculateEffectiveTemperature(project, ambientTemp);
        double volumeM3 = project.getSculptureVolumeLiters() * LITERS_TO_M3;
        double initialMassKg = volumeM3 * ICE_DENSITY_KG_M3;
        double meltRate = calculateMeltRateKgPerHour(project, effectiveTemp);
        return (initialMassKg * STRUCTURAL_FAILURE_MASS_LOSS_RATIO) / meltRate;
    }

    // Surface area of a sphere with equivalent volume
    double sphereSurfaceArea(double volumeM3) {
        return 4.0 * Math.PI * Math.pow((3.0 * volumeM3) / (4.0 * Math.PI), 2.0 / 3.0);
    }

    private Map<String, Double> buildCoolingBreakdown(IceProject project) {
        Map<String, Double> breakdown = new LinkedHashMap<>();

        if (project.getVenueType() == VenueType.INDOOR_COOLED || project.getVenueType() == VenueType.INDOOR_UNCOOLED) {
            breakdown.put("venue_ac", project.getIndoorCoolingOffset());
        }

        for (CoolingEquipment equipment : project.getCoolingEquipments()) {
            String key = equipment.getEquipmentType().name().toLowerCase();
            double reduction = equipment.getEquipmentType().getTemperatureReductionCelsius() * equipment.getQuantity();
            breakdown.merge(key, reduction, Double::sum);
        }

        return breakdown;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
