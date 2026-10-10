package com.ridelink.fareservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RideLink - Fare & Payment Service
 * Microservice owned by Member 4.
 * 
 * Responsibilities:
 * 1. Fare estimation based on distance and locations.
 * 2. Final fare calculation for completed rides.
 * 3. Simulated payment processing with idempotency and simulated outcomes.
 * 4. Electronic receipt generation and retrieval.
 */
@SpringBootApplication
public class FarePaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarePaymentServiceApplication.class, args);
    }
}
