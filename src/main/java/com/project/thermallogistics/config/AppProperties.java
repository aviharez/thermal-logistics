package com.project.thermallogistics.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "weather.api")
public class AppProperties {

    private String key = "demo-key";
    private String baseUrl = "https://api.openweathermap.org/data/2.5";
    private int connectTimeout = 5000;
    private int readTimeout = 10000;
}
