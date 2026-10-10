package com.ridelink.rideservice.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Same error shape as the Account and Driver services: {errorCode, message, timestamp, details}. */
public class ErrorResponse {

    private String errorCode;
    private String message;
    private LocalDateTime timestamp;
    private List<String> details;

    public ErrorResponse() {
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

    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public List<String> getDetails() { return details; }
}
