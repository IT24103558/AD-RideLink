package com.ridelink.fareservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configurable pricing properties for fare calculations.
 * Avoids hard-coding pricing throughout the codebase.
 */
@Configuration
@ConfigurationProperties(prefix = "fare.pricing")
@Getter
@Setter
public class FarePricingProperties {

    /** Base fare charged at the start of any ride (default 200.00 LKR). */
    private BigDecimal baseFare = new BigDecimal("200.00");

    /** Rate charged per kilometre travelled (default 100.00 LKR/km). */
    private BigDecimal ratePerKm = new BigDecimal("100.00");

    /** Optional rate charged per minute of travel (default 10.00 LKR/min). */
    private BigDecimal ratePerMinute = new BigDecimal("10.00");

    /** Standard currency code (default LKR). */
    private String currency = "LKR";

    /** Vehicle type multipliers (e.g. BIKE=0.8, CAR=1.0, VAN=1.5). */
    private Map<String, BigDecimal> multipliers = new HashMap<>();

    public BigDecimal getMultiplier(String vehicleType) {
        if (vehicleType == null || vehicleType.isBlank()) {
            return BigDecimal.ONE;
        }
        return multipliers.getOrDefault(vehicleType.toUpperCase(), BigDecimal.ONE);
    }
}
