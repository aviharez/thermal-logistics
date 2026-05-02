package com.project.thermallogistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ThermalLogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThermalLogisticsApplication.class, args);
    }

}
