package com.ridelink.fareservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MongoDB document representing an official issued receipt.
 * Receipts are ONLY issued for successfully completed payments.
 * Stored in the 'receipts' collection.
 */
@Document(collection = "receipts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Receipt {

    @Id
    private String id;

    /** Unique reference to the successful payment transaction. */
    @Indexed(unique = true)
    private String paymentId;

    /** Reference to the ride identifier. */
    @Indexed
    private String rideId;

    /** Reference to the paying passenger user ID. */
    private String passengerId;

    /** Total amount paid. */
    private BigDecimal amount;

    /** Currency code, e.g. LKR. */
    private String currency;

    /** Payment method used. */
    private PaymentMethod paymentMethod;

    /** Payment status (Must be SUCCESS for an official receipt). */
    private PaymentStatus paymentStatus;

    /** Timestamp when this receipt was issued. */
    @CreatedDate
    private LocalDateTime issuedAt;

    /** Descriptive receipt statement or footer text. */
    private String receiptNotes;
}
