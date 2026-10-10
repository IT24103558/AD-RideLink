package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.dto.FareResponse;
import com.ridelink.fareservice.dto.FinalFareRequest;
import com.ridelink.fareservice.exception.ResourceNotFoundException;
import com.ridelink.fareservice.model.Fare;
import com.ridelink.fareservice.model.FareStatus;
import com.ridelink.fareservice.repository.FareRepository;
import com.ridelink.fareservice.service.impl.FareServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FareService Unit Tests")
class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    @Mock
    private FareCalculationService calculationService;

    @InjectMocks
    private FareServiceImpl fareService;

    private FareCalculationService.CalculationResult mockCalcResult;

    @BeforeEach
    void setUp() {
        mockCalcResult = new FareCalculationService.CalculationResult(
                new BigDecimal("200.00"),
                new BigDecimal("500.00"),
                BigDecimal.ZERO,
                BigDecimal.ONE,
                new BigDecimal("700.00"),
                "LKR",
                "Base Fare (200.00 LKR) + (5.00 km x 100.00 LKR/km) = 700.00 LKR"
        );
        lenient().when(calculationService.getRatePerKm()).thenReturn(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should estimate fare successfully with valid distance")
    void shouldEstimateFareWithDistance() {
        when(calculationService.calculateFare(5.0, null, "CAR")).thenReturn(mockCalcResult);

        FareEstimateRequest request = FareEstimateRequest.builder()
                .rideId("ride-001")
                .pickupLocation("Colombo Fort")
                .destinationLocation("Galle Face")
                .distanceKm(5.0)
                .vehicleType("CAR")
                .build();

        FareEstimateResponse response = fareService.estimateFare(request);

        assertThat(response).isNotNull();
        assertThat(response.getRideId()).isEqualTo("ride-001");
        assertThat(response.getEstimatedFare()).isEqualByComparingTo("700.00");
        assertThat(response.getAmount()).isEqualByComparingTo("700.00");
        assertThat(response.getCurrency()).isEqualTo("LKR");
        assertThat(response.getDistanceKm()).isEqualTo(5.0);
    }

    @Test
    @DisplayName("Should estimate fare using GPS coordinates when distance is not explicitly passed")
    void shouldEstimateFareWithCoordinates() {
        when(calculationService.calculateHaversineDistanceKm(6.9344, 79.8525, 6.9271, 79.8436)).thenReturn(1.5);
        when(calculationService.calculateFare(1.5, null, "CAR")).thenReturn(mockCalcResult);

        FareEstimateRequest request = FareEstimateRequest.builder()
                .pickupLatitude(6.9344)
                .pickupLongitude(79.8525)
                .destinationLatitude(6.9271)
                .destinationLongitude(79.8436)
                .vehicleType("CAR")
                .build();

        FareEstimateResponse response = fareService.estimateFare(request);

        assertThat(response).isNotNull();
        assertThat(response.getEstimatedFare()).isEqualByComparingTo("700.00");
        assertThat(response.getDistanceKm()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("Should reject fare estimate with negative distance")
    void shouldRejectNegativeDistance() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(-3.0)
                .build();

        assertThrows(IllegalArgumentException.class, () -> fareService.estimateFare(request));
    }

    @Test
    @DisplayName("Should reject fare estimate without distance or coordinates")
    void shouldRejectMissingDistanceAndCoordinates() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .pickupLocation("A")
                .destinationLocation("B")
                .build();

        assertThrows(IllegalArgumentException.class, () -> fareService.estimateFare(request));
    }

    @Test
    @DisplayName("Should calculate and save final fare for completed ride")
    void shouldCalculateAndSaveFinalFare() {
        when(fareRepository.findByRideIdAndStatus("ride-001", FareStatus.CALCULATED)).thenReturn(Optional.empty());
        when(calculationService.calculateFare(8.0, 15.0, "CAR")).thenReturn(mockCalcResult);

        Fare savedFare = Fare.builder()
                .id("fare-mongoid-01")
                .rideId("ride-001")
                .passengerId("pass-01")
                .driverId("driver-01")
                .vehicleType("CAR")
                .distanceKm(8.0)
                .durationMinutes(15.0)
                .baseFare(new BigDecimal("200.00"))
                .distanceFare(new BigDecimal("500.00"))
                .durationFare(BigDecimal.ZERO)
                .vehicleMultiplier(BigDecimal.ONE)
                .totalFare(new BigDecimal("700.00"))
                .currency("LKR")
                .status(FareStatus.CALCULATED)
                .pricingBreakdown("Formula")
                .createdAt(LocalDateTime.now())
                .build();

        when(fareRepository.save(any(Fare.class))).thenReturn(savedFare);

        FinalFareRequest request = FinalFareRequest.builder()
                .rideId("ride-001")
                .passengerId("pass-01")
                .driverUserId("driver-01")
                .vehicleType("CAR")
                .distanceKm(8.0)
                .durationMinutes(15.0)
                .build();

        FareResponse response = fareService.calculateFinalFare(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("fare-mongoid-01");
        assertThat(response.getRideId()).isEqualTo("ride-001");
        assertThat(response.getAmount()).isEqualByComparingTo("700.00");
        assertThat(response.getStatus()).isEqualTo(FareStatus.CALCULATED);

        verify(fareRepository, times(1)).save(any(Fare.class));
    }

    @Test
    @DisplayName("Should return existing fare when final fare already calculated for this ride (Idempotency)")
    void shouldReturnExistingWhenFinalFareAlreadyCalculated() {
        Fare existingFare = Fare.builder()
                .id("fare-existing")
                .rideId("ride-001")
                .totalFare(new BigDecimal("700.00"))
                .currency("LKR")
                .status(FareStatus.CALCULATED)
                .distanceKm(8.0)
                .build();

        when(fareRepository.findByRideIdAndStatus("ride-001", FareStatus.CALCULATED))
                .thenReturn(Optional.of(existingFare));

        FinalFareRequest request = FinalFareRequest.builder()
                .rideId("ride-001")
                .distanceKm(8.0)
                .build();

        FareResponse response = fareService.calculateFinalFare(request);

        assertThat(response.getId()).isEqualTo("fare-existing");
        verify(fareRepository, never()).save(any(Fare.class));
    }

    @Test
    @DisplayName("Should retrieve fare by ID when it exists")
    void shouldGetFareByIdWhenExists() {
        Fare fare = Fare.builder()
                .id("fare-123")
                .rideId("ride-123")
                .totalFare(new BigDecimal("500.00"))
                .status(FareStatus.CALCULATED)
                .build();

        when(fareRepository.findById("fare-123")).thenReturn(Optional.of(fare));

        FareResponse response = fareService.getFareById("fare-123");
        assertThat(response.getId()).isEqualTo("fare-123");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fare ID does not exist")
    void shouldThrowNotFoundWhenFareIdMissing() {
        when(fareRepository.findById("fare-999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> fareService.getFareById("fare-999"));
    }
}
