package com.ridelink.rideservice.dto;

import com.ridelink.rideservice.model.Place;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceDto(
        @NotBlank(message = "address is required") String address,
        @NotNull(message = "latitude is required") @DecimalMin(value = "-90", message = "latitude must be >= -90") @DecimalMax(value = "90", message = "latitude must be <= 90") Double latitude,
        @NotNull(message = "longitude is required") @DecimalMin(value = "-180", message = "longitude must be >= -180") @DecimalMax(value = "180", message = "longitude must be <= 180") Double longitude
) {
    public Place toPlace() {
        return new Place(address.trim(), latitude, longitude);
    }

    public static PlaceDto from(Place p) {
        return p == null ? null : new PlaceDto(p.getAddress(), p.getLatitude(), p.getLongitude());
    }
}
