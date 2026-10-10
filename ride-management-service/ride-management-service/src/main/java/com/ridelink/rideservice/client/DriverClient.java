package com.ridelink.rideservice.client;

import com.ridelink.rideservice.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** REST client for the Driver & Vehicle Service (port 8082). The caller's JWT is forwarded. */
@Component
public class DriverClient {

    private static final Logger log = LoggerFactory.getLogger(DriverClient.class);

    private final RestClient restClient;

    public DriverClient(@Value("${services.driver.base-url}") String baseUrl,
                        @Value("${services.timeout-ms:5000}") long timeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /**
     * GET /api/v1/drivers/available. The Driver Service answers 404 NO_DRIVER_AVAILABLE when nobody is
     * free - that is a normal situation here and is returned as an empty list.
     */
    public List<DriverInfo> getAvailableDrivers(String token, String serviceArea) {
        try {
            DriverInfo[] drivers;
            if (serviceArea == null || serviceArea.isBlank()) {
                drivers = restClient.get().uri("/api/v1/drivers/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(DriverInfo[].class);
            } else {
                drivers = restClient.get().uri("/api/v1/drivers/available?serviceArea={area}", serviceArea)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().body(DriverInfo[].class);
            }
            return drivers == null ? List.of() : List.of(drivers);
        } catch (HttpClientErrorException.NotFound e) {
            return List.of();
        } catch (RestClientResponseException e) {
            log.warn("Driver service returned {} for /available", e.getStatusCode());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "DRIVER_SERVICE_ERROR",
                    "Driver service returned an error (" + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            log.warn("Driver service unreachable: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DRIVER_SERVICE_UNAVAILABLE",
                    "Driver service is not reachable right now");
        }
    }

    /** Best effort: PATCH /api/v1/drivers/{id}/availability. Returns false (never throws) on failure. */
    public boolean setAvailability(String token, String driverProfileId, String status) {
        try {
            restClient.patch().uri("/api/v1/drivers/{id}/availability", driverProfileId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", status))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException e) {
            log.warn("Could not set driver {} availability to {}: {}", driverProfileId, status, e.getMessage());
            return false;
        }
    }
}
