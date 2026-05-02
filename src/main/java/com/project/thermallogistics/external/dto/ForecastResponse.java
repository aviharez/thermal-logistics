package com.project.thermallogistics.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastResponse {

    @JsonProperty("list")
    private List<ForecastItem> items;

    @JsonProperty("city")
    private City city;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastItem {
        @JsonProperty("dt")
        private Long timestamp;

        @JsonProperty("dt_txt")
        private String dateText;

        @JsonProperty("main")
        private WeatherResponse.Main main;

        @JsonProperty("weather")
        private List<WeatherResponse.WeatherCondition> weather;

        @JsonProperty("wind")
        private WeatherResponse.Wind wind;

        public double getTemperature() {
            return main != null ? main.getTemp() : 20.0;
        }

        public String getDescription() {
            if (weather != null && !weather.isEmpty()) {
                return weather.get(0).getDescription();
            }
            return "unknown";
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class City {
        @JsonProperty("name")
        private String name;

        @JsonProperty("country")
        private String country;
    }
}
