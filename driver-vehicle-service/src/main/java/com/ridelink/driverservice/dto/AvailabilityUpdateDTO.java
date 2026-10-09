package com.ridelink.driverservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.ridelink.driverservice.model.AvailabilityStatus;

@Data
public class AvailabilityUpdateDTO {

    @NotNull(message = "status is required")
    private AvailabilityStatus status;
}
