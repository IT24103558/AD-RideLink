package com.ridelink.rideservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pickup / destination (simulated coordinates - no real map integration). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Place {
    private String address;
    private double latitude;
    private double longitude; 
    
}
