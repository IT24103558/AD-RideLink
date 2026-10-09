package com.ridelink.driverservice.exception;

/**
 * Thrown when no driver matching the requested criteria is available.
 * This maps directly to the "no available driver" negative scenario
 * required by the assignment brief.
 */
public class NoAvailableDriverException extends RuntimeException {
    public NoAvailableDriverException(String message) {
        super(message);
    }
}
