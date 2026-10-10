package com.ridelink.fareservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Swagger UI configuration for the Fare & Payment Service.
 * Configures JWT Bearer authentication scheme and API documentation metadata.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "RideLink - Fare & Payment Service API",
                version = "1.0.0",
                description = "Backend microservice for Member 4: handles ride fare estimation, final ride fare calculation, " +
                        "simulated payment processing, and electronic receipt generation.",
                contact = @Contact(
                        name = "IT3130 RideLink Team - Member 4",
                        email = "member4@ridelink.local"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8084", description = "Local Microservice Environment")
        },
        security = {
                @SecurityRequirement(name = "BearerAuth")
        }
)
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT Bearer token issued by Account Service (or generated via /api/auth/dev-token)"
)
public class OpenApiConfig {
}
