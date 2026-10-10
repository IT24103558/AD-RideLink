package com.ridelink.fareservice.service;

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
import com.ridelink.fareservice.service.impl.PaymentServiceImpl;
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
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FareRepository fareRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Should successfully process simulated payment with CARD_SIMULATED")
    void shouldProcessSuccessfulPayment() {
        when(paymentRepository.existsByRideIdAndStatus("ride-001", PaymentStatus.SUCCESS)).thenReturn(false);

        Payment savedPayment = Payment.builder()
                .id("PAY-TEST001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .status(PaymentStatus.SUCCESS)
                .transactionReference("TXN-SIM-001")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentRequest request = PaymentRequest.builder()
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .simulateFailure(false)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentId()).isEqualTo("PAY-TEST001");
        assertThat(response.getRideId()).isEqualTo("ride-001");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getAmount()).isEqualByComparingTo("750.00");

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should prevent duplicate payment when ride is already successfully paid (HTTP 409 Conflict)")
    void shouldPreventDuplicatePayment() {
        when(paymentRepository.existsByRideIdAndStatus("ride-001", PaymentStatus.SUCCESS)).thenReturn(true);

        PaymentRequest request = PaymentRequest.builder()
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .build();

        assertThrows(DuplicatePaymentException.class, () -> paymentService.processPayment(request));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should reject invalid zero or negative payment amount with InvalidPaymentException")
    void shouldRejectInvalidAmount() {
        when(paymentRepository.existsByRideIdAndStatus("ride-001", PaymentStatus.SUCCESS)).thenReturn(false);

        PaymentRequest zeroAmountRequest = PaymentRequest.builder()
                .rideId("ride-001")
                .amount(BigDecimal.ZERO)
                .build();

        assertThrows(InvalidPaymentException.class, () -> paymentService.processPayment(zeroAmountRequest));

        PaymentRequest negativeAmountRequest = PaymentRequest.builder()
                .rideId("ride-001")
                .amount(new BigDecimal("-100.00"))
                .build();

        assertThrows(InvalidPaymentException.class, () -> paymentService.processPayment(negativeAmountRequest));
    }

    @Test
    @DisplayName("Should fetch finalized fare amount from FareRepository if amount is omitted in request")
    void shouldFetchAmountFromFareWhenOmitted() {
        when(paymentRepository.existsByRideIdAndStatus("ride-001", PaymentStatus.SUCCESS)).thenReturn(false);

        Fare fare = Fare.builder()
                .rideId("ride-001")
                .totalFare(new BigDecimal("820.00"))
                .currency("LKR")
                .status(FareStatus.CALCULATED)
                .build();

        when(fareRepository.findByRideIdAndStatus("ride-001", FareStatus.CALCULATED)).thenReturn(Optional.of(fare));

        Payment savedPayment = Payment.builder()
                .id("PAY-AUTO")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("820.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CASH)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentRequest request = PaymentRequest.builder()
                .rideId("ride-001")
                .passengerId("pass-001")
                .paymentMethod(PaymentMethod.CASH)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertThat(response.getAmount()).isEqualByComparingTo("820.00");
    }

    @Test
    @DisplayName("Should simulate payment failure, persist FAILED record, and throw PaymentFailedException")
    void shouldHandleSimulatedPaymentFailure() {
        when(paymentRepository.existsByRideIdAndStatus("ride-001", PaymentStatus.SUCCESS)).thenReturn(false);

        Payment savedFailed = Payment.builder()
                .id("PAY-FAIL001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("500.00"))
                .currency("LKR")
                .status(PaymentStatus.FAILED)
                .failureReason("INSUFFICIENT_FUNDS")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedFailed);

        PaymentRequest request = PaymentRequest.builder()
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("500.00"))
                .simulateFailure(true)
                .failureReason("INSUFFICIENT_FUNDS")
                .build();

        PaymentFailedException ex = assertThrows(PaymentFailedException.class, () ->
                paymentService.processPayment(request)
        );

        assertThat(ex.getPaymentResponse().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(ex.getPaymentResponse().getFailureReason()).isEqualTo("INSUFFICIENT_FUNDS");

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should retrieve payment by ID when exists")
    void shouldGetPaymentByIdWhenExists() {
        Payment payment = Payment.builder()
                .id("PAY-100")
                .rideId("ride-100")
                .amount(new BigDecimal("600.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findById("PAY-100")).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById("PAY-100");
        assertThat(response.getPaymentId()).isEqualTo("PAY-100");
        assertThat(response.getRideId()).isEqualTo("ride-100");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment ID does not exist")
    void shouldThrowNotFoundWhenPaymentMissing() {
        when(paymentRepository.findById("PAY-NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById("PAY-NONEXISTENT"));
    }
}
