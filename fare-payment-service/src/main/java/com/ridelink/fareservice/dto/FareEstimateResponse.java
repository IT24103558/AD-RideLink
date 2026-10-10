package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload containing estimated ride fare and transparent breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing estimated fare calculation and breakdown")
public class FareEstimateResponse {

    @Schema(description = "Ride identifier associated with this estimate", example = "ride-101")
    private String rideId;

    @Schema(description = "Estimated total fare amount", example = "720.00")
    private BigDecimal estimatedFare;

    /** Alias for interservice client compatibility (FareInfo.amount). */
    @Schema(description = "Total fare amount alias", example = "720.00")
    private BigDecimal amount;

    @Schema(description = "Currency of the fare", example = "LKR")
    private String currency;

    @Schema(description = "Distance in kilometres used for calculation", example = "5.2")
    private Double distanceKm;

    @Schema(description = "Vehicle category applied", example = "CAR")
    private String vehicleType;

    @Schema(description = "Base fare amount applied", example = "200.00")
    private BigDecimal baseFare;

    @Schema(description = "Rate applied per kilometre", example = "100.00")
    private BigDecimal ratePerKm;

    @Schema(description = "Distance fare portion (distance * ratePerKm)", example = "520.00")
    private BigDecimal distanceFare;

    @Schema(description = "Vehicle multiplier applied", example = "1.0")
    private BigDecimal vehicleMultiplier;

    @Schema(description = "Human-readable pricing breakdown formula", 
            example = "Fare = Base Fare (200.00 LKR) + (5.20 km x 100.00 LKR/km) x Multiplier 1.0 = 720.00 LKR")
    private String formula;

    @Schema(description = "Timestamp when the estimate was generated")
    private LocalDateTime calculatedAt;
}
