package com.ridelink.driverservice.dto;

import com.ridelink.driverservice.model.AvailabilityStatus;
import com.ridelink.driverservice.model.Location;
import com.ridelink.driverservice.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * This is the CONTRACT consumed by the Ride Management Service when it
 * calls GET /api/v1/drivers/available. Keep field names stable - changing
 * them requires updating the agreed contract with the Ride Service owner.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverResponseDTO {

    private String id;
    private String userId;
    private String fullName;
    private String licenseNumber;
    private String phoneNumber;
    private Vehicle vehicle;
    private AvailabilityStatus availabilityStatus;
    private String serviceArea;
    private Location currentLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
