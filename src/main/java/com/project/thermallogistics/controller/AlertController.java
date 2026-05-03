package com.project.thermallogistics.controller;

import com.project.thermallogistics.model.dto.AlertResponse;
import com.project.thermallogistics.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Thermal Alerts", description = "Critical temperature change alerting for active installations")
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/check/{projectId}")
    @Operation(summary = "Check and generate Critical Temperature Warnings")
    public ResponseEntity<List<AlertResponse>> checkAlerts(@PathVariable Long projectId) {
        return ResponseEntity.ok(alertService.checkAndGenerateAlerts(projectId));
    }

    @GetMapping
    @Operation(summary = "List all alerts", description = "Returns all thermal alerts across all projects.")
    public ResponseEntity<List<AlertResponse>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/unacknowledged")
    @Operation(summary = "List all unacknowledged alerts",
            description = "Returns all alerts that have not yet been acknowledged by the operations team.")
    public ResponseEntity<List<AlertResponse>> getUnacknowledged() {
        return ResponseEntity.ok(alertService.getUnacknowledgedAlerts());
    }

    @GetMapping("/{alertId}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponse> getAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.getAlertById(alertId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get all alerts for a project")
    public ResponseEntity<List<AlertResponse>> getAlertsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(alertService.getAlertsByProject(projectId));
    }

    @GetMapping("/project/{projectId}/recent")
    @Operation(summary = "Get alerts triggered in the last 24 hours for a project")
    public ResponseEntity<List<AlertResponse>> getRecentAlerts(@PathVariable Long projectId) {
        return ResponseEntity.ok(alertService.getRecentAlertsForProject(projectId));
    }

    @PutMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge an alert",
            description = "Marks an alert as reviewed by the operations team. Does not dismiss the underlying condition.")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable Long alertId) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(alertId));
    }
}
