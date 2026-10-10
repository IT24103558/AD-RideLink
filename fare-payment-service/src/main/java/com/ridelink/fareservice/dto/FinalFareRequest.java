package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for calculating and persisting the final fare for a completed ride.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for calculating final completed ride fare")
public class FinalFareRequest {

    @NotBlank(message = "Ride ID is required")
    @Schema(description = "Stable identifier of the completed ride", example = "ride-101", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rideId;

    @Schema(description = "Passenger identifier from Account Service", example = "pass-001")
    private String passengerId;

    @Schema(description = "Driver identifier from Driver & Vehicle Service", example = "driver-001")
    private String driverUserId;

    @Schema(description = "Vehicle category (e.g. CAR, BIKE, VAN)", example = "CAR")
    private String vehicleType;

    @NotNull(message = "Distance in kilometres is required")
    @DecimalMin(value = "0.01", message = "Completed ride distance must be greater than zero")
    @Schema(description = "Actual trip distance in kilometres (must be > 0)", example = "8.5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Double distanceKm;

    @DecimalMin(value = "0.0", message = "Trip duration cannot be negative")
    @Schema(description = "Actual trip duration in minutes (>= 0)", example = "22.0")
    private Double durationMinutes;

    @Schema(description = "Optional pickup location address", example = "Colombo Fort Station")
    private String pickupLocation;

    @Schema(description = "Optional destination location address", example = "Mount Lavinia Beach")
    private String destinationLocation;
}
