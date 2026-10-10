package com.ridelink.fareservice.service.impl;

import com.ridelink.fareservice.dto.ReceiptResponse;
import com.ridelink.fareservice.exception.ReceiptNotEligibleException;
import com.ridelink.fareservice.exception.ResourceNotFoundException;
import com.ridelink.fareservice.model.Payment;
import com.ridelink.fareservice.model.PaymentStatus;
import com.ridelink.fareservice.model.Receipt;
import com.ridelink.fareservice.repository.PaymentRepository;
import com.ridelink.fareservice.repository.ReceiptRepository;
import com.ridelink.fareservice.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptServiceImpl.class);

    private final ReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public ReceiptResponse getOrCreateReceiptByPaymentId(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalArgumentException("Payment ID cannot be null or blank");
        }

        // 1. Check if a receipt already exists for this payment (prevent duplicate receipts)
        Optional<Receipt> existingReceipt = receiptRepository.findByPaymentId(paymentId);
        if (existingReceipt.isPresent()) {
            log.info("Receipt already issued for payment [{}]. Returning existing receipt.", paymentId);
            return mapToResponse(existingReceipt.get());
        }

        // 2. Fetch the corresponding payment record
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        // 3. Rule: Only issue a paid receipt when the payment is successful
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            log.warn("Attempt to generate receipt for unsuccessful payment [{}]. Status is [{}]",
                    paymentId, payment.getStatus());
            throw new ReceiptNotEligibleException(
                    "Cannot issue or retrieve a receipt for a failed or non-successful payment. " +
                    "Payment status is: " + payment.getStatus() +
                    (payment.getFailureReason() != null ? " (" + payment.getFailureReason() + ")" : ""));
        }

        // 4. Generate and persist new official receipt
        String receiptId = "REC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        Receipt receipt = Receipt.builder()
                .id(receiptId)
                .paymentId(payment.getId())
                .rideId(payment.getRideId())
                .passengerId(payment.getPassengerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getStatus())
                .issuedAt(LocalDateTime.now())
                .receiptNotes("Official RideLink electronic receipt. Payment verified successfully.")
                .build();

        Receipt savedReceipt = receiptRepository.save(receipt);
        log.info("Issued official receipt [{}] for successful payment [{}] on ride [{}]",
                savedReceipt.getId(), payment.getId(), payment.getRideId());

        return mapToResponse(savedReceipt);
    }

    @Override
    public ReceiptResponse getReceiptByRideId(String rideId) {
        if (rideId == null || rideId.isBlank()) {
            throw new IllegalArgumentException("Ride ID cannot be null or blank");
        }

        // Check if receipt already issued for this ride
        Optional<Receipt> existing = receiptRepository.findByRideId(rideId);
        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        // Check if a successful payment exists for this ride
        Payment successfulPayment = paymentRepository.findByRideIdAndStatus(rideId, PaymentStatus.SUCCESS)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt", "No successful payment found to issue receipt for ride ID: " + rideId));

        return getOrCreateReceiptByPaymentId(successfulPayment.getId());
    }

    private ReceiptResponse mapToResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .receiptId(receipt.getId())
                .paymentId(receipt.getPaymentId())
                .rideId(receipt.getRideId())
                .passengerId(receipt.getPassengerId())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .paymentMethod(receipt.getPaymentMethod())
                .paymentStatus(receipt.getPaymentStatus())
                .issuedAt(receipt.getIssuedAt())
                .receiptNotes(receipt.getReceiptNotes())
                .build();
    }
}
