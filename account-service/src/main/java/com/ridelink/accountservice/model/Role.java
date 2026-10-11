package com.ridelink.accountservice.model;

/** Roles carried in the JWT "role" claim and used by every RideLink service. */
public enum Role {
    PASSENGER,
    DRIVER,
    ADMIN
}
