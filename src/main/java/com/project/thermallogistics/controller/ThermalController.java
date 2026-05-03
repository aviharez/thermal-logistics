package com.project.thermallogistics.controller;

import com.project.thermallogistics.model.dto.ThermalCalculationResult;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.service.IceProjectService;
import com.project.thermallogistics.service.ThermalCalculationService;
import com.project.thermallogistics.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/thermal")
@RequiredArgsConstructor
@Tag(name = "Thermal Calculations", description = "Physics-based melt rate and thermal state calculations")
public class ThermalController {

    private final ThermalCalculationService thermalService;
    private final IceProjectService projectService;
    private final WeatherService weatherService;

    @GetMapping("/{projectId}/calculate")
    @Operation(summary = "Calculate thermal state for a project",
            description = """
                        Fetches live weather data for the project's venue location and computes the full thermal
                        state including: effective temperature, melt rate (kg/h), time to structural failure,
                        peak clarity window, and whether the event duration fits within the safe display window.
                    """)
    public ResponseEntity<ThermalCalculationResult> calculate(@PathVariable Long projectId) {
        IceProject project = projectService.findOrThrow(projectId);
        return ResponseEntity.ok(thermalService.calculate(project));
    }

    @GetMapping("/{projectId}/calculate/override")
    @Operation(summary = "Calculate thermal state with a manual temperature override",
            description = "Run the melt calculation with a custom ambient temperature. " +
                    "Useful for planning worst-case scenarios or heat-wave contingencies")
    public ResponseEntity<ThermalCalculationResult> calculateWithOverride(
            @PathVariable Long projectId,
            @Parameter(description = "Override ambient temperature in C", required = true, example = "32.5")
            @RequestParam double temperatureCelsius) {
        if (temperatureCelsius < - 30 || temperatureCelsius > 60) {
            throw new IllegalArgumentException("Temperature override must be between -30 C and 60 C");
        }

        IceProject project = projectService.findOrThrow(projectId);
        String weatherDesc = String.format("%.1f C (manual override", temperatureCelsius);
        return ResponseEntity.ok(thermalService.calculateWithTemperature(project, temperatureCelsius, weatherDesc));
    }

    @GetMapping("/{projectId}/melt-rate")
    @Operation(summary = "Get current melt rate",
            description = "Returns only the melt rate in kg/hour with the effective temperature for a quick status check.")
    public ResponseEntity<MeltRateSummary> getMeltRate(@PathVariable Long projectId) {
        IceProject project = projectService.findOrThrow(projectId);
        double ambientTemp = weatherService.getCurrentWeather(project).getTemperature();
        double effectiveTemp = thermalService.calculateEffectiveTemperature(project, ambientTemp);
        double meltRate = thermalService.calculateMeltRateKgPerHour(project, effectiveTemp);

        return ResponseEntity.ok(new MeltRateSummary(
                project.getId(),
                project.getName(),
                ambientTemp,
                effectiveTemp,
                meltRate,
                project.getIceType().name()
        ));
    }

    public record MeltRateSummary(
            Long projectId,
            String projectName,
            double ambientTemperatureCelsius,
            double effectiveTemperatureCelsius,
            double meltRateKgPerHour,
            String iceType
    ) {}
}
