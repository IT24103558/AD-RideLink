package com.ridelink.rideservice.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Both fields are optional; when omitted the estimates are used for the final fare. */
public record CompleteRideRequest(
        @PositiveOrZero(message = "actualDistanceKm must be >= 0") Double actualDistanceKm,
        @PositiveOrZero(message = "actualDurationMinutes must be >= 0") Double actualDurationMinutes
) {
}
