package com.project.thermallogistics.service;

import com.project.thermallogistics.exception.IceProjectNotFoundException;
import com.project.thermallogistics.model.dto.AlertResponse;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.entity.ThermalAlert;
import com.project.thermallogistics.model.enums.AlertSeverity;
import com.project.thermallogistics.repository.ThermalAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AlertService {

    private final ThermalAlertRepository alertRepository;
    private final IceProjectService projectService;
    private final WeatherService weatherService;
    private final ThermalCalculationService thermalService;

    static final double LOW_THRESHOLD = 1.0;
    static final double MEDIUM_THRESHOLD = 3.0;
    static final double HIGH_THRESHOLD = 5.0;
    static final double CRITICAL_THRESHOLD = 8.0;

    static final long ALERT_WINDOW_HOURS = 24;

    /**
     * Checks for Critical Temperature Warnings for the given project.
     *
     * <p>Only executes when the event is within the next {@value ALERT_WINDOW_HOURS} hours.</p>
     * Returns an empty list if the event is too far in the future or has already passed.
     *
     * <p>On the first call within the window the current weather is stored as the baseline.</p>
     * All subsequent calls compare the live event-time forecast against that baseline.
     */
    public List<AlertResponse> checkAndGenerateAlerts(Long projectId) {
        IceProject project = projectService.findOrThrow(projectId);

        long hoursUntilEvent = ChronoUnit.HOURS.between(LocalDateTime.now(), project.getEventStartTime());

        if (hoursUntilEvent > ALERT_WINDOW_HOURS) {
            log.info("Alert check skipped for project '{}': event is {}h away (outside {}h window).",
                    project.getName(), hoursUntilEvent, ALERT_WINDOW_HOURS);
            return List.of();
        }

        if (hoursUntilEvent < 0) {
            log.info("Alert check skipped for project '{}': event has already started or passed.", project.getName());
            return List.of();
        }

        // First call within the window: lock in the baseline from current conditions
        if (project.getBaselineTemperatureCelsius() == null) {
            double baseline = weatherService.getCurrentWeather(project).getTemperature();
            projectService.captureBaselineTemperature(projectId, baseline);
            project.setBaselineTemperatureCelsius(baseline);
            log.info("Baseline temperature captured for project '{}': {} C ({}h until event).",
                    project.getName(), baseline, hoursUntilEvent);
            return List.of();
        }

        double forecastTemp = weatherService.getForecastTemperatureForEvent(project);
        double baselineTemp = project.getBaselineTemperatureCelsius();
        double delta = forecastTemp - baselineTemp;

        log.info("Alert check for project '{}': baseline={} C, forecast={} C, delta=+{} C, hoursUntilEvent={}",
                project.getName(), baselineTemp, forecastTemp, delta, hoursUntilEvent);

        if (delta < LOW_THRESHOLD) {
            log.debug("No alert threshold breached for project '{}' (delta={} C).", project.getName(), delta);
            return List.of();
        }

        AlertSeverity severity = classifySeverity(delta);
        String message = buildAlertMessage(project, severity, baselineTemp, forecastTemp, delta, hoursUntilEvent);

        ThermalAlert alert = ThermalAlert.builder()
                .iceProject(project)
                .severity(severity)
                .message(message)
                .baselineTemperatureCelsius(baselineTemp)
                .triggeredTemperatureCelsius(forecastTemp)
                .temperatureDeltaCelsius(delta)
                .acknowledged(false)
                .build();

        ThermalAlert saved = alertRepository.save(alert);
        log.warn("[{}] Critical Temperature Warning - project='{}', delta=+{} C, {}h until event.",
                severity, project.getName(), delta, hoursUntilEvent);

        return List.of(toResponse(saved));
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlertsByProject(Long projectId) {
        projectService.findOrThrow(projectId);
        return alertRepository.findByIceProjectId(projectId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnacknowledgedAlerts() {
        return alertRepository.findByAcknowledgedFalse()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long alertId) {
        return alertRepository.findById(alertId)
                .map(this::toResponse)
                .orElseThrow(() -> new IceProjectNotFoundException("Alert not found with id: " + alertId));
    }

    public AlertResponse acknowledgeAlert(Long alertId) {
        ThermalAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IceProjectNotFoundException("Alert not found with id: " + alertId));
        alert.setAcknowledged(true);
        ThermalAlert saved = alertRepository.save(alert);
        log.info("Alert {} acknowledged.", alertId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getRecentAlertsForProject(Long projectId) {
        projectService.findOrThrow(projectId);
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return alertRepository.findRecentAlertsForProject(projectId, since)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    AlertSeverity classifySeverity(double delta) {
        if (delta >= CRITICAL_THRESHOLD) return AlertSeverity.CRITICAL;
        if (delta >= HIGH_THRESHOLD) return AlertSeverity.HIGH;
        if (delta >= MEDIUM_THRESHOLD) return AlertSeverity.MEDIUM;
        return AlertSeverity.LOW;
    }

    private String buildAlertMessage(IceProject project, AlertSeverity severity, double baseline, double current, double delta, long hoursUntilEvent) {
        double revisedWindow = thermalService.calculateTimeToStructuralFailureHours(project, current);
        return String.format(
                "[%s] Forecast for '%s' has risen +%.1f C above baseline (%.1f C -> %.1f C) " +
                        "with %dh until event start. " +
                        "Safe display window revised to %.1fh. " +
                        "Immediate review of installation schedule required.",
                severity.name(), project.getName(), delta, baseline, current,
                hoursUntilEvent, revisedWindow);
    }

    private AlertResponse toResponse(ThermalAlert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .projectId(alert.getIceProject().getId())
                .projectName(alert.getIceProject().getName())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .baselineTemperatureCelsius(alert.getBaselineTemperatureCelsius())
                .triggeredTemperatureCelsius(alert.getTriggeredTemperatureCelsius())
                .temperatureDeltaCelsius(alert.getTemperatureDeltaCelsius())
                .acknowledged(alert.getAcknowledged())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }
}
