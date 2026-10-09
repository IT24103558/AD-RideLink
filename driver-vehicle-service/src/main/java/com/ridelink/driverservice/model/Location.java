package com.ridelink.driverservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simulated GPS coordinates for a driver's current position.
 * No real map integration is required for this assignment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    private Double latitude;
    private Double longitude;
}
