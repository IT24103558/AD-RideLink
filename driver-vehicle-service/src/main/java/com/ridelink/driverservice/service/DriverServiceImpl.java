package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.*;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.exception.DuplicateLicenseException;
import com.ridelink.driverservice.exception.NoAvailableDriverException;
import com.ridelink.driverservice.model.AvailabilityStatus;
import com.ridelink.driverservice.model.Driver;
import com.ridelink.driverservice.model.Location;
import com.ridelink.driverservice.model.Vehicle;
import com.ridelink.driverservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;

    @Override
    public DriverResponseDTO registerDriver(DriverRegisterDTO dto) {
        if (driverRepository.existsByLicenseNumber(dto.getLicenseNumber())) {
            throw new DuplicateLicenseException(
                    "A driver with license number " + dto.getLicenseNumber() + " is already registered");
        }
        if (driverRepository.existsByUserId(dto.getUserId())) {
            throw new DuplicateLicenseException(
                    "A driver profile already exists for userId " + dto.getUserId());
        }

        Vehicle vehicle = new Vehicle(
                dto.getVehicle().getPlateNumber(),
                dto.getVehicle().getVehicleType(),
                dto.getVehicle().getModel(),
                dto.getVehicle().getCapacity()
        );

        Driver driver = new Driver();
        driver.setUserId(dto.getUserId());
        driver.setFullName(dto.getFullName());
        driver.setLicenseNumber(dto.getLicenseNumber());
        driver.setPhoneNumber(dto.getPhoneNumber());
        driver.setServiceArea(dto.getServiceArea());
        driver.setVehicle(vehicle);
        driver.setAvailabilityStatus(AvailabilityStatus.OFFLINE); // default until driver goes online
        driver.setCreatedAt(LocalDateTime.now());
        driver.setUpdatedAt(LocalDateTime.now());

        Driver saved = driverRepository.save(driver);
        return mapToResponseDTO(saved);
    }

    @Override
    public DriverResponseDTO getDriverById(String id) {
        Driver driver = findDriverOrThrow(id);
        return mapToResponseDTO(driver);
    }

    @Override
    public DriverResponseDTO getDriverByUserId(String userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new DriverNotFoundException("No driver found for userId " + userId));
        return mapToResponseDTO(driver);
    }

    @Override
    public DriverResponseDTO updateVehicle(String id, VehicleDTO dto) {
        Driver driver = findDriverOrThrow(id);
        Vehicle vehicle = new Vehicle(dto.getPlateNumber(), dto.getVehicleType(), dto.getModel(), dto.getCapacity());
        driver.setVehicle(vehicle);
        driver.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(driverRepository.save(driver));
    }

    @Override
    public DriverResponseDTO updateAvailability(String id, AvailabilityUpdateDTO dto) {
        Driver driver = findDriverOrThrow(id);
        driver.setAvailabilityStatus(dto.getStatus());
        driver.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(driverRepository.save(driver));
    }

    @Override
    public DriverResponseDTO updateLocation(String id, LocationUpdateDTO dto) {
        Driver driver = findDriverOrThrow(id);
        driver.setCurrentLocation(new Location(dto.getLatitude(), dto.getLongitude()));
        driver.setUpdatedAt(LocalDateTime.now());
        return mapToResponseDTO(driverRepository.save(driver));
    }

    @Override
    public List<DriverResponseDTO> getAvailableDrivers(String serviceArea) {
        List<Driver> drivers = (serviceArea != null && !serviceArea.isBlank())
                ? driverRepository.findByAvailabilityStatusAndServiceArea(AvailabilityStatus.AVAILABLE, serviceArea)
                : driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);

        if (drivers.isEmpty()) {
            throw new NoAvailableDriverException(
                    "No available drivers found" + (serviceArea != null ? " in service area: " + serviceArea : ""));
        }

        return drivers.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    private Driver findDriverOrThrow(String id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new DriverNotFoundException("Driver not found with id: " + id));
    }

    private DriverResponseDTO mapToResponseDTO(Driver driver) {
        return DriverResponseDTO.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .fullName(driver.getFullName())
                .licenseNumber(driver.getLicenseNumber())
                .phoneNumber(driver.getPhoneNumber())
                .vehicle(driver.getVehicle())
                .availabilityStatus(driver.getAvailabilityStatus())
                .serviceArea(driver.getServiceArea())
                .currentLocation(driver.getCurrentLocation())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
