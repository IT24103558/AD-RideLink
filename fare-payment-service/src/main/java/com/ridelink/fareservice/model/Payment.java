package com.ridelink.fareservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MongoDB document representing a simulated payment transaction.
 * Stored in the 'payments' collection.
 * 
 * IMPORTANT: This is a purely simulated payment record.
 * Never accept or store real credit card numbers, CVVs, passwords, or bank credentials.
 */
@Document(collection = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private String id;

    /** Reference to the Ride Management Service's stable ride ID. */
    @Indexed
    private String rideId;

    /** Reference to the Account Service's passenger user ID. */
    @Indexed
    private String passengerId;

    /** Payment amount charged. */
    private BigDecimal amount;

    /** Currency code, e.g. LKR. */
    private String currency;

    /** Simulated payment method (CASH, CARD_SIMULATED, etc.). */
    private PaymentMethod paymentMethod;

    /** Transaction status: SUCCESS, FAILED, PENDING. */
    private PaymentStatus status;

    /** Simulated transaction reference identifier (e.g. TXN-...). */
    private String transactionReference;

    /** Explanation if the simulated transaction failed. */
    private String failureReason;

    /** Optional audit notes. */
    private String remarks;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
