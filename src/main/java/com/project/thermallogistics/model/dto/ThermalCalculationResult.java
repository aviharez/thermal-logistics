package com.project.thermallogistics.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Thermal melt calculation result for an Ice Project")
public class ThermalCalculationResult {

    @Schema(description = "Project identifier")
    private Long projectId;

    @Schema(description = "Project name")
    private String projectName;

    @Schema(description = "Raw ambient temperature from weather data (C)")
    private Double ambientTemperatureCelsius;

    @Schema(description = "Effective temperature after all cooling adjustment (C)")
    private Double effectiveTemperatureCelsius;

    @Schema(description = "Estimated surface area of the sculpture (m2)")
    private Double surfaceAreaM2;

    @Schema(description = "Initial ice mass (kg)")
    private Double initialMassKg;

    @Schema(description = "Current melt rate (kg/hour)")
    private Double meltRateKgPerHour;

    @Schema(description = "Total estimated time until fully melted (hours)")
    private Double totalMeltTimeHours;

    @Schema(description = "Time until structural integrity is compromised at 40% mass loss (hours)")
    private Double timeToStructuralFailureHours;

    @Schema(description = "Duration of the peak visual clarity window (hours)")
    private Double peakClarityWindowHours;

    @Schema(description = "Safe display window, time before mandatory removal (hours)")
    private Double safeDisplayWindowHours;

    @Schema(description = "Breakdown of cooling contributions by source (C reduction per source)")
    private Map<String, Double> coolingBreakdown;

    @Schema(description = "Weather condition summary")
    private String weatherDescription;

    @Schema(description = "Timestamp of this calculation")
    private LocalDateTime calculatedAt;

    @Schema(description = "Projected structural failure time given current weather")
    private LocalDateTime projectedFailureTime;

    @Schema(description = "Whether the event duration exceeds the safe display window")
    private Boolean eventExceedsSafeWindow;
}
