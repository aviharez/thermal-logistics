package com.project.thermallogistics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI thermalLogisticsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Glacier & Garnish - Thermal Logistics API")
                        .description("""
                                    Professional thermal lifecycle management for high-end ice sculpture catering.
                                   \s
                                    This API manages the complete thermal lifecycle of ice installations:
                                    from initial project creation through real-time melt monitoring,
                                    automated installation scheduling, and critical temperature alerting.
                               \s""")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}
