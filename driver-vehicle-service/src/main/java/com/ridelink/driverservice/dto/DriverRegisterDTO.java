package com.ridelink.driverservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DriverRegisterDTO {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "License number is required")
    @Pattern(regexp = "^[A-Z0-9]{5,15}$", message = "License number must be 5-15 uppercase letters/digits")
    private String licenseNumber;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Service area is required")
    private String serviceArea;

    @Valid
    @NotNull(message = "Vehicle details are required")
    private VehicleDTO vehicle;
}
