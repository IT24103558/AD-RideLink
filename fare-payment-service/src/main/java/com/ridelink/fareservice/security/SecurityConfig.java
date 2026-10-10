package com.ridelink.fareservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fareservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security configuration for Fare & Payment Service.
 * Enforces stateless JWT authentication and role-based access control.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, 401, "UNAUTHORIZED",
                                "Authentication required: supply a valid 'Authorization: Bearer <token>' header"))
                        .accessDeniedHandler((req, res, ex) -> writeError(res, 403, "ACCESS_DENIED",
                                "Forbidden: you do not have permission to execute this operation")))
                .authorizeHttpRequests(auth -> auth
                        // Public OpenAPI / Swagger documentation
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/webjars/**"
                        ).permitAll()

                        // Local dev token generation endpoint (for quick Postman/Swagger testing without Account Service)
                        .requestMatchers("/api/auth/dev-token").permitAll()

                        // Fare estimation: publicly open or authenticated
                        .requestMatchers(HttpMethod.POST, "/api/fares/estimate", "/api/v1/fares/estimate").permitAll()

                        // Final fare calculation: DRIVER, ADMIN or authenticated service
                        .requestMatchers(HttpMethod.POST, "/api/fares/final", "/api/v1/fares/final").authenticated()

                        // Fare retrieval: readable by authenticated callers
                        .requestMatchers(HttpMethod.GET, "/api/fares/**", "/api/v1/fares/**").authenticated()

                        // Payments: authenticated (PASSENGER, ADMIN, etc.)
                        .requestMatchers(HttpMethod.POST, "/api/payments", "/api/v1/payments").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/payments/**", "/api/v1/payments/**").authenticated()

                        // Receipts: authenticated
                        .requestMatchers(HttpMethod.GET, "/api/receipts/**", "/api/v1/receipts/**").authenticated()

                        // Any remaining endpoints require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), new ErrorResponse(code, message));
    }
}
