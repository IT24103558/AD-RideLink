package com.ridelink.rideservice.dto;

import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.RideStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RideResponse(
        String id,
        String passengerId,
        PlaceDto pickup,
        PlaceDto destination,
        RideStatus status,
        String requestedVehicleType,
        String serviceArea,
        String driverProfileId,
        String driverUserId,
        String driverName,
        String driverPhone,
        String vehiclePlate,
        String vehicleType,
        String vehicleModel,
        Double estimatedDistanceKm,
        Double estimatedDurationMinutes,
        BigDecimal estimatedFare,
        Double actualDistanceKm,
        Double actualDurationMinutes,
        BigDecimal finalFare,
        String currency,
        String fareId,
        String fareStatus,
        String cancelReason,
        String cancelledByRole,
        Instant requestedAt,
        Instant assignedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant updatedAt
) {
    public static RideResponse from(Ride r) {
        return new RideResponse(r.getId(), r.getPassengerId(), PlaceDto.from(r.getPickup()),
                PlaceDto.from(r.getDestination()), r.getStatus(), r.getRequestedVehicleType(), r.getServiceArea(),
                r.getDriverProfileId(), r.getDriverUserId(), r.getDriverName(), r.getDriverPhone(),
                r.getVehiclePlate(), r.getVehicleType(), r.getVehicleModel(), r.getEstimatedDistanceKm(),
                r.getEstimatedDurationMinutes(), r.getEstimatedFare(), r.getActualDistanceKm(),
                r.getActualDurationMinutes(), r.getFinalFare(), r.getCurrency(), r.getFareId(), r.getFareStatus(),
                r.getCancelReason(), r.getCancelledByRole(), r.getRequestedAt(), r.getAssignedAt(),
                r.getAcceptedAt(), r.getStartedAt(), r.getCompletedAt(), r.getCancelledAt(), r.getUpdatedAt());
    }
}
