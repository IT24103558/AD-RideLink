package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.dto.ErrorResponse;
import com.ridelink.fareservice.dto.PaymentRequest;
import com.ridelink.fareservice.dto.PaymentResponse;
import com.ridelink.fareservice.dto.ReceiptResponse;
import com.ridelink.fareservice.service.PaymentService;
import com.ridelink.fareservice.service.ReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
@Tag(name = "Payment & Receipt Processing", description = "Endpoints for simulated payments and official receipt generation")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReceiptService receiptService;

    @PostMapping
    @Operation(summary = "Process simulated payment",
            description = "Simulates processing a payment for a ride (CASH or CARD_SIMULATED). Supports failure simulation via 'simulateFailure': true.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simulated payment successfully completed",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or negative amount",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "402", description = "Simulated payment failed / declined",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Payment already completed for this ride",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment transaction by ID",
            description = "Retrieves a payment transaction record using its unique payment ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment transaction record found",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment ID not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get payment transaction by ride ID",
            description = "Retrieves the payment transaction record associated with a given ride ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment transaction record found",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "No payment transaction found for this ride ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PaymentResponse> getPaymentByRideId(@PathVariable String rideId) {
        PaymentResponse response = paymentService.getPaymentByRideId(rideId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Get or generate receipt for a payment",
            description = "Generates or retrieves an official paid receipt. Only succeeds if the payment was successful.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Receipt retrieved or generated successfully",
                    content = @Content(schema = @Schema(implementation = ReceiptResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payment failed; cannot issue receipt",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment ID not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReceiptResponse> getReceiptByPaymentId(@PathVariable String paymentId) {
        ReceiptResponse response = receiptService.getOrCreateReceiptByPaymentId(paymentId);
        return ResponseEntity.ok(response);
    }
}
