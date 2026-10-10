package com.ridelink.fareservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource (Fare, Payment, Receipt) cannot be found in MongoDB.
 * Results in HTTP 404 NOT FOUND.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, String id) {
        super(HttpStatus.NOT_FOUND, resourceName.toUpperCase() + "_NOT_FOUND",
                resourceName + " not found with identifier: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}
