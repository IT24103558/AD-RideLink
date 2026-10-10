package com.ridelink.fareservice.service;

import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.dto.FareResponse;
import com.ridelink.fareservice.dto.FinalFareRequest;

public interface FareService {

    /**
     * Estimates the fare for a ride based on distance or locations.
     */
    FareEstimateResponse estimateFare(FareEstimateRequest request);

    /**
     * Calculates and saves the final fare for a completed ride into MongoDB.
     */
    FareResponse calculateFinalFare(FinalFareRequest request);

    /**
     * Retrieves a fare record by its unique MongoDB identifier.
     */
    FareResponse getFareById(String fareId);

    /**
     * Retrieves the fare record for a given ride ID.
     */
    FareResponse getFareByRideId(String rideId);
}
