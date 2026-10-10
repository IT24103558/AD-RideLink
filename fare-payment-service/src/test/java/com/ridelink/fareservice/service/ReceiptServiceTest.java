package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.ReceiptResponse;
import com.ridelink.fareservice.exception.ReceiptNotEligibleException;
import com.ridelink.fareservice.exception.ResourceNotFoundException;
import com.ridelink.fareservice.model.Payment;
import com.ridelink.fareservice.model.PaymentMethod;
import com.ridelink.fareservice.model.PaymentStatus;
import com.ridelink.fareservice.model.Receipt;
import com.ridelink.fareservice.repository.PaymentRepository;
import com.ridelink.fareservice.repository.ReceiptRepository;
import com.ridelink.fareservice.service.impl.ReceiptServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReceiptService Unit Tests")
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReceiptServiceImpl receiptService;

    @Test
    @DisplayName("Should successfully generate receipt for a successful payment")
    void shouldGenerateReceiptForSuccessfulPayment() {
        when(receiptRepository.findByPaymentId("PAY-001")).thenReturn(Optional.empty());

        Payment successfulPayment = Payment.builder()
                .id("PAY-001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("950.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById("PAY-001")).thenReturn(Optional.of(successfulPayment));

        Receipt savedReceipt = Receipt.builder()
                .id("REC-001")
                .paymentId("PAY-001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("950.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .paymentStatus(PaymentStatus.SUCCESS)
                .issuedAt(LocalDateTime.now())
                .build();

        when(receiptRepository.save(any(Receipt.class))).thenReturn(savedReceipt);

        ReceiptResponse response = receiptService.getOrCreateReceiptByPaymentId("PAY-001");

        assertThat(response).isNotNull();
        assertThat(response.getReceiptId()).isEqualTo("REC-001");
        assertThat(response.getPaymentId()).isEqualTo("PAY-001");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getAmount()).isEqualByComparingTo("950.00");

        verify(receiptRepository, times(1)).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should reject receipt generation for failed payment (ReceiptNotEligibleException)")
    void shouldRejectReceiptForFailedPayment() {
        when(receiptRepository.findByPaymentId("PAY-FAILED")).thenReturn(Optional.empty());

        Payment failedPayment = Payment.builder()
                .id("PAY-FAILED")
                .rideId("ride-001")
                .amount(new BigDecimal("500.00"))
                .status(PaymentStatus.FAILED)
                .failureReason("INSUFFICIENT_FUNDS")
                .build();

        when(paymentRepository.findById("PAY-FAILED")).thenReturn(Optional.of(failedPayment));

        assertThrows(ReceiptNotEligibleException.class, () ->
                receiptService.getOrCreateReceiptByPaymentId("PAY-FAILED")
        );

        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should return existing receipt when already issued (Idempotency - No Duplicate Receipts)")
    void shouldReturnExistingReceiptIfAlreadyIssued() {
        Receipt existingReceipt = Receipt.builder()
                .id("REC-EXISTING")
                .paymentId("PAY-001")
                .rideId("ride-001")
                .amount(new BigDecimal("950.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        when(receiptRepository.findByPaymentId("PAY-001")).thenReturn(Optional.of(existingReceipt));

        ReceiptResponse response = receiptService.getOrCreateReceiptByPaymentId("PAY-001");

        assertThat(response.getReceiptId()).isEqualTo("REC-EXISTING");
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment does not exist")
    void shouldThrowNotFoundWhenPaymentMissing() {
        when(receiptRepository.findByPaymentId("PAY-MISSING")).thenReturn(Optional.empty());
        when(paymentRepository.findById("PAY-MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                receiptService.getOrCreateReceiptByPaymentId("PAY-MISSING")
        );
    }

    @Test
    @DisplayName("Should retrieve receipt by ride ID when successful payment exists")
    void shouldGetReceiptByRideId() {
        when(receiptRepository.findByRideId("ride-001")).thenReturn(Optional.empty());

        Payment payment = Payment.builder()
                .id("PAY-001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("600.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CASH)
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByRideIdAndStatus("ride-001", PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(payment));
        when(paymentRepository.findById("PAY-001")).thenReturn(Optional.of(payment));

        Receipt receipt = Receipt.builder()
                .id("REC-001")
                .paymentId("PAY-001")
                .rideId("ride-001")
                .amount(new BigDecimal("600.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        when(receiptRepository.save(any(Receipt.class))).thenReturn(receipt);

        ReceiptResponse response = receiptService.getReceiptByRideId("ride-001");
        assertThat(response.getReceiptId()).isEqualTo("REC-001");
    }
}
