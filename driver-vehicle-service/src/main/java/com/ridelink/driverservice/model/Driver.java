package com.ridelink.driverservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Driver aggregate root. Vehicle and Location are embedded because they
 * are always accessed together with the driver and have no independent
 * lifecycle of their own.
 *
 * userId is a STABLE REFERENCE to the account created in the Account
 * Service. This service never queries the Account Service's database
 * directly - only this id is stored here.
 */
@Document(collection = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    private String fullName;

    @Indexed(unique = true)
    private String licenseNumber;

    private String phoneNumber;

    private Vehicle vehicle;

    private AvailabilityStatus availabilityStatus;

    private String serviceArea;

    private Location currentLocation;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
