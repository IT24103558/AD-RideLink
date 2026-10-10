package com.ridelink.rideservice.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * passengerId and driverUserId are ACCOUNT ids (JWT subjects, from the Account Service).
 * driverProfileId is the id of the driver's profile in the Driver & Vehicle Service.
 * Driver/vehicle fields are snapshots taken at assignment time (no cross-database reads).
 */
@Document(collection = "rides")
@Getter
@Setter
@NoArgsConstructor
public class Ride {

    @Id
    private String id;

    @Version
    private Long version;

    @Indexed
    private String passengerId;
    private Place pickup;
    private Place destination;
    private String requestedVehicleType;
    private String serviceArea;

    @Indexed
    private RideStatus status;

    // assigned driver (snapshot)
    private String driverProfileId;
    @Indexed
    private String driverUserId;
    private String driverName;
    private String driverPhone;
    private String vehiclePlate;
    private String vehicleType;
    private String vehicleModel;
    private List<String> rejectedDriverProfileIds = new ArrayList<>();

    // fare
    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private BigDecimal estimatedFare;
    private Double actualDistanceKm;
    private Double actualDurationMinutes;
    private BigDecimal finalFare;
    private String currency;
    private String fareId;
    /** null until completion, then PENDING (fare service not reached yet) or CALCULATED. */
    private String fareStatus;

    private String cancelReason;
    private String cancelledByRole;

    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant updatedAt;
}
