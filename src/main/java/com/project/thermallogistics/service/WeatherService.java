package com.project.thermallogistics.service;

import com.project.thermallogistics.external.WeatherApiClient;
import com.project.thermallogistics.external.dto.ForecastResponse;
import com.project.thermallogistics.external.dto.WeatherResponse;
import com.project.thermallogistics.model.entity.IceProject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiClient weatherApiClient;

    public WeatherResponse getCurrentWeather(IceProject project) {
        log.debug("Fetching current weather for project '{}' at ({}, {})",
                project.getName(), project.getLatitude(), project.getLongitude());
        return weatherApiClient.getCurrentWeather(project.getLatitude(), project.getLongitude());
    }

    public WeatherResponse getCurrentWeather(double lat, double lon) {
        return weatherApiClient.getCurrentWeather(lat, lon);
    }

    public ForecastResponse getForecast(IceProject project) {
        log.debug("Fetching 5-day forecast for project '{}' at ({}, {})",
                project.getName(), project.getLatitude(), project.getLongitude());
        return weatherApiClient.getFiveDayForecast(project.getLatitude(), project.getLongitude());
    }

    public double getForecastTemperatureForEvent(IceProject project) {
        ForecastResponse forecast = getForecast(project);
        List<ForecastResponse.ForecastItem> items = forecast.getItems();

        if (items == null || items.isEmpty()) {
            log.warn("No forecast items available for project '{}'. Failing back to current weather.", project.getName());
            return getCurrentWeather(project).getTemperature();
        }

        long eventEpoch = project.getEventStartTime().toEpochSecond(ZoneOffset.UTC);

        Optional<ForecastResponse.ForecastItem> closest = items.stream()
                .min(Comparator.comparingLong(item -> Math.abs(item.getTimestamp() - eventEpoch)));

        return closest.map(ForecastResponse.ForecastItem::getTemperature)
                .orElseGet(() -> {
                    log.warn("Could not find matching forecast slot. Using first available.");
                    return items.get(0).getTemperature();
                });
    }

    public double getMaxForecastTemperatureDuring(IceProject project) {
        ForecastResponse forecast = getForecast(project);
        List<ForecastResponse.ForecastItem> items = forecast.getItems();

        if (items == null || items.isEmpty()) {
            return getCurrentWeather(project).getTemperature();
        }

        long eventStart = project.getEventStartTime().toEpochSecond(ZoneOffset.UTC);
        long eventEnd = project.getEventEndTime().toEpochSecond(ZoneOffset.UTC);

        return items.stream()
                .filter(item -> item.getTimestamp() >= eventStart && item.getTimestamp() <= eventEnd)
                .mapToDouble(ForecastResponse.ForecastItem::getTemperature)
                .max()
                .orElseGet(() -> getCurrentWeather(project).getTemperature());
    }

    public String getForecastSummary(IceProject project) {
        try {
            ForecastResponse forecast = getForecast(project);
            List<ForecastResponse.ForecastItem> items = forecast.getItems();

            if (items == null || items.isEmpty()) {
                return "Weather data unavailable";
            }

            double temp = getForecastTemperatureForEvent(project);
            String cityName = forecast.getCity() != null ? forecast.getCity().getName() : "venue";
            String condition = items.isEmpty() ? "unknown" : items.get(0).getDescription();

            return String.format("%.1f C, %s at %s", temp, condition, cityName);
        } catch (Exception e) {
            log.warn("Could not generate forecast summary: {}", e.getMessage());
            return "Weather data temporarily unavailable";
        }
    }
}
