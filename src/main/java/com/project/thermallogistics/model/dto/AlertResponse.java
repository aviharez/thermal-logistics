package com.project.thermallogistics.model.dto;

import com.project.thermallogistics.model.enums.AlertSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Thermal alert triggered by forecast changes")
public class AlertResponse {

    @Schema(description = "Alert identifier")
    private Long id;

    @Schema(description = "Associated project ID")
    private Long projectId;

    @Schema(description = "Associated project name")
    private String projectName;

    @Schema(description = "Alert severity level")
    private AlertSeverity severity;

    @Schema(description = "Human-readable alert message")
    private String message;

    @Schema(description = "Temperature at time of original schedule calculation (C)")
    private Double baselineTemperatureCelsius;

    @Schema(description = "Forecasted temperature that triggered the alert (C)")
    private Double triggeredTemperatureCelsius;

    @Schema(description = "Temperature increate delta (C)")
    private Double temperatureDeltaCelsius;

    @Schema(description = "Whether this alert has been acknowledged")
    private Boolean acknowledged;

    @Schema(description = "Alert creation timestamp")
    private LocalDateTime triggeredAt;
}
