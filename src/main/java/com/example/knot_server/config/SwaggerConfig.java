package com.example.knot_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;

// http://localhost:8080/swagger-ui.html
@Configuration
public class SwaggerConfig {
        @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CS407 Final Project - Knot Server API")
                .version("v0.0.1")
                .description("API documentation for the Knot Server application. Please use the postman..."));
    }
}
