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
 * Official receipt response generated exclusively for successful payments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Official payment receipt issued for a completed, successful transaction")
public class ReceiptResponse {

    @Schema(description = "Unique receipt ID", example = "REC-8234ab1c")
    private String receiptId;

    @Schema(description = "Reference to the successful payment transaction ID", example = "PAY-9a8b7c6d5e")
    private String paymentId;

    @Schema(description = "Reference to the ride ID", example = "ride-101")
    private String rideId;

    @Schema(description = "Passenger user ID", example = "pass-001")
    private String passengerId;

    @Schema(description = "Amount paid", example = "1050.00")
    private BigDecimal amount;

    @Schema(description = "Currency", example = "LKR")
    private String currency;

    @Schema(description = "Payment method used", example = "CARD_SIMULATED")
    private PaymentMethod paymentMethod;

    @Schema(description = "Payment status (always SUCCESS for issued receipts)", example = "SUCCESS")
    private PaymentStatus paymentStatus;

    @Schema(description = "Timestamp when the receipt was officially issued")
    private LocalDateTime issuedAt;

    @Schema(description = "Receipt notes or verification statement", 
            example = "Thank you for riding with RideLink. Payment verified successfully.")
    private String receiptNotes;
}
