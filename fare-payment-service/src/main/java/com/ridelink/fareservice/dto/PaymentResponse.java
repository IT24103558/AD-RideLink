package com.ridelink.fareservice.dto;

import com.ridelink.fareservice.model.PaymentMethod;
import com.ridelink.fareservice.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload representing a simulated payment transaction result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing simulated payment outcome and transaction record")
public class PaymentResponse {

    @Schema(description = "Unique payment transaction ID", example = "PAY-9a8b7c6d5e")
    private String paymentId;

    @Schema(description = "Associated ride ID", example = "ride-101")
    private String rideId;

    @Schema(description = "Passenger ID who paid", example = "pass-001")
    private String passengerId;

    @Schema(description = "Amount charged", example = "1050.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "LKR")
    private String currency;

    @Schema(description = "Payment method used", example = "CARD_SIMULATED")
    private PaymentMethod paymentMethod;

    @Schema(description = "Transaction status: SUCCESS or FAILED", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "Simulated bank or gateway transaction reference", example = "TXN-SIM-20261010-8472")
    private String transactionReference;

    @Schema(description = "Failure reason if status is FAILED", example = "INSUFFICIENT_FUNDS")
    private String failureReason;

    @Schema(description = "Timestamp when the payment transaction was recorded")
    private LocalDateTime createdAt;
}
