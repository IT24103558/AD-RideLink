package com.ridelink.fareservice.service;

import com.ridelink.fareservice.config.FarePricingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dedicated domain calculation service implementing the documented fare formula:
 * 
 * Total Fare = (Base Fare + (Distance in km x Rate per km) + (Duration in minutes x Rate per minute)) x Vehicle Multiplier
 * 
 * Uses BigDecimal with 2 decimal places (HALF_UP rounding) for accurate currency computations.
 */
@Service
@RequiredArgsConstructor
public class FareCalculationService {

    private final FarePricingProperties pricingProperties;

    /**
     * Structure holding the calculated fare components.
     */
    public record CalculationResult(
            BigDecimal baseFare,
            BigDecimal distanceFare,
            BigDecimal durationFare,
            BigDecimal vehicleMultiplier,
            BigDecimal totalFare,
            String currency,
            String breakdownFormula
    ) {}

    public BigDecimal getBaseFare() {
        return pricingProperties.getBaseFare();
    }

    public BigDecimal getRatePerKm() {
        return pricingProperties.getRatePerKm();
    }

    /**
     * Calculates the fare components using the documented formula.
     *
     * @param distanceKm      Distance in kilometres (> 0)
     * @param durationMinutes Optional duration in minutes (>= 0, default 0 if null)
     * @param vehicleType     Optional vehicle category (CAR, BIKE, VAN)
     * @return CalculationResult with breakdown and total amount
     */
    public CalculationResult calculateFare(double distanceKm, Double durationMinutes, String vehicleType) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance in kilometres must be greater than zero. Received: " + distanceKm);
        }

        if (durationMinutes != null && durationMinutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative. Received: " + durationMinutes);
        }

        double duration = (durationMinutes != null) ? durationMinutes : 0.0;

        BigDecimal baseFare = pricingProperties.getBaseFare().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratePerKm = pricingProperties.getRatePerKm().setScale(2, RoundingMode.HALF_UP);
        BigDecimal ratePerMin = pricingProperties.getRatePerMinute().setScale(2, RoundingMode.HALF_UP);

        BigDecimal distanceBigDecimal = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationBigDecimal = BigDecimal.valueOf(duration).setScale(2, RoundingMode.HALF_UP);

        // Distance fare = distanceKm * ratePerKm
        BigDecimal distanceFare = distanceBigDecimal.multiply(ratePerKm).setScale(2, RoundingMode.HALF_UP);

        // Duration fare = durationMinutes * ratePerMinute
        BigDecimal durationFare = durationBigDecimal.multiply(ratePerMin).setScale(2, RoundingMode.HALF_UP);

        // Subtotal before multiplier = baseFare + distanceFare + durationFare
        BigDecimal subtotal = baseFare.add(distanceFare).add(durationFare);

        // Vehicle multiplier (e.g. BIKE=0.8, CAR=1.0, VAN=1.5)
        BigDecimal multiplier = pricingProperties.getMultiplier(vehicleType);

        // Final total fare
        BigDecimal totalFare = subtotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        String currency = pricingProperties.getCurrency();

        // Documented breakdown formula text
        StringBuilder formula = new StringBuilder();
        formula.append(String.format("Base Fare (%s %s) + (%.2f km x %s %s/km = %s %s)",
                baseFare, currency, distanceKm, ratePerKm, currency, distanceFare, currency));

        if (duration > 0 && ratePerMin.compareTo(BigDecimal.ZERO) > 0) {
            formula.append(String.format(" + (%.1f min x %s %s/min = %s %s)",
                    duration, ratePerMin, currency, durationFare, currency));
        }

        if (multiplier.compareTo(BigDecimal.ONE) != 0) {
            formula.append(String.format(" x Vehicle Multiplier (%.1fx for %s)", multiplier, vehicleType));
        }

        formula.append(String.format(" = %s %s", totalFare, currency));

        return new CalculationResult(baseFare, distanceFare, durationFare, multiplier, totalFare, currency, formula.toString());
    }

    /**
     * Calculates distance in kilometres between two GPS coordinates using the Haversine formula.
     * Useful when clients submit latitude/longitude rather than pre-calculated distance.
     */
    public double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;
        // Round to 2 decimal places
        return Math.round(distance * 100.0) / 100.0;
    }
}
