package com.ridelink.driverservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Swagger / OpenAPI docs - open for demo purposes
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml",
                                "/webjars/**")
                        .permitAll()

                        // GET endpoints are readable by any authenticated caller (including
                        // the Ride Management Service acting on a passenger/driver's behalf).
                        // NOTE: discuss with your group whether inter-service calls should
                        // instead use a dedicated service-to-service credential.
                        .requestMatchers(HttpMethod.GET, "/api/v1/drivers/**").authenticated()

                        // Only a DRIVER may register / update their own operational data
                        .requestMatchers(HttpMethod.POST, "/api/v1/drivers").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/drivers/*/vehicle").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/drivers/*/availability").hasRole("DRIVER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/drivers/*/location").hasRole("DRIVER")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
