package com.ridelink.fareservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.ridelink.fareservice.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request payload for processing a simulated payment.
 * Never supply or store real payment credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for processing a simulated payment")
public class PaymentRequest {

    @NotBlank(message = "Ride ID is required")
    @Schema(description = "Stable identifier of the ride being paid for", example = "ride-101", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rideId;

    @Schema(description = "Passenger user ID (optional; if omitted, taken from authenticated token)", example = "pass-001")
    private String passengerId;

    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    @Schema(description = "Payment amount (if omitted, fetched from finalized ride fare)", example = "1050.00")
    private BigDecimal amount;

    @JsonAlias("method")
    @Schema(description = "Payment method (CASH, CARD_SIMULATED, CARD, WALLET_SIMULATED)", example = "CARD_SIMULATED")
    private PaymentMethod paymentMethod;

    @Schema(description = "Currency code (default LKR)", example = "LKR")
    private String currency;

    @Schema(description = "Flag to simulate payment failure for testing negative scenarios", example = "false")
    private Boolean simulateFailure;

    @Schema(description = "Simulated failure reason if simulateFailure is true", example = "INSUFFICIENT_FUNDS")
    private String failureReason;
}
