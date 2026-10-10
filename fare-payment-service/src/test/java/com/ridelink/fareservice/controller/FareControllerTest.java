package com.ridelink.fareservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.dto.FareResponse;
import com.ridelink.fareservice.dto.FinalFareRequest;
import com.ridelink.fareservice.exception.GlobalExceptionHandler;
import com.ridelink.fareservice.model.FareStatus;
import com.ridelink.fareservice.security.JwtAuthFilter;
import com.ridelink.fareservice.security.JwtUtil;
import com.ridelink.fareservice.service.FareService;
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

@WebMvcTest({FareController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FareController Web Layer Tests")
class FareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FareService fareService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Test
    @DisplayName("POST /api/fares/estimate should return 200 OK with estimated fare")
    void shouldReturnEstimatedFare() throws Exception {
        FareEstimateResponse response = FareEstimateResponse.builder()
                .rideId("ride-001")
                .estimatedFare(new BigDecimal("700.00"))
                .amount(new BigDecimal("700.00"))
                .currency("LKR")
                .distanceKm(5.0)
                .formula("Base Fare (200.00 LKR) + (5.00 km x 100.00 LKR/km) = 700.00 LKR")
                .build();

        when(fareService.estimateFare(any(FareEstimateRequest.class))).thenReturn(response);

        FareEstimateRequest request = FareEstimateRequest.builder()
                .rideId("ride-001")
                .pickupLocation("Colombo Fort")
                .destinationLocation("Galle Face")
                .distanceKm(5.0)
                .build();

        mockMvc.perform(post("/api/fares/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride-001"))
                .andExpect(jsonPath("$.estimatedFare").value(700.00))
                .andExpect(jsonPath("$.currency").value("LKR"));
    }

    @Test
    @DisplayName("POST /api/fares/final should return 201 Created with final fare details")
    void shouldReturnFinalFare() throws Exception {
        FareResponse response = FareResponse.builder()
                .id("fare-001")
                .rideId("ride-001")
                .amount(new BigDecimal("1050.00"))
                .currency("LKR")
                .status(FareStatus.CALCULATED)
                .createdAt(LocalDateTime.now())
                .build();

        when(fareService.calculateFinalFare(any(FinalFareRequest.class))).thenReturn(response);

        FinalFareRequest request = FinalFareRequest.builder()
                .rideId("ride-001")
                .distanceKm(8.5)
                .durationMinutes(20.0)
                .build();

        mockMvc.perform(post("/api/fares/final")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("fare-001"))
                .andExpect(jsonPath("$.amount").value(1050.00))
                .andExpect(jsonPath("$.status").value("CALCULATED"));
    }

    @Test
    @DisplayName("GET /api/fares/{fareId} should return 200 OK when fare exists")
    void shouldGetFareById() throws Exception {
        FareResponse response = FareResponse.builder()
                .id("fare-001")
                .rideId("ride-001")
                .amount(new BigDecimal("1050.00"))
                .currency("LKR")
                .build();

        when(fareService.getFareById("fare-001")).thenReturn(response);

        mockMvc.perform(get("/api/fares/fare-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("fare-001"))
                .andExpect(jsonPath("$.amount").value(1050.00));
    }
}
