package com.ridelink.rideservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.rideservice.dto.ErrorResponse;
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
                .authenticationEntryPoint((req, res, ex) -> write(res, 401, "UNAUTHORIZED",
                        "Authentication required: send a valid 'Authorization: Bearer <token>' header"))
                .accessDeniedHandler((req, res, ex) -> write(res, 403, "ACCESS_DENIED",
                        "You do not have permission to perform this action")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()

                // rides
                .requestMatchers(HttpMethod.POST, "/api/v1/rides").hasRole("PASSENGER")
                .requestMatchers(HttpMethod.POST, "/api/v1/rides/*/assign").hasAnyRole("PASSENGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/rides/*/accept").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/rides/*/reject").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/rides/*/start").hasRole("DRIVER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/rides/*/complete").hasRole("DRIVER")
                .requestMatchers(HttpMethod.POST, "/api/v1/rides/*/fare").hasAnyRole("DRIVER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/rides/*/cancel").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/rides").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/rides/**").authenticated()

                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void write(HttpServletResponse res, int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(res.getOutputStream(), new ErrorResponse(code, message));
    }
}
