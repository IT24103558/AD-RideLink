package com.ridelink.fareservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MongoDB document representing a calculated or estimated fare for a ride.
 * Stored in the 'fares' collection inside the Fare & Payment Service database.
 */
@Document(collection = "fares")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fare {

    @Id
    private String id;

    /** Reference to the Ride Management Service's stable ride ID. */
    @Indexed
    private String rideId;

    /** Reference to the Account Service's passenger ID. */
    private String passengerId;

    /** Reference to the Driver & Vehicle Service's driver user ID. */
    private String driverId;

    /** Type of vehicle assigned to the ride (e.g. CAR, BIKE, VAN). */
    private String vehicleType;

    /** Pickup address or human-readable location. */
    private String pickupLocation;

    /** Destination address or human-readable location. */
    private String destinationLocation;

    /** Optional GPS latitude of pickup. */
    private Double pickupLatitude;

    /** Optional GPS longitude of pickup. */
    private Double pickupLongitude;

    /** Optional GPS latitude of destination. */
    private Double destinationLatitude;

    /** Optional GPS longitude of destination. */
    private Double destinationLongitude;

    /** Distance in kilometres. */
    private Double distanceKm;

    /** Travel duration in minutes. */
    private Double durationMinutes;

    /** Base fare applied. */
    private BigDecimal baseFare;

    /** Fare component based on distance (distanceKm * ratePerKm). */
    private BigDecimal distanceFare;

    /** Fare component based on duration (durationMinutes * ratePerMinute). */
    private BigDecimal durationFare;

    /** Multiplier applied based on vehicle type (e.g. 1.0 for CAR). */
    private BigDecimal vehicleMultiplier;

    /** Final calculated fare amount. */
    private BigDecimal totalFare;

    /** Currency code, e.g. LKR. */
    private String currency;

    /** Lifecycle status: ESTIMATED or CALCULATED. */
    private FareStatus status;

    /** Detailed breakdown of calculation formula. */
    private String pricingBreakdown;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
