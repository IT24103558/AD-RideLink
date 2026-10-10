package com.ridelink.fareservice.model;

/**
 * Supported payment methods for simulated ride transactions.
 * Never involves real payment credentials or external payment gateways.
 */
public enum PaymentMethod {
    CASH,
    CARD_SIMULATED,
    CARD,
    WALLET_SIMULATED
}
