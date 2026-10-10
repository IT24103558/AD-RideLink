package com.ridelink.fareservice.service;

import com.ridelink.fareservice.config.FarePricingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("FareCalculationService Unit Tests")
class FareCalculationServiceTest {

    private FarePricingProperties pricingProperties;
    private FareCalculationService calculationService;

    @BeforeEach
    void setUp() {
        pricingProperties = new FarePricingProperties();
        pricingProperties.setBaseFare(new BigDecimal("200.00"));
        pricingProperties.setRatePerKm(new BigDecimal("100.00"));
        pricingProperties.setRatePerMinute(new BigDecimal("10.00"));
        pricingProperties.setCurrency("LKR");
        pricingProperties.setMultipliers(Map.of(
                "BIKE", new BigDecimal("0.8"),
                "CAR", new BigDecimal("1.0"),
                "VAN", new BigDecimal("1.5")
        ));

        calculationService = new FareCalculationService(pricingProperties);
    }

    @Test
    @DisplayName("Should correctly calculate fare using documented formula: Base + (Distance x Rate)")
    void shouldCalculateCorrectFareForStandardTrip() {
        // Base: 200.00, Distance: 5.0 km @ 100.00/km = 500.00 => Total = 700.00 LKR
        FareCalculationService.CalculationResult result =
                calculationService.calculateFare(5.0, 0.0, "CAR");

        assertThat(result.baseFare()).isEqualByComparingTo("200.00");
        assertThat(result.distanceFare()).isEqualByComparingTo("500.00");
        assertThat(result.durationFare()).isEqualByComparingTo("0.00");
        assertThat(result.vehicleMultiplier()).isEqualByComparingTo("1.0");
        assertThat(result.totalFare()).isEqualByComparingTo("700.00");
        assertThat(result.currency()).isEqualTo("LKR");
        assertThat(result.breakdownFormula()).contains("700.00 LKR");
    }

    @Test
    @DisplayName("Should apply vehicle type multiplier correctly (e.g. VAN 1.5x)")
    void shouldApplyVehicleMultiplier() {
        // Base: 200, Distance: 10km * 100 = 1000 => Subtotal: 1200 * 1.5 = 1800.00 LKR
        FareCalculationService.CalculationResult result =
                calculationService.calculateFare(10.0, 0.0, "VAN");

        assertThat(result.totalFare()).isEqualByComparingTo("1800.00");
        assertThat(result.vehicleMultiplier()).isEqualByComparingTo("1.5");
    }

    @Test
    @DisplayName("Should apply bike multiplier correctly (e.g. BIKE 0.8x)")
    void shouldApplyBikeMultiplier() {
        // Base: 200, Distance: 10km * 100 = 1000 => Subtotal: 1200 * 0.8 = 960.00 LKR
        FareCalculationService.CalculationResult result =
                calculationService.calculateFare(10.0, 0.0, "BIKE");

        assertThat(result.totalFare()).isEqualByComparingTo("960.00");
        assertThat(result.vehicleMultiplier()).isEqualByComparingTo("0.8");
    }

    @Test
    @DisplayName("Should incorporate duration fare when duration is provided")
    void shouldIncludeDurationFare() {
        // Base: 200, Distance: 5km * 100 = 500, Duration: 15 min * 10 = 150 => Total: 850.00 LKR
        FareCalculationService.CalculationResult result =
                calculationService.calculateFare(5.0, 15.0, "CAR");

        assertThat(result.distanceFare()).isEqualByComparingTo("500.00");
        assertThat(result.durationFare()).isEqualByComparingTo("150.00");
        assertThat(result.totalFare()).isEqualByComparingTo("850.00");
    }

    @Test
    @DisplayName("Should reject zero distance with IllegalArgumentException")
    void shouldRejectZeroDistance() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                calculationService.calculateFare(0.0, 0.0, "CAR")
        );
        assertThat(ex.getMessage()).contains("must be greater than zero");
    }

    @Test
    @DisplayName("Should reject negative distance with IllegalArgumentException")
    void shouldRejectNegativeDistance() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                calculationService.calculateFare(-5.5, 0.0, "CAR")
        );
        assertThat(ex.getMessage()).contains("must be greater than zero");
    }

    @Test
    @DisplayName("Should reject negative duration with IllegalArgumentException")
    void shouldRejectNegativeDuration() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                calculationService.calculateFare(5.0, -10.0, "CAR")
        );
        assertThat(ex.getMessage()).contains("Duration cannot be negative");
    }

    @Test
    @DisplayName("Should calculate Haversine distance between two coordinates accurately")
    void shouldCalculateHaversineDistance() {
        // Colombo Fort (6.9344, 79.8525) to Galle Face Green (6.9271, 79.8436) ~ 1.25 km
        double distance = calculationService.calculateHaversineDistanceKm(6.9344, 79.8525, 6.9271, 79.8436);
        assertThat(distance).isGreaterThan(1.0).isLessThan(2.0);
    }
}
