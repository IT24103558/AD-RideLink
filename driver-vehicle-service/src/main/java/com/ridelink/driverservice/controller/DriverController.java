package com.ridelink.driverservice.controller;

import com.ridelink.driverservice.dto.*;
import com.ridelink.driverservice.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Driver & Vehicle Service", description = "Manage driver profiles, vehicles, availability and location")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverService driverService;

    @Operation(summary = "Register a new driver profile with vehicle details")
    @PostMapping
    public ResponseEntity<DriverResponseDTO> registerDriver(@Valid @RequestBody DriverRegisterDTO dto) {
        DriverResponseDTO response = driverService.registerDriver(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get a driver profile by driver id")
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDTO> getDriver(@PathVariable String id) {
        return ResponseEntity.ok(driverService.getDriverById(id));
    }

    @Operation(summary = "Get a driver profile by the Account Service userId")
    @GetMapping("/user/{userId}")
    public ResponseEntity<DriverResponseDTO> getDriverByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(driverService.getDriverByUserId(userId));
    }

    @Operation(summary = "Update vehicle details for a driver")
    @PutMapping("/{id}/vehicle")
    public ResponseEntity<DriverResponseDTO> updateVehicle(@PathVariable String id,
                                                             @Valid @RequestBody VehicleDTO dto) {
        return ResponseEntity.ok(driverService.updateVehicle(id, dto));
    }

    @Operation(summary = "Update a driver's availability status (AVAILABLE / BUSY / OFFLINE)")
    @PatchMapping("/{id}/availability")
    public ResponseEntity<DriverResponseDTO> updateAvailability(@PathVariable String id,
                                                                  @Valid @RequestBody AvailabilityUpdateDTO dto) {
        return ResponseEntity.ok(driverService.updateAvailability(id, dto));
    }

    @Operation(summary = "Update a driver's simulated current location")
    @PatchMapping("/{id}/location")
    public ResponseEntity<DriverResponseDTO> updateLocation(@PathVariable String id,
                                                               @Valid @RequestBody LocationUpdateDTO dto) {
        return ResponseEntity.ok(driverService.updateLocation(id, dto));
    }

    @Operation(summary = "Get eligible available drivers, optionally filtered by service area. "
            + "Called by the Ride Management Service to assign a driver to a ride request.")
    @GetMapping("/available")
    public ResponseEntity<List<DriverResponseDTO>> getAvailableDrivers(
            @RequestParam(required = false) String serviceArea) {
        return ResponseEntity.ok(driverService.getAvailableDrivers(serviceArea));
    }
}
