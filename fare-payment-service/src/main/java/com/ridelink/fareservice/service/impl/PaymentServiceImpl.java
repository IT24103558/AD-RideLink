package com.ridelink.fareservice.service.impl;

import com.ridelink.fareservice.dto.PaymentRequest;
import com.ridelink.fareservice.dto.PaymentResponse;
import com.ridelink.fareservice.exception.DuplicatePaymentException;
import com.ridelink.fareservice.exception.InvalidPaymentException;
import com.ridelink.fareservice.exception.PaymentFailedException;
import com.ridelink.fareservice.exception.ResourceNotFoundException;
import com.ridelink.fareservice.model.Fare;
import com.ridelink.fareservice.model.FareStatus;
import com.ridelink.fareservice.model.Payment;
import com.ridelink.fareservice.model.PaymentMethod;
import com.ridelink.fareservice.model.PaymentStatus;
import com.ridelink.fareservice.repository.FareRepository;
import com.ridelink.fareservice.repository.PaymentRepository;
import com.ridelink.fareservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        if (request == null) {
            throw new InvalidPaymentException("Payment request body cannot be null");
        }

        String rideId = request.getRideId();
        if (rideId == null || rideId.isBlank()) {
            throw new InvalidPaymentException("Ride ID is required to process payment");
        }

        // Rule: Prevent duplicate payments if a payment was already SUCCESS for this ride
        if (paymentRepository.existsByRideIdAndStatus(rideId, PaymentStatus.SUCCESS)) {
            log.warn("Attempt to re-pay already successful ride [{}]", rideId);
            throw new DuplicatePaymentException(rideId);
        }

        // Resolve passenger ID: from request or from JWT principal
        String passengerId = request.getPassengerId();
        if (passengerId == null || passengerId.isBlank()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
                passengerId = auth.getName();
            } else {
                passengerId = "PASSENGER-ANONYMOUS";
            }
        }

        // Resolve payment method (default to CARD_SIMULATED if unspecified)
        PaymentMethod method = (request.getPaymentMethod() != null)
                ? request.getPaymentMethod()
                : PaymentMethod.CARD_SIMULATED;

        // Resolve amount: from request or from Fare repository
        BigDecimal amount = request.getAmount();
        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency()
                : "LKR";

        if (amount == null) {
            // Fetch finalized fare from database
            Optional<Fare> fareOpt = fareRepository.findByRideIdAndStatus(rideId, FareStatus.CALCULATED);
            if (fareOpt.isPresent()) {
                amount = fareOpt.get().getTotalFare();
                currency = fareOpt.get().getCurrency();
            } else {
                // Check if any fare exists for rideId
                Optional<Fare> anyFare = fareRepository.findByRideId(rideId);
                if (anyFare.isPresent()) {
                    amount = anyFare.get().getTotalFare();
                    currency = anyFare.get().getCurrency();
                } else {
                    throw new InvalidPaymentException(
                            "Payment amount was not specified and no finalized fare record was found for ride ID: " + rideId);
                }
            }
        }

        // Validate amount is positive
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero. Received: " + amount);
        }

        // Generate unique payment ID and simulated transaction reference
        String paymentId = "PAY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        String txnRef = "TXN-SIM-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Check if simulated failure is requested
        boolean isSimulatedFailure = Boolean.TRUE.equals(request.getSimulateFailure());

        if (isSimulatedFailure) {
            String failureReason = (request.getFailureReason() != null && !request.getFailureReason().isBlank())
                    ? request.getFailureReason()
                    : "SIMULATED_TRANSACTION_DECLINED";

            Payment failedPayment = Payment.builder()
                    .id(paymentId)
                    .rideId(rideId)
                    .passengerId(passengerId)
                    .amount(amount)
                    .currency(currency)
                    .paymentMethod(method)
                    .status(PaymentStatus.FAILED)
                    .transactionReference(txnRef)
                    .failureReason(failureReason)
                    .remarks("Simulated negative test scenario: payment declined")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Payment savedFailed = paymentRepository.save(failedPayment);
            log.warn("Simulated payment FAILED recorded for ride [{}], payment ID [{}]", rideId, savedFailed.getId());

            PaymentResponse failedResponse = mapToResponse(savedFailed);
            throw new PaymentFailedException("Simulated payment transaction failed: " + failureReason, failedResponse);
        }

        // Successful Payment
        Payment successfulPayment = Payment.builder()
                .id(paymentId)
                .rideId(rideId)
                .passengerId(passengerId)
                .amount(amount)
                .currency(currency)
                .paymentMethod(method)
                .status(PaymentStatus.SUCCESS)
                .transactionReference(txnRef)
                .failureReason(null)
                .remarks("Simulated payment processed successfully")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(successfulPayment);
        log.info("Simulated payment SUCCESS recorded for ride [{}], payment ID [{}] amount {} {}",
                rideId, savedPayment.getId(), savedPayment.getAmount(), savedPayment.getCurrency());

        return mapToResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        return mapToResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByRideId(String rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for ride ID", rideId));
        return mapToResponse(payment);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .rideId(payment.getRideId())
                .passengerId(payment.getPassengerId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
