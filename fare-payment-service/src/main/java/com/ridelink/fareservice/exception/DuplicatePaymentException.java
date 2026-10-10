package com.ridelink.fareservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to process payment for a ride that has already been successfully paid.
 * Results in HTTP 409 CONFLICT.
 */
public class DuplicatePaymentException extends ApiException {

    public DuplicatePaymentException(String rideId) {
        super(HttpStatus.CONFLICT, "DUPLICATE_PAYMENT",
                "Payment has already been successfully completed for ride ID: " + rideId);
    }
}
