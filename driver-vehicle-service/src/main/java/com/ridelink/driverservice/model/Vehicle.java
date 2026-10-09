package com.ridelink.driverservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    private String plateNumber;
    private String vehicleType;   // e.g. CAR, VAN, TUK
    private String model;
    private int capacity;
}
