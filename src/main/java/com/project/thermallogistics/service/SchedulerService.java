package com.project.thermallogistics.service;

import com.project.thermallogistics.model.dto.InstallationSchedule;
import com.project.thermallogistics.model.dto.ThermalCalculationResult;
import com.project.thermallogistics.model.entity.CoolingEquipment;
import com.project.thermallogistics.model.entity.IceProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final ThermalCalculationService thermalService;
    private final IceProjectService projectService;
    private final WeatherService weatherService;

    @Value("${thermal.safety-buffer-hours:0.5}")
    private double safetyBufferHours;

    public InstallationSchedule generateSchedule(Long projectId) {
        IceProject project = projectService.findOrThrow(projectId);
        return generateScheduleForProject(project);
    }

    public InstallationSchedule generateScheduleForProject(IceProject project) {
        log.info("Generating installation schedule for project '{}' (id={})", project.getName(), project.getId());

        double forecastTemp = weatherService.getForecastTemperatureForEvent(project);
        String weatherSummary = weatherService.getForecastSummary(project);
        ThermalCalculationResult thermal = thermalService.calculateWithTemperature(project, forecastTemp, weatherSummary);

        int setupMinutes = resolveSetupLeadTime(project.getSculptureVolumeLiters());
        LocalDateTime eventStart = project.getEventStartTime();

        LocalDateTime setupArrival = eventStart.minusMinutes(setupMinutes + 30L);
        LocalDateTime installationTime = eventStart.minusMinutes(setupMinutes);
        LocalDateTime peakClarityStart = installationTime;
        LocalDateTime peakClarityEnd = installationTime.plusMinutes((long) (thermal.getPeakClarityWindowHours() * 60));
        LocalDateTime structuralFailureTime = installationTime.plusMinutes((long) (thermal.getTimeToStructuralFailureHours() * 60));
        LocalDateTime mandatoryTeardown = structuralFailureTime.minusMinutes((long) (safetyBufferHours * 60));

        double eventDurationHours = Duration.between(project.getEventStartTime(), project.getEventEndTime()).toMinutes() / 60.0;
        boolean eventFitsInSafeWindow = eventDurationHours <= thermal.getTimeToStructuralFailureHours();

        List<String> recommendations = buildRecommendations(project, thermal, setupMinutes, eventFitsInSafeWindow);

        return InstallationSchedule.builder()
                .projectId(project.getId())
                .projectName(project.getName())
                .setupArrivalTime(setupArrival)
                .recommendedInstallationTime(installationTime)
                .peakClarityStart(peakClarityStart)
                .peakClarityEnd(peakClarityEnd)
                .estimatedStructuralFailureTime(structuralFailureTime)
                .mandatoryTeardownTime(mandatoryTeardown)
                .estimatedMeltTimeHours(thermal.getTotalMeltTimeHours())
                .peakClarityDurationHours(thermal.getPeakClarityWindowHours())
                .safeDisplayWindowHours(thermal.getTimeToStructuralFailureHours())
                .forecastTemperatureCelsius(thermal.getAmbientTemperatureCelsius())
                .effectiveTemperatureCelsius(thermal.getEffectiveTemperatureCelsius())
                .eventFitsInSafeWindow(eventFitsInSafeWindow)
                .setupLeadTimeMinutes(setupMinutes)
                .recommendations(recommendations)
                .weatherSummary(weatherSummary)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private int resolveSetupLeadTime(double volumeLiters) {
        if (volumeLiters < 50) return 30;
        if (volumeLiters < 200) return 60;
        if (volumeLiters < 500) return 90;
        return 120;
    }

    private List<String> buildRecommendations(IceProject project, ThermalCalculationResult thermal, int setupMinutes, boolean fits) {
        List<String> recs = new ArrayList<>();

        recs.add(String.format("Arrive at venue %d minutes before installation (setup lead time for %.0fL sculpture).",
                setupMinutes + 30, project.getSculptureVolumeLiters()));

        if (!fits) {
            recs.add("CRITICAL: Event duration exceeds the safe display window. " +
                    "Consider a mid-event ice replacement or reduce sculpture volume.");
        }

        if (thermal.getEffectiveTemperatureCelsius() > 20) {
            recs.add("Ambient temperature is high. Deploy all available dry ice boosters to extend display life.");
        } else if (thermal.getEffectiveTemperatureCelsius() > 15) {
            recs.add("Moderate temperatures detected. Ensure fans are operational before guest arrival.");
        }

        if (project.getCoolingEquipments().isEmpty()) {
            recs.add("No cooling equipment assigned. Adding at 1 fan and 1 dry ice booster is strongly recommended.");
        }

        long dryIceCount = project.getCoolingEquipments().stream()
                .filter(e -> e.getEquipmentType().name().equals("DRY_ICE_BOOSTER"))
                .mapToLong(CoolingEquipment::getQuantity)
                .sum();

        if (dryIceCount == 0 && thermal.getAmbientTemperatureCelsius() > 18) {
            recs.add("No dry ice booster configured. At forecast temperatures, adding 2 units would extend " +
                    "the safe window by approximately " +
                    String.format("%.1f", (14.0 / thermal.getMeltRateKgPerHour())) + " hours.");
        }

        if (thermal.getPeakClarityWindowHours() < 1.0) {
            recs.add("Peak clarity window is under 1 hour. Schedule VIP photography immediately after installation.");
        } else {
            recs.add(String.format("Peak clarity window lasts %.1f hours from installation. " +
                    "Ideal for guest arrival and photography.", thermal.getPeakClarityWindowHours()));
        }

        recs.add(String.format("Mandatory teardown no later than %s to prevent structural failure.",
                project.getEventStartTime()
                        .minusMinutes(setupMinutes)
                        .plusMinutes((long) ((thermal.getTimeToStructuralFailureHours() - safetyBufferHours) * 60))
                        .toString()));

        return recs;
    }
}
