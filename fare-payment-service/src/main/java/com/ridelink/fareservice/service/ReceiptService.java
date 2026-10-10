package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.ReceiptResponse;

public interface ReceiptService {

    /**
     * Generates or retrieves an official paid receipt for a successful payment.
     * Rejects receipt issuance if the payment was unsuccessful or failed.
     */
    ReceiptResponse getOrCreateReceiptByPaymentId(String paymentId);

    /**
     * Retrieves the receipt for a ride ID.
     */
    ReceiptResponse getReceiptByRideId(String rideId);
}
