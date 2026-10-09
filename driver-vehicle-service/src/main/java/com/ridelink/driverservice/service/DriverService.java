package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.*;

import java.util.List;

public interface DriverService {

    DriverResponseDTO registerDriver(DriverRegisterDTO dto);

    DriverResponseDTO getDriverById(String id);

    DriverResponseDTO getDriverByUserId(String userId);

    DriverResponseDTO updateVehicle(String id, VehicleDTO dto);

    DriverResponseDTO updateAvailability(String id, AvailabilityUpdateDTO dto);

    DriverResponseDTO updateLocation(String id, LocationUpdateDTO dto);

    List<DriverResponseDTO> getAvailableDrivers(String serviceArea);
}
