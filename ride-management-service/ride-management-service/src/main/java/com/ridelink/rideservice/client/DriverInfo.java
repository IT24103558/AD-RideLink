package com.ridelink.rideservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** The part of the Driver & Vehicle Service's DriverResponseDTO that this service uses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DriverInfo(
        String id,
        String userId,
        String fullName,
        String phoneNumber,
        VehicleInfo vehicle,
        String availabilityStatus,
        String serviceArea,
        LocationInfo currentLocation
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VehicleInfo(String plateNumber, String vehicleType, String model, Integer capacity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LocationInfo(Double latitude, Double longitude) {
    }
}
