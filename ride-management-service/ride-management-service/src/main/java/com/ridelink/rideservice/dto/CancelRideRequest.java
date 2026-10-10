package com.ridelink.rideservice.dto;

import jakarta.validation.constraints.Size;

public record CancelRideRequest(
        @Size(max = 255, message = "reason must be at most 255 characters") String reason
) {
}
