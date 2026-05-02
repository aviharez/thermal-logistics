package com.project.thermallogistics.external;

import com.project.thermallogistics.config.AppProperties;
import com.project.thermallogistics.exception.WeatherApiException;
import com.project.thermallogistics.external.dto.ForecastResponse;
import com.project.thermallogistics.external.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherApiClient {

    private final WebClient weatherWebClient;
    private final AppProperties appProperties;

    private static final String DEMO_KEY = "demo-key";

    public WeatherResponse getCurrentWeather(double latitude, double longitude) {
        if (isDemoMode()) {
            log.debug("Weather API in demo mode. Returning synthetic for lat={}, lon={}", latitude, longitude);
            return buildDemoWeatherResponse(latitude, longitude);
        }

        return weatherWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/weather")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("appid", appProperties.getKey())
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .timeout(Duration.ofMillis(appProperties.getReadTimeout()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Weather API responded with status {}: {}", ex.getStatusCode(), ex.getMessage());
                    return Mono.error(new WeatherApiException("Weather service returned error " + ex.getStatusCode() + ": " + ex.getMessage(), ex));
                })
                .onErrorResume(Exception.class, ex -> {
                    if (ex instanceof WeatherApiException) return Mono.error(ex);
                    log.error("Weather API call failed: {}", ex.getMessage());
                    return Mono.error(new WeatherApiException("Failed to reach weather service: " + ex.getMessage(), ex));
                })
                .block();
    }

    public ForecastResponse getFiveDayForecast(double latitude, double longitude) {
        if (isDemoMode()) {
            log.debug("Weather API in demo mode. Returning synthetic forecast for lat={}, lon={}", latitude, longitude);
            return buildDemoForecastResponse(latitude, longitude);
        }

        return weatherWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("lat", latitude)
                        .queryParam("lon", longitude)
                        .queryParam("appid", appProperties.getKey())
                        .queryParam("units", "metric")
                        .build())
                .retrieve()
                .bodyToMono(ForecastResponse.class)
                .timeout(Duration.ofMillis(appProperties.getReadTimeout()))
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error("Forecast API responded with status {}: {}", ex.getStatusCode(), ex.getMessage());
                    return Mono.error(new WeatherApiException("Forecast service returned error " + ex.getStatusCode() + ": " + ex.getMessage(), ex));
                })
                .onErrorResume(Exception.class, ex -> {
                    if (ex instanceof WeatherApiException) return Mono.error(ex);
                    log.error("Forecast API call failed: {}", ex.getMessage());
                    return Mono.error(new WeatherApiException("Failed to reach forecast service: " + ex.getMessage(), ex));
                })
                .block();
    }

    private boolean isDemoMode() {
        String key = appProperties.getKey();
        return key == null || key.isBlank() || DEMO_KEY.equals(key);
    }

    // Synthetic data: latitude-adjusted temperature (equator warm, poles cold)
    private WeatherResponse buildDemoWeatherResponse(double latitude, double longitude) {
        double baseTemp = 30.0 - (Math.abs(latitude) * 0.4);
        double variance = (longitude % 10) * 0.3;
        double temp = Math.round((baseTemp + variance) * 10.0) / 10.0;

        WeatherResponse response = new WeatherResponse();

        WeatherResponse.Main main = new WeatherResponse.Main();
        main.setTemp(temp);
        main.setFeelsLike(temp + 1.5);
        main.setTempMin(temp - 2.0);
        main.setTempMax(temp + 3.0);
        main.setHumidity(65);
        response.setMain(main);

        WeatherResponse.WeatherCondition condition = new WeatherResponse.WeatherCondition();
        condition.setId(800);
        condition.setMain("Clear");
        condition.setDescription("clear sky (demo data)");
        condition.setIcon("01d");
        response.setWeather(List.of(condition));

        WeatherResponse.Wind wind = new WeatherResponse.Wind();
        wind.setSpeed(3.5);
        wind.setDeg(180);
        response.setWind(wind);

        response.setCityName("Demo City");
        response.setTimestamp(System.currentTimeMillis() / 1000);

        return response;
    }

    private ForecastResponse buildDemoForecastResponse(double latitude, double longitude) {
        double baseTemp = 30.0 - (Math.abs(latitude) * 0.4);

        ForecastResponse forecast = new ForecastResponse();
        List<ForecastResponse.ForecastItem> items = new ArrayList<>();

        long now = System.currentTimeMillis() / 1000;
        for (int i = 0; i < 40; i++) {
            ForecastResponse.ForecastItem item = new ForecastResponse.ForecastItem();
            item.setTimestamp(now + (i * 3 * 3600L));

            // Simulate diurnal temperature variation
            double hourOfDay = (i * 3) % 24;
            double diurnalDelta = Math.sin(Math.PI * (hourOfDay - 6) / 12) * 4;
            double temp = Math.round((baseTemp + diurnalDelta + (i * 0.05)) * 10.0) / 10.0;

            WeatherResponse.Main main = new WeatherResponse.Main();
            main.setTemp(temp);
            main.setFeelsLike(temp + 1.0);
            main.setTempMin(temp - 1.5);
            main.setTempMax(temp + 2.0);
            main.setHumidity(60 + (i % 20));
            item.setMain(main);

            WeatherResponse.WeatherCondition condition = new WeatherResponse.WeatherCondition();
            condition.setDescription("partly cloudy (demo data)");
            item.setWeather(List.of(condition));

            items.add(item);
        }

        forecast.setItems(items);

        ForecastResponse.City city = new ForecastResponse.City();
        city.setName("Demo City");
        city.setCountry("XX");
        forecast.setCity(city);

        return forecast;
    }
}
