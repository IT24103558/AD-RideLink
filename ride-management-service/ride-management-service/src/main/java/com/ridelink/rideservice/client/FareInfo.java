package com.ridelink.rideservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** The part of the Fare & Payment Service's FareResponse that this service uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FareInfo(
        String id,
        BigDecimal amount,
        String currency,
        Double distanceKm,
        Double durationMinutes
) {
}
