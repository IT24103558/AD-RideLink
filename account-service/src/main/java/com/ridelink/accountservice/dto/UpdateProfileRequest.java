package com.ridelink.accountservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Both fields optional - only the ones supplied are changed. */
@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Invalid phone number format")
    private String phoneNumber;
}
