package com.project.thermallogistics.service;

import com.project.thermallogistics.model.entity.CoolingEquipment;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.VenueType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

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
