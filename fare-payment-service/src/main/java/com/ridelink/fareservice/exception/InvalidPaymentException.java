package com.ridelink.fareservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when payment parameters or calculations are invalid.
 * Results in HTTP 400 BAD REQUEST.
 */
public class InvalidPaymentException extends ApiException {

    public InvalidPaymentException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT", message);
    }
}
