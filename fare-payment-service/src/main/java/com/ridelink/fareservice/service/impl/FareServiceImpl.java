package com.ridelink.fareservice.service.impl;

import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.dto.FareResponse;
import com.ridelink.fareservice.dto.FinalFareRequest;
import com.ridelink.fareservice.exception.ResourceNotFoundException;
import com.ridelink.fareservice.model.Fare;
import com.ridelink.fareservice.model.FareStatus;
import com.ridelink.fareservice.repository.FareRepository;
import com.ridelink.fareservice.service.FareCalculationService;
import com.ridelink.fareservice.service.FareService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FareServiceImpl implements FareService {

    private static final Logger log = LoggerFactory.getLogger(FareServiceImpl.class);

    private final FareRepository fareRepository;
    private final FareCalculationService calculationService;

    @Override
    public FareEstimateResponse estimateFare(FareEstimateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Fare estimate request body is required");
        }

        double distanceKm;

        if (request.getDistanceKm() != null) {
            if (request.getDistanceKm() <= 0) {
                throw new IllegalArgumentException("Estimated distance must be greater than zero");
            }
            distanceKm = request.getDistanceKm();
        } else if (hasValidCoordinates(request)) {
            // Compute distance from GPS coordinates using Haversine formula
            distanceKm = calculationService.calculateHaversineDistanceKm(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    request.getDestinationLatitude(), request.getDestinationLongitude());
            if (distanceKm <= 0) {
                // Minimum fallback for very close coordinates in the same area
                distanceKm = 0.5;
            }
        } else {
            throw new IllegalArgumentException(
                    "Either distanceKm (> 0) or valid pickup and destination coordinates must be provided");
        }

        String rideId = (request.getRideId() != null && !request.getRideId().isBlank())
                ? request.getRideId()
                : "ESTIMATE-" + UUID.randomUUID().toString().substring(0, 8);

        String vehicleType = (request.getVehicleType() != null && !request.getVehicleType().isBlank())
                ? request.getVehicleType()
                : "CAR";

        FareCalculationService.CalculationResult calc = calculationService.calculateFare(
                distanceKm, null, vehicleType);

        log.info("Estimated fare calculated for ride [{}]: {} {}", rideId, calc.totalFare(), calc.currency());

        return FareEstimateResponse.builder()
                .rideId(rideId)
                .estimatedFare(calc.totalFare())
                .amount(calc.totalFare())
                .currency(calc.currency())
                .distanceKm(distanceKm)
                .vehicleType(vehicleType)
                .baseFare(calc.baseFare())
                .ratePerKm(calculationService.getRatePerKm())
                .distanceFare(calc.distanceFare())
                .vehicleMultiplier(calc.vehicleMultiplier())
                .formula(calc.breakdownFormula())
                .calculatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public FareResponse calculateFinalFare(FinalFareRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Final fare request body is required");
        }
        if (request.getRideId() == null || request.getRideId().isBlank()) {
            throw new IllegalArgumentException("Ride ID is required");
        }
        if (request.getDistanceKm() == null || request.getDistanceKm() <= 0) {
            throw new IllegalArgumentException("Final distance must be greater than zero");
        }

        // Idempotency: if final fare was already calculated for this ride, return it
        Optional<Fare> existingFare = fareRepository.findByRideIdAndStatus(request.getRideId(), FareStatus.CALCULATED);
        if (existingFare.isPresent()) {
            log.info("Final fare for ride [{}] already calculated. Returning existing record.", request.getRideId());
            return mapToResponse(existingFare.get());
        }

        String vehicleType = (request.getVehicleType() != null && !request.getVehicleType().isBlank())
                ? request.getVehicleType()
                : "CAR";

        double duration = (request.getDurationMinutes() != null && request.getDurationMinutes() > 0)
                ? request.getDurationMinutes()
                : 0.0;

        FareCalculationService.CalculationResult calc = calculationService.calculateFare(
                request.getDistanceKm(), duration, vehicleType);

        Fare fare = Fare.builder()
                .rideId(request.getRideId())
                .passengerId(request.getPassengerId())
                .driverId(request.getDriverUserId())
                .vehicleType(vehicleType)
                .pickupLocation(request.getPickupLocation())
                .destinationLocation(request.getDestinationLocation())
                .distanceKm(request.getDistanceKm())
                .durationMinutes(duration)
                .baseFare(calc.baseFare())
                .distanceFare(calc.distanceFare())
                .durationFare(calc.durationFare())
                .vehicleMultiplier(calc.vehicleMultiplier())
                .totalFare(calc.totalFare())
                .currency(calc.currency())
                .status(FareStatus.CALCULATED)
                .pricingBreakdown(calc.breakdownFormula())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Fare savedFare = fareRepository.save(fare);
        log.info("Saved final fare [{}] for ride [{}]: {} {}", savedFare.getId(), savedFare.getRideId(),
                savedFare.getTotalFare(), savedFare.getCurrency());

        return mapToResponse(savedFare);
    }

    @Override
    public FareResponse getFareById(String fareId) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare", fareId));
        return mapToResponse(fare);
    }

    @Override
    public FareResponse getFareByRideId(String rideId) {
        Fare fare = fareRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Fare for ride ID", rideId));
        return mapToResponse(fare);
    }

    private boolean hasValidCoordinates(FareEstimateRequest request) {
        return request.getPickupLatitude() != null && request.getPickupLongitude() != null
                && request.getDestinationLatitude() != null && request.getDestinationLongitude() != null;
    }

    private FareResponse mapToResponse(Fare fare) {
        return FareResponse.builder()
                .id(fare.getId())
                .rideId(fare.getRideId())
                .passengerId(fare.getPassengerId())
                .driverId(fare.getDriverId())
                .vehicleType(fare.getVehicleType())
                .amount(fare.getTotalFare())
                .currency(fare.getCurrency())
                .distanceKm(fare.getDistanceKm())
                .durationMinutes(fare.getDurationMinutes())
                .baseFare(fare.getBaseFare())
                .distanceFare(fare.getDistanceFare())
                .durationFare(fare.getDurationFare())
                .vehicleMultiplier(fare.getVehicleMultiplier())
                .status(fare.getStatus())
                .pricingBreakdown(fare.getPricingBreakdown())
                .createdAt(fare.getCreatedAt())
                .build();
    }
}
