package com.ridelink.rideservice.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * REQUESTED  -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED
 *                  |  (driver rejects -> back to REQUESTED)
 * CANCELLED is possible from REQUESTED, ASSIGNED or ACCEPTED (not once the trip has started).
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;



    
    /** Rides that still occupy the passenger. */
    public static final Set<RideStatus> ACTIVE = EnumSet.of(REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS);
    /** Rides that occupy a driver. */
    public static final Set<RideStatus> DRIVER_OCCUPIED = EnumSet.of(ASSIGNED, ACCEPTED, IN_PROGRESS);
}
