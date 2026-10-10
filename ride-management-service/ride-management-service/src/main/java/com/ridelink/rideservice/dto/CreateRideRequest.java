package com.ridelink.rideservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** vehicleType (e.g. CAR, VAN, TUK) and serviceArea are optional filters used when picking a driver. */
public record CreateRideRequest(
        @NotNull(message = "pickup is required") @Valid PlaceDto pickup,
        @NotNull(message = "destination is required") @Valid PlaceDto destination,
        @Size(max = 30, message = "vehicleType is too long") String vehicleType,
        @Size(max = 100, message = "serviceArea is too long") String serviceArea
) {
}
