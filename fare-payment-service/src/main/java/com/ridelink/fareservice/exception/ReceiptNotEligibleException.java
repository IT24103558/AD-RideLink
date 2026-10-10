package com.ridelink.fareservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to issue or retrieve a receipt for a payment that is not successful.
 * Results in HTTP 400 BAD REQUEST.
 */
public class ReceiptNotEligibleException extends ApiException {

    public ReceiptNotEligibleException(String message) {
        super(HttpStatus.BAD_REQUEST, "RECEIPT_NOT_ELIGIBLE", message);
    }
}
