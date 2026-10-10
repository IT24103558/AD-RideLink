package com.ridelink.fareservice.exception;

import com.ridelink.fareservice.dto.PaymentResponse;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a simulated payment transaction fails.
 * Returns HTTP 402 PAYMENT_REQUIRED as specified in the assignment test flow.
 */
public class PaymentFailedException extends ApiException {

    private final PaymentResponse paymentResponse;

    public PaymentFailedException(String message, PaymentResponse paymentResponse) {
        super(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", message);
        this.paymentResponse = paymentResponse;
    }

    public PaymentResponse getPaymentResponse() {
        return paymentResponse;
    }
}
