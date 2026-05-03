package com.project.thermallogistics.service;

import com.project.thermallogistics.external.WeatherApiClient;
import com.project.thermallogistics.external.dto.ForecastResponse;
import com.project.thermallogistics.external.dto.WeatherResponse;
import com.project.thermallogistics.model.entity.IceProject;
import com.project.thermallogistics.model.enums.IceType;
import com.project.thermallogistics.model.enums.ProjectStatus;
import com.project.thermallogistics.model.enums.VenueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService - forecast retrieval and parsing")
public class WeatherServiceTest {

    @Mock private WeatherApiClient weatherApiClient;

    @InjectMocks private WeatherService weatherService;

    private IceProject testProject;

    @BeforeEach
    void setUp() {
        testProject = IceProject.builder()
                .id(1L)
                .name("Weather Test Project")
                .latitude(51.5074)
                .longitude(-0.1278)
                .venueName("Test Venue")
                .venueType(VenueType.OUTDOOR)
                .indoorCoolingOffset(0.0)
                .iceType(IceType.WHITE)
                .sculptureVolumeLiters(100.0)
                .eventStartTime(LocalDateTime.now().plusDays(2))
                .eventEndTime(LocalDateTime.now().plusDays(2).plusHours(4))
                .status(ProjectStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("getCurrentWeather delegates to API client with correct coordinates")
    void getCurrentWeatherDelegatesToClient() {
        WeatherResponse mockResponse = buildMockWeatherResponse(22.5);
        when(weatherApiClient.getCurrentWeather(51.5074, -0.1278)).thenReturn(mockResponse);

        WeatherResponse result = weatherService.getCurrentWeather(testProject);

        assertThat(result.getTemperature()).isEqualTo(22.5);
        verify(weatherApiClient).getCurrentWeather(51.5074, -0.1278);
    }

    @Test
    @DisplayName("getForecastTemperatureForEvent returns closest forecast slot to event time")
    void getForecastTemperatureForEventReturnsClosestSlot() {
        long eventEpoch = testProject.getEventStartTime().toEpochSecond(ZoneOffset.UTC);

        ForecastResponse forecast = buildMockForecast(List.of(
                new SlotData(eventEpoch - 7200, 18.0),
                new SlotData(eventEpoch + 1800, 23.0),
                new SlotData(eventEpoch + 10800, 26.0)
        ));

        when(weatherApiClient.getFiveDayForecast(anyDouble(), anyDouble())).thenReturn(forecast);

        double temp = weatherService.getForecastTemperatureForEvent(testProject);

        assertThat(temp).isEqualTo(23.0);
    }

    @Test
    @DisplayName("getMaxForecastTemperatureDuring returns highest temp within event window")
    void getMaxTemperatureDuringEventWindow() {
        long eventStart = testProject.getEventStartTime().toEpochSecond(ZoneOffset.UTC);
        long eventEnd = testProject.getEventEndTime().toEpochSecond(ZoneOffset.UTC);

        ForecastResponse forecast = buildMockForecast(List.of(
                new SlotData(eventStart - 3600, 15.0),
                new SlotData(eventStart + 3600, 28.0),
                new SlotData(eventStart + 7200, 31.0),
                new SlotData(eventEnd + 3600, 20)
        ));

        when(weatherApiClient.getFiveDayForecast(anyDouble(), anyDouble())).thenReturn(forecast);

        double maxTemp = weatherService.getMaxForecastTemperatureDuring(testProject);

        assertThat(maxTemp).isEqualTo(31.0);
    }

    @Test
    @DisplayName("getForecastSummary returns formatted string with temperature and condition")
    void getForecastSummaryFormattedCorrectly() {
        long eventEpoch = testProject.getEventStartTime().toEpochSecond(ZoneOffset.UTC);
        ForecastResponse forecast = buildMockForecast(List.of(new SlotData(eventEpoch, 19.5)));
        forecast.getCity().setName("London");

        when(weatherApiClient.getFiveDayForecast(anyDouble(), anyDouble())).thenReturn(forecast);

        String summary = weatherService.getForecastSummary(testProject);

        assertThat(summary).contains("19.5 C");
        assertThat(summary).contains("London");
    }

    @Test
    @DisplayName("getForecastSummary returns fallback string when API fails")
    void getForecastSummaryFallbackOnError() {
        when(weatherApiClient.getFiveDayForecast(anyDouble(), anyDouble())).thenThrow(new RuntimeException("API unavailable"));

        String summary = weatherService.getForecastSummary(testProject);

        assertThat(summary).contains("unavailable");
    }

    private WeatherResponse buildMockWeatherResponse(double temp) {
        WeatherResponse response = new WeatherResponse();
        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(temp);
        response.setMain(main);

        WeatherResponse.WeatherCondition condition = new WeatherResponse.WeatherCondition();
        condition.setDescription("clear sky");
        response.setWeather(List.of(condition));
        return response;
    }

    private record SlotData(long timestamp, double temp) {}

    private ForecastResponse buildMockForecast(List<SlotData> slots) {
        ForecastResponse forecast = new ForecastResponse();
        List<ForecastResponse.ForecastItem> items = new ArrayList<>();

        for (SlotData slot : slots) {
            ForecastResponse.ForecastItem item = new ForecastResponse.ForecastItem();
            item.setTimestamp(slot.timestamp());

            WeatherResponse.Main main = new WeatherResponse.Main();
            main.setTemp(slot.temp());
            item.setMain(main);

            WeatherResponse.WeatherCondition cond = new WeatherResponse.WeatherCondition();
            cond.setDescription("partly cloudy");
            item.setWeather(List.of(cond));

            items.add(item);
        }
        forecast.setItems(items);

        ForecastResponse.City city = new ForecastResponse.City();
        city.setName("Test City");
        city.setCountry("GB");
        forecast.setCity(city);

        return forecast;
    }
}
