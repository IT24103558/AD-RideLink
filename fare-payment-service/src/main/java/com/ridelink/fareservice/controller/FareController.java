package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.dto.ErrorResponse;
import com.ridelink.fareservice.dto.FareEstimateRequest;
import com.ridelink.fareservice.dto.FareEstimateResponse;
import com.ridelink.fareservice.dto.FareResponse;
import com.ridelink.fareservice.dto.FinalFareRequest;
import com.ridelink.fareservice.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/fares", "/api/v1/fares"})
@RequiredArgsConstructor
@Tag(name = "Fare Management", description = "Endpoints for calculating ride fare estimates and finalized trip fares")
public class FareController {

    private final FareService fareService;

    @PostMapping("/estimate")
    @Operation(summary = "Estimate fare for a ride",
            description = "Calculates an estimated fare based on distance or pickup/destination coordinates using the formula: Base Fare + (Distance x Rate per km).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare estimate calculated successfully",
                    content = @Content(schema = @Schema(implementation = FareEstimateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or zero/negative distance",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FareEstimateResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        FareEstimateResponse response = fareService.estimateFare(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/final")
    @Operation(summary = "Calculate and store final fare",
            description = "Calculates the final fare for a completed ride and persists the record to MongoDB.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Final fare calculated and persisted successfully",
                    content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed on input fields",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication token missing or invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FareResponse> calculateFinalFare(@Valid @RequestBody FinalFareRequest request) {
        FareResponse response = fareService.calculateFinalFare(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fareId}")
    @Operation(summary = "Get fare by record ID",
            description = "Retrieves a stored fare document from MongoDB using its unique fare ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare record retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "404", description = "Fare record not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FareResponse> getFareById(@PathVariable String fareId) {
        FareResponse response = fareService.getFareById(fareId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get fare by ride ID",
            description = "Retrieves the fare record associated with a specific ride ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fare record retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FareResponse.class))),
            @ApiResponse(responseCode = "404", description = "No fare record found for this ride ID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FareResponse> getFareByRideId(@PathVariable String rideId) {
        FareResponse response = fareService.getFareByRideId(rideId);
        return ResponseEntity.ok(response);
    }
}
