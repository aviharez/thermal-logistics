package com.project.thermallogistics.service;

import com.project.thermallogistics.model.dto.ThermalCalculationResult;
import com.project.thermallogistics.model.entity.CoolingEquipment;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.EquipmentType;
import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.VenueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThermalCalculationService - melt physics engine")
public class ThermalCalculationServiceTest {

    @Mock private WeatherService weatherService;

    @InjectMocks
    private ThermalCalculationService service;

    private IceProject baseProject;

    @BeforeEach
    void setUp() {
        baseProject = IceProject.builder()
                .id(1L)
                .name("Test Sculpture")
                .latitude(51.5074)
                .longitude(-0.1278)
                .venueName("Test Venue")
                .venueType(VenueType.OUTDOOR)
                .indoorCoolingOffset(0.0)
                .iceType(IceType.WHITE)
                .sculptureVolumeLiters(100.0)
                .eventStartTime(LocalDateTime.now().plusDays(1))
                .eventEndTime(LocalDateTime.now().plusDays(1).plusHours(4))
                .build();
    }

    @Nested
    @DisplayName("Surface area calculation")
    class SurfaceAreaTests {

        @Test
        @DisplayName("Surface area of 1m3 sphere should be approximately 4.836 m2")
        void surfaceAreaOfOneM3() {
            double area = service.sphereSurfaceArea(1.0);
            assertThat(area).isCloseTo(4.836, within(0.01));
        }

        @Test
        @DisplayName("Surface area increases as volume increases")
        void surfaceAreaScalesWithVolume() {
            double small = service.sphereSurfaceArea(0.05);
            double large = service.sphereSurfaceArea(0.5);
            assertThat(large).isGreaterThan(small);
        }

        @Test
        @DisplayName("100L sculpture surface area is approximately 1.04 m2 (sphere equivalent)")
        void surfaceAreaFor100Liters() {
            double volumeM3 = 0.1;
            double area = service.sphereSurfaceArea(volumeM3);
            assertThat(area).isBetween(0.90, 1.20);
        }
    }

    @Nested
    @DisplayName("Melt rate calculation")
    class MeltRateTests {

        @Test
        @DisplayName("Higher ambient temperature produces faster melt rate")
        void higherTempFasterMelt() {
            double rateCold = service.calculateMeltRateKgPerHour(baseProject, 5.0);
            double rateHot = service.calculateMeltRateKgPerHour(baseProject, 30.0);
            assertThat(rateHot).isGreaterThan(rateCold);
        }

        @Test
        @DisplayName("Clear ice melts slower than white ice at identical conditions")
        void clearIceMeltsSlower() {
            IceProject clearProject = buildProjectWithIceType(IceType.CLEAR);
            IceProject whiteProject = buildProjectWithIceType(IceType.WHITE);

            double clearRate = service.calculateMeltRateKgPerHour(clearProject, 20.0);
            double whiteRate = service.calculateMeltRateKgPerHour(whiteProject, 20.0);

            assertThat(clearRate).isLessThan(whiteRate);
        }

        @Test
        @DisplayName("Clear ice melt rate multiplier is 0.85x white ice")
        void clearIceMeltMultiplier() {
            IceProject clearProject = buildProjectWithIceType(IceType.CLEAR);
            IceProject whiteProject = buildProjectWithIceType(IceType.WHITE);

            double clearRate = service.calculateMeltRateKgPerHour(clearProject, 20.0);
            double whiteRate = service.calculateMeltRateKgPerHour(whiteProject, 20.0);

            assertThat(clearRate / whiteRate).isCloseTo(0.85, within(0.001));
        }

        @Test
        @DisplayName("Melt rate is positive for any temperature above 0 C")
        void meltRateIsPositiveAboveZero() {
            double rate = service.calculateMeltRateKgPerHour(baseProject, 1.0);
            assertThat(rate).isPositive();
        }

        @ParameterizedTest(name = "Temperature {0}C -> melt rate > 0")
        @CsvSource({ "1.0", "5.0", "15.0", "25.0", "40.0" })
        @DisplayName("Melt rate is always positive for valid temperatures")
        void meltRatePositiveAcrossTemps(double temp) {
            double rate = service.calculateMeltRateKgPerHour(baseProject, temp);
            assertThat(rate).isPositive();
        }
    }

    @Nested
    @DisplayName("Effective temperature calculation")
    class EffectiveTemperatureTests {

        @Test
        @DisplayName("Outdoor venue with no equipment: effective temp equals ambient")
        void outdoorNoEquipmentEffectiveTemp() {
            double effective = service.calculateEffectiveTemperature(baseProject, 25.0);
            assertThat(effective).isCloseTo(25.0, within(0.01));
        }

        @Test
        @DisplayName("Each fan reduces effective temperature by 2.5C")
        void fanReducesTemperature() {
            addEquipment(baseProject, EquipmentType.FAN, 1);
            double effective = service.calculateEffectiveTemperature(baseProject, 25.0);
            assertThat(effective).isCloseTo(22.5, within(0.01));
        }

        @Test
        @DisplayName("Dry ice booster reduces effective temperature by 7.0C per unit")
        void dryIceReducesTemperature() {
            addEquipment(baseProject, EquipmentType.DRY_ICE_BOOSTER, 1);
            double effective = service.calculateEffectiveTemperature(baseProject, 25.0);
            assertThat(effective).isCloseTo(18.0, within(0.01));
        }

        @Test
        @DisplayName("Multiple equipment types combine cooling reduction")
        void combinedEquipmentReducesTemperature() {
            addEquipment(baseProject, EquipmentType.FAN, 2);
            addEquipment(baseProject, EquipmentType.DRY_ICE_BOOSTER, 1);

            double effective = service.calculateEffectiveTemperature(baseProject, 25.0);
            assertThat(effective).isCloseTo(13.0, within(0.01));
        }

        @Test
        @DisplayName("Effective temperature cannot drop below 0.1C")
        void effectiveTempMinimum() {
            addEquipment(baseProject, EquipmentType.DRY_ICE_BOOSTER, 5);
            double effective = service.calculateEffectiveTemperature(baseProject, 10.0);
            assertThat(effective).isGreaterThanOrEqualTo(0.1);
        }

        @Test
        @DisplayName("Indoor cooled venue applies cooling offset")
        void indoorCooledVenueReducesTemp() {
            IceProject indoorProject = IceProject.builder()
                    .id(2L).name("Indoor").latitude(0.0).longitude(0.0)
                    .venueName("Hall").venueType(VenueType.INDOOR_COOLED)
                    .indoorCoolingOffset(8.0).iceType(IceType.WHITE)
                    .sculptureVolumeLiters(100.0)
                    .eventStartTime(LocalDateTime.now().plusDays(1))
                    .eventEndTime(LocalDateTime.now().plusDays(1).plusHours(3))
                    .build();

            double effective = service.calculateEffectiveTemperature(indoorProject, 28.0);
            assertThat(effective).isCloseTo(20.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Structural failure time")
    class StructuralFailureTests {

        @Test
        @DisplayName("Structural failure time increases as temperature decreases")
        void coolerTempLongerSurvival() {
            double timeAtHot = service.calculateTimeToStructuralFailureHours(baseProject, 30.0);
            double timeAtCool = service.calculateTimeToStructuralFailureHours(baseProject, 10.0);
            assertThat(timeAtCool).isGreaterThan(timeAtHot);
        }

        @Test
        @DisplayName("Structural failure time is based on 40% mass loss threshold")
        void structuralFailureThreshold() {
            double meltRate = service.calculateMeltRateKgPerHour(baseProject, 20.0);
            double volumeM3 = baseProject.getSculptureVolumeLiters() * 0.001;
            double initialMass = volumeM3 * 917.0;
            double expectedFailureTime = (initialMass * ThermalCalculationService.STRUCTURAL_FAILURE_MASS_LOSS_RATIO) / meltRate;

            double actualFailureTime = service.calculateTimeToStructuralFailureHours(baseProject, 20.0);
            assertThat(actualFailureTime).isCloseTo(expectedFailureTime, within(0.001));
        }

        @Test
        @DisplayName("Larger sculpture have longer survival time than smaller ones at same temperature")
        void largerSculptureLongerSurvival() {
            IceProject large = buildProjectWithVolume(500.0);
            IceProject small = buildProjectWithVolume(50.0);

            double largeTime = service.calculateTimeToStructuralFailureHours(large, 20.0);
            double smallTime = service.calculateTimeToStructuralFailureHours(small, 20.0);

            assertThat(largeTime).isGreaterThan(smallTime);
        }
    }

    @Nested
    @DisplayName("Full calculation result")
    class FullCalculationTests {

        @Test
        @DisplayName("Calculate returns all required fields populated")
        void calculateReturnsCompleteResult() {
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(22.0);
            when(weatherService.getForecastSummary(any())).thenReturn("22C, partly cloudy");

            ThermalCalculationResult result = service.calculate(baseProject);

            assertThat(result.getProjectId()).isEqualTo(1L);
            assertThat(result.getAmbientTemperatureCelsius()).isEqualTo(22.0);
            assertThat(result.getMeltRateKgPerHour()).isPositive();
            assertThat(result.getTimeToStructuralFailureHours()).isPositive();
            assertThat(result.getPeakClarityWindowHours()).isPositive();
            assertThat(result.getCoolingBreakdown()).isNotNull();
            assertThat(result.getCalculatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Peak clarity window is shorter that structural failure time")
        void peakClarityWindowShorterThanFailure() {
            when(weatherService.getForecastTemperatureForEvent(any())).thenReturn(20.0);
            when(weatherService.getForecastSummary(any())).thenReturn("20 C");

            ThermalCalculationResult result = service.calculate(baseProject);

            assertThat(result.getPeakClarityWindowHours()).isLessThan(result.getTimeToStructuralFailureHours());
        }

        @Test
        @DisplayName("eventExceedsSafeWindow is true when event is longer than failure time")
        void eventExceedsSafeWindowFlag() {
            IceProject tinyProject = buildProjectWithVolume(5.0);
            tinyProject.setEventStartTime(LocalDateTime.now().plusDays(1));
            tinyProject.setEventEndTime(LocalDateTime.now().plusDays(1).plusHours(8));

            ThermalCalculationResult result = service.calculateWithTemperature(tinyProject, 40.0, "40 C");

            assertThat(result.getEventExceedsSafeWindow()).isTrue();
        }
    }

    private IceProject buildProjectWithIceType(IceType iceType) {
        return IceProject.builder()
                .id(10L).name("Type Test").latitude(0.0).longitude(0.0)
                .venueName("Venue").venueType(VenueType.OUTDOOR).indoorCoolingOffset(0.0)
                .iceType(iceType).sculptureVolumeLiters(100.0)
                .eventStartTime(LocalDateTime.now().plusDays(1))
                .eventEndTime(LocalDateTime.now().plusDays(1).plusHours(4))
                .build();
    }

    private IceProject buildProjectWithVolume(double volumeLiters) {
        return IceProject.builder()
                .id(20L).name("Type Test").latitude(0.0).longitude(0.0)
                .venueName("Venue").venueType(VenueType.OUTDOOR).indoorCoolingOffset(0.0)
                .iceType(IceType.WHITE).sculptureVolumeLiters(volumeLiters)
                .eventStartTime(LocalDateTime.now().plusDays(1))
                .eventEndTime(LocalDateTime.now().plusDays(1).plusHours(4))
                .build();
    }

    private void addEquipment(IceProject project, EquipmentType type, int qty) {
        CoolingEquipment eq = CoolingEquipment.builder()
                .id((long) project.getCoolingEquipments().size() + 1)
                .iceProject(project)
                .equipmentType(type)
                .quantity(qty)
                .build();
        project.getCoolingEquipments().add(eq);
    }
}
