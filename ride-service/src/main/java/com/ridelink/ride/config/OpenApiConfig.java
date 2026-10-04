package com.ridelink.ride.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        final String s = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("RideLink — Ride Management Service API")
                        .description("Ride lifecycle, driver assignment, and state machine.").version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(s))
                .components(new Components().addSecuritySchemes(s,
                        new SecurityScheme().name(s).type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
