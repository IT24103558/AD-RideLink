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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** REST client for the Fare & Payment Service (port 8084). The caller's JWT is forwarded. */
@Component
public class FareClient {

    private static final Logger log = LoggerFactory.getLogger(FareClient.class);

    /** Body of POST /api/v1/fares/final (field names match the Fare Service's FinalFareRequest). */
    public record FinalFareBody(String rideId, String passengerId, String driverUserId, String vehicleType,
                                double distanceKm, double durationMinutes) {
    }

    private final RestClient restClient;

    public FareClient(@Value("${services.fare.base-url}") String baseUrl,
                      @Value("${services.timeout-ms:5000}") long timeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** Best effort: a ride can be requested even if the Fare Service is down (no estimate then). */
    public Optional<FareInfo> estimate(String token, String vehicleType, double pickupLat, double pickupLng,
                                       double destLat, double destLng) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (vehicleType != null && !vehicleType.isBlank()) {
            body.put("vehicleType", vehicleType);
        }
        body.put("pickupLatitude", pickupLat);
        body.put("pickupLongitude", pickupLng);
        body.put("destinationLatitude", destLat);
        body.put("destinationLongitude", destLng);
        try {
            FareInfo info = restClient.post().uri("/api/v1/fares/estimate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(FareInfo.class);
            return Optional.ofNullable(info);
        } catch (RestClientException e) {
            log.warn("Fare estimate unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** POST /api/v1/fares/final. Throws ApiException when the Fare Service cannot be used. */
    public FareInfo createFinalFare(String token, FinalFareBody body) {
        try {
            FareInfo info = restClient.post().uri("/api/v1/fares/final")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve().body(FareInfo.class);
            if (info == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "FARE_SERVICE_ERROR", "Fare service returned an empty response");
            }
            return info;
        } catch (RestClientResponseException e) {
            log.warn("Fare service returned {} for /fares/final", e.getStatusCode());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FARE_SERVICE_ERROR",
                    "Fare service returned an error (" + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            log.warn("Fare service unreachable: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FARE_SERVICE_UNAVAILABLE",
                    "Fare service is not reachable right now");
        }
    }
}
