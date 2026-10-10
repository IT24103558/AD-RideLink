package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.PaymentRequest;
import com.ridelink.fareservice.dto.PaymentResponse;

public interface PaymentService {

    /**
     * Processes a simulated payment for a completed ride.
     * Prevents duplicate payments and supports simulated success/failure outcomes.
     */
    PaymentResponse processPayment(PaymentRequest request);

    /**
     * Retrieves a payment transaction record by its unique payment ID.
     */
    PaymentResponse getPaymentById(String paymentId);

    /**
     * Retrieves the latest payment transaction for a given ride ID.
     */
    PaymentResponse getPaymentByRideId(String rideId);
}
