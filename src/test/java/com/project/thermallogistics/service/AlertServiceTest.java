package com.project.thermallogistics.service;

import com.project.thermallogistics.external.dto.WeatherResponse;
import com.project.thermallogistics.model.dto.AlertResponse;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.entity.ThermalAlert;
import com.project.thermallogistics.model.enums.AlertSeverity;
import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import com.project.thermallogistics.repository.ThermalAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService - Critical Temperature Warning system")
public class AlertServiceTest {

    @Mock private ThermalAlertRepository alertRepository;
    @Mock private IceProjectService projectService;
    @Mock private WeatherService weatherService;
    @Mock private ThermalCalculationService thermalService;

    @InjectMocks
    private AlertService alertService;

    private IceProject projectWithin24h;
    private IceProject projectOutside24h;
    private IceProject projectWithBaseline;

    @BeforeEach
    void setUp() {
        // event is 12 hours away -> inside the 24h window
        projectWithin24h = buildProject(1L, LocalDateTime.now().plusHours(12), null);

        // event is 48 hours away -> outside the 24h window
        projectOutside24h = buildProject(2L, LocalDateTime.now().plusHours(48), null);

        // event is 6 hours away with a stored baseline of 20 C
        projectWithBaseline = buildProject(3L, LocalDateTime.now().plusHours(6), 20.0);
    }

    @Nested
    @DisplayName("24-hour window enforcement")
    class WindowEnforcementTests {

        @Test
        @DisplayName("Returns empty list when event is more than 24 hours away")
        void returnsEmptyWhenOutsideWindow() {
            when(projectService.findOrThrow(2L)).thenReturn(projectOutside24h);

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(2L);

            assertThat(alerts).isEmpty();
            verifyNoInteractions(weatherService, alertRepository);
        }

        @Test
        @DisplayName("Returns empty list when event has already passed")
        void returnsEmptyWhenEventPassed() {
            IceProject pastProject = buildProject(4L, LocalDateTime.now().minusHours(2), null);
            when(projectService.findOrThrow(4L)).thenReturn(pastProject);

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(4L);

            assertThat(alerts).isEmpty();
            verifyNoInteractions(weatherService, alertRepository);
        }

        @Test
        @DisplayName("Proceeds with alert check when event is exactly at the window boundary (24h)")
        void proceedsAtWindowBoundary() {
            IceProject boundaryProject = buildProject(5L, LocalDateTime.now().plusHours(23).plusMinutes(30), 18.0);
            when(projectService.findOrThrow(5L)).thenReturn(boundaryProject);
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(18.2);

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(5L);

            assertThat(alerts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Baseline capture")
    class BaselineCaptureTests {

        @Test
        @DisplayName("Captures baseline on first call within window and returns empty list")
        void capturesBaselineOnFirstCall() {
            when(projectService.findOrThrow(1L)).thenReturn(projectWithin24h);
            when(weatherService.getCurrentWeather(any())).thenReturn(mockWeatherResponse(22.5));

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(1L);

            assertThat(alerts).isEmpty();
            verify(projectService).captureBaselineTemperature(1L, 22.5);
            verifyNoInteractions(alertRepository);
        }

        @Test
        @DisplayName("Does not re-capture baseline on subsequent calls (baseline already set)")
        void doesNotRecaptureBaseline() {
            when(projectService.findOrThrow(3L)).thenReturn(projectWithBaseline);
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(20.5);

            alertService.checkAndGenerateAlerts(3L);

            verify(projectService, never()).captureBaselineTemperature(anyLong(), anyDouble());
            verify(weatherService, never()).getCurrentWeather(any(IceProject.class));
        }
    }

    @Nested
    @DisplayName("Alert severity classification")
    class SeverityClassificationTests {

        @ParameterizedTest(name = "delta={0}C -> severity={1}")
        @CsvSource({
                "1.0, LOW",
                "2.5, LOW",
                "3.0, MEDIUM",
                "4.9, MEDIUM",
                "5.0, HIGH",
                "7.9, HIGH",
                "8.0, CRITICAL",
                "15.0, CRITICAL"
        })
        @DisplayName("classifySeverity maps temperature delta to correct severity tier")
        void classifySeverityMapping(double delta, AlertSeverity expected) {
            assertThat(alertService.classifySeverity(delta)).isEqualTo(expected);
        }

        @Test
        @DisplayName("LOW alert generated when delta is between 1C and 3C")
        void lowAlertGenerated() {
            when(projectService.findOrThrow(3L)).thenReturn(projectWithBaseline);
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(21.5);
            when(thermalService.calculateTimeToStructuralFailureHours(any(), anyDouble())).thenReturn(5.0);
            stubAlertSave(AlertSeverity.LOW, 1.5);

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(3L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.LOW);
        }

        @Test
        @DisplayName("CRITICAL alert generated when delta exceeds 8C")
        void criticalAlertGenerated() {
            when(projectService.findOrThrow(3L)).thenReturn(projectWithBaseline);
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(29.0);
            when(thermalService.calculateTimeToStructuralFailureHours(any(), anyDouble())).thenReturn(1.5);
            stubAlertSave(AlertSeverity.CRITICAL, 9.0);

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(3L);

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(alerts.get(0).getTemperatureDeltaCelsius()).isEqualTo(9.0);
        }

        @Test
        @DisplayName("No alert when delta is below LOW threshold")
        void noAlertBelowThreshold() {
            when(projectService.findOrThrow(3L)).thenReturn(projectWithBaseline);
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(20.8);

            List<AlertResponse> alerts = alertService.checkAndGenerateAlerts(3L);

            assertThat(alerts).isEmpty();
            verifyNoInteractions(alertRepository);
        }
    }

    @Nested
    @DisplayName("Alert content validation")
    class AlertContentTests {

        @Test
        @DisplayName("Saved alert contains correct baseline and triggered temperatures")
        void alertContainsCorrectTemperatures() {
            when(projectService.findOrThrow(3L)).thenReturn(projectWithBaseline);
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(26.0);
            when(thermalService.calculateTimeToStructuralFailureHours(any(), anyDouble())).thenReturn(3.0);

            ArgumentCaptor<ThermalAlert> alertCaptor = ArgumentCaptor.forClass(ThermalAlert.class);
            when(alertRepository.save(alertCaptor.capture())).thenAnswer(inv -> {
                ThermalAlert a = inv.getArgument(0);
                a = ThermalAlert.builder()
                        .id(1L).iceProject(a.getIceProject()).severity(a.getSeverity())
                        .message(a.getMessage()).baselineTemperatureCelsius(a.getBaselineTemperatureCelsius())
                        .temperatureDeltaCelsius(a.getTemperatureDeltaCelsius())
                        .acknowledged(false).build();
                return a;
            });

            alertService.checkAndGenerateAlerts(3L);

            ThermalAlert captured = alertCaptor.getValue();
            assertThat(captured.getBaselineTemperatureCelsius()).isEqualTo(20.0);
            assertThat(captured.getTriggeredTemperatureCelsius()).isEqualTo(26.0);
            assertThat(captured.getTemperatureDeltaCelsius()).isEqualTo(6.0);
            assertThat(captured.getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(captured.getMessage()).contains("HIGH");
            assertThat(captured.getMessage()).contains("20.0 C");
            assertThat(captured.getMessage()).contains("26.0 C");
        }
    }

    @Nested
    @DisplayName("Acknowledge alert")
    class AcknowledgeTests {

        @Test
        @DisplayName("Acknowledge sets acknowledged flag to true")
        void acknowledgeUpdatesFlag() {
            ThermalAlert alert = ThermalAlert.builder()
                    .id(10L).iceProject(projectWithBaseline)
                    .severity(AlertSeverity.HIGH).message("Test alert")
                    .baselineTemperatureCelsius(20.0).triggeredTemperatureCelsius(26.0)
                    .temperatureDeltaCelsius(6.0).acknowledged(false).build();

            ThermalAlert acknowledged = ThermalAlert.builder()
                    .id(10L).iceProject(projectWithBaseline)
                    .severity(AlertSeverity.HIGH).message("Test alert")
                    .baselineTemperatureCelsius(20.0).triggeredTemperatureCelsius(26.0)
                    .temperatureDeltaCelsius(6.0).acknowledged(true).build();

            when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
            when(alertRepository.save(any())).thenReturn(acknowledged);

            AlertResponse response = alertService.acknowledgeAlert(10L);

            assertThat(response.getAcknowledged()).isTrue();
        }
    }

    // helpers

    private IceProject buildProject(Long id, LocalDateTime eventStart, Double baselineTemp) {
        IceProject p = IceProject.builder()
                .id(id).name("Project " + id)
                .latitude(51.5074).longitude(-0.1278)
                .venueName("Test Venue").venueType(VenueType.OUTDOOR)
                .indoorCoolingOffset(0.0).iceType(IceType.WHITE)
                .sculptureVolumeLiters(100.0)
                .eventStartTime(eventStart)
                .eventEndTime(eventStart.plusHours(4))
                .status(ProjectStatus.SCHEDULED)
                .build();
        p.setBaselineTemperatureCelsius(baselineTemp);
        return p;
    }

    private WeatherResponse mockWeatherResponse(double temp) {
        WeatherResponse response = new WeatherResponse();
        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(temp);
        response.setMain(main);
        return response;
    }

    private void stubAlertSave(AlertSeverity severity, double delta) {
        when(alertRepository.save(any(ThermalAlert.class))).thenAnswer(inv -> {
            ThermalAlert a = inv.getArgument(0);
            return ThermalAlert.builder()
                    .id(99L).iceProject(a.getIceProject()).severity(a.getSeverity())
                    .message(a.getMessage()).baselineTemperatureCelsius(a.getBaselineTemperatureCelsius())
                    .triggeredTemperatureCelsius(a.getTriggeredTemperatureCelsius())
                    .temperatureDeltaCelsius(a.getTemperatureDeltaCelsius())
                    .acknowledged(false).build();
        });
    }
}
