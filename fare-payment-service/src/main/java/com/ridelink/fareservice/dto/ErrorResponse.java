package com.ridelink.fareservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response shared across all RideLink microservices.
 */
@Getter
@Setter
@Schema(description = "Standard API error response format")
public class ErrorResponse {

    @Schema(description = "Standard application error code", example = "RESOURCE_NOT_FOUND")
    private String errorCode;

    @Schema(description = "Human-readable description of what went wrong", example = "Fare record not found with id: fare-101")
    private String message;

    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;

    @Schema(description = "Optional list of fine-grained error details or validation errors")
    private List<String> details;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String errorCode, String message, List<String> details) {
        this(errorCode, message);
        this.details = details;
    }
}
