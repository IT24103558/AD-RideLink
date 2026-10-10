package com.ridelink.fareservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fareservice.dto.PaymentRequest;
import com.ridelink.fareservice.dto.PaymentResponse;
import com.ridelink.fareservice.dto.ReceiptResponse;
import com.ridelink.fareservice.exception.GlobalExceptionHandler;
import com.ridelink.fareservice.exception.PaymentFailedException;
import com.ridelink.fareservice.model.PaymentMethod;
import com.ridelink.fareservice.model.PaymentStatus;
import com.ridelink.fareservice.security.JwtAuthFilter;
import com.ridelink.fareservice.security.JwtUtil;
import com.ridelink.fareservice.service.PaymentService;
import com.ridelink.fareservice.service.ReceiptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PaymentController.class, ReceiptController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController and ReceiptController Web Layer Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private ReceiptService receiptService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("POST /api/payments should return 201 Created on successful payment")
    void shouldReturnCreatedOnSuccessfulPayment() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId("PAY-001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .status(PaymentStatus.SUCCESS)
                .transactionReference("TXN-SIM-001")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        PaymentRequest request = PaymentRequest.builder()
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .build();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value("PAY-001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(750.00));
    }

    @Test
    @DisplayName("POST /api/payments with simulateFailure=true should return 402 Payment Required")
    void shouldReturn402OnSimulatedFailure() throws Exception {
        PaymentResponse failedResponse = PaymentResponse.builder()
                .paymentId("PAY-FAIL")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .status(PaymentStatus.FAILED)
                .failureReason("INSUFFICIENT_FUNDS")
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class)))
                .thenThrow(new PaymentFailedException("Simulated payment transaction failed: INSUFFICIENT_FUNDS", failedResponse));

        PaymentRequest request = PaymentRequest.builder()
                .rideId("ride-001")
                .amount(new BigDecimal("750.00"))
                .simulateFailure(true)
                .failureReason("INSUFFICIENT_FUNDS")
                .build();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.errorCode").value("PAYMENT_FAILED"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} should return 200 OK when payment exists")
    void shouldGetPaymentById() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .paymentId("PAY-001")
                .rideId("ride-001")
                .amount(new BigDecimal("750.00"))
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentService.getPaymentById("PAY-001")).thenReturn(response);

        mockMvc.perform(get("/api/payments/PAY-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value("PAY-001"));
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId}/receipt should return 200 OK with official receipt")
    void shouldGetReceiptByPaymentId() throws Exception {
        ReceiptResponse response = ReceiptResponse.builder()
                .receiptId("REC-001")
                .paymentId("PAY-001")
                .rideId("ride-001")
                .passengerId("pass-001")
                .amount(new BigDecimal("750.00"))
                .currency("LKR")
                .paymentMethod(PaymentMethod.CARD_SIMULATED)
                .paymentStatus(PaymentStatus.SUCCESS)
                .issuedAt(LocalDateTime.now())
                .build();

        when(receiptService.getOrCreateReceiptByPaymentId("PAY-001")).thenReturn(response);

        mockMvc.perform(get("/api/payments/PAY-001/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("REC-001"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/receipts/ride/{rideId} should return 200 OK with receipt for completed ride")
    void shouldGetReceiptByRideId() throws Exception {
        ReceiptResponse response = ReceiptResponse.builder()
                .receiptId("REC-001")
                .paymentId("PAY-001")
                .rideId("ride-001")
                .amount(new BigDecimal("750.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        when(receiptService.getReceiptByRideId("ride-001")).thenReturn(response);

        mockMvc.perform(get("/api/receipts/ride/ride-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptId").value("REC-001"));
    }
}
