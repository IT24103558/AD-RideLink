package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for estimating ride fare.
 * Accepts ride ID, pickup & destination locations or coordinates, and estimated distance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for calculating an estimated fare")
public class FareEstimateRequest {

    @Schema(description = "Ride identifier (optional if pre-booking estimate)", example = "ride-101")
    private String rideId;

    @Schema(description = "Pickup location or address", example = "Colombo Fort Station")
    private String pickupLocation;

    @Schema(description = "Destination location or address", example = "Galle Face Green")
    private String destinationLocation;

    @Schema(description = "Pickup GPS Latitude", example = "6.9344")
    private Double pickupLatitude;

    @Schema(description = "Pickup GPS Longitude", example = "79.8525")
    private Double pickupLongitude;

    @Schema(description = "Destination GPS Latitude", example = "6.9271")
    private Double destinationLatitude;

    @Schema(description = "Destination GPS Longitude", example = "79.8436")
    private Double destinationLongitude;

    @DecimalMin(value = "0.01", message = "Estimated distance in kilometres must be greater than zero")
    @Schema(description = "Estimated distance in kilometres (must be greater than 0 if provided)", example = "5.2")
    private Double distanceKm;

    @Schema(description = "Vehicle category: CAR, BIKE, or VAN", example = "CAR")
    private String vehicleType;
}
