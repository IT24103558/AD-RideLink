package com.ridelink.driverservice.service;

import com.ridelink.driverservice.dto.*;
import com.ridelink.driverservice.exception.DriverNotFoundException;
import com.ridelink.driverservice.exception.DuplicateLicenseException;
import com.ridelink.driverservice.exception.NoAvailableDriverException;
import com.ridelink.driverservice.model.AvailabilityStatus;
import com.ridelink.driverservice.model.Driver;
import com.ridelink.driverservice.model.Vehicle;
import com.ridelink.driverservice.repository.DriverRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverServiceImpl driverService;

    private DriverRegisterDTO registerDTO;
    private Driver savedDriver;

    @BeforeEach
    void setUp() {
        VehicleDTO vehicleDTO = new VehicleDTO();
        vehicleDTO.setPlateNumber("CAR-1234");
        vehicleDTO.setVehicleType("CAR");
        vehicleDTO.setModel("Toyota Prius");
        vehicleDTO.setCapacity(4);

        registerDTO = new DriverRegisterDTO();
        registerDTO.setUserId("user-001");
        registerDTO.setFullName("Kasun Perera");
        registerDTO.setLicenseNumber("B1234567");
        registerDTO.setPhoneNumber("+94771234567");
        registerDTO.setServiceArea("Colombo");
        registerDTO.setVehicle(vehicleDTO);

        savedDriver = new Driver();
        savedDriver.setId("driver-001");
        savedDriver.setUserId("user-001");
        savedDriver.setFullName("Kasun Perera");
        savedDriver.setLicenseNumber("B1234567");
        savedDriver.setPhoneNumber("+94771234567");
        savedDriver.setServiceArea("Colombo");
        savedDriver.setVehicle(new Vehicle("CAR-1234", "CAR", "Toyota Prius", 4));
        savedDriver.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        savedDriver.setCreatedAt(LocalDateTime.now());
        savedDriver.setUpdatedAt(LocalDateTime.now());
    }

    // ---------- registerDriver ----------

    @Test
    void registerDriver_shouldSucceed_whenLicenseAndUserIdAreUnique() {
        when(driverRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(driverRepository.existsByUserId(anyString())).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenReturn(savedDriver);

        DriverResponseDTO result = driverService.registerDriver(registerDTO);

        assertThat(result.getId()).isEqualTo("driver-001");
        assertThat(result.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        verify(driverRepository, times(1)).save(any(Driver.class));
    }

    @Test
    void registerDriver_shouldThrowException_whenLicenseNumberAlreadyExists() {
        when(driverRepository.existsByLicenseNumber(anyString())).thenReturn(true);

        assertThrows(DuplicateLicenseException.class, () -> driverService.registerDriver(registerDTO));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void registerDriver_shouldThrowException_whenUserIdAlreadyHasDriverProfile() {
        when(driverRepository.existsByLicenseNumber(anyString())).thenReturn(false);
        when(driverRepository.existsByUserId(anyString())).thenReturn(true);

        assertThrows(DuplicateLicenseException.class, () -> driverService.registerDriver(registerDTO));
        verify(driverRepository, never()).save(any(Driver.class));
    }

    // ---------- getDriverById ----------

    @Test
    void getDriverById_shouldReturnDriver_whenFound() {
        when(driverRepository.findById("driver-001")).thenReturn(Optional.of(savedDriver));

        DriverResponseDTO result = driverService.getDriverById("driver-001");

        assertThat(result.getFullName()).isEqualTo("Kasun Perera");
    }

    @Test
    void getDriverById_shouldThrowException_whenNotFound() {
        when(driverRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThrows(DriverNotFoundException.class, () -> driverService.getDriverById("missing-id"));
    }

    // ---------- updateAvailability ----------

    @Test
    void updateAvailability_shouldUpdateStatus() {
        when(driverRepository.findById("driver-001")).thenReturn(Optional.of(savedDriver));
        when(driverRepository.save(any(Driver.class))).thenReturn(savedDriver);

        AvailabilityUpdateDTO dto = new AvailabilityUpdateDTO();
        dto.setStatus(AvailabilityStatus.AVAILABLE);

        driverService.updateAvailability("driver-001", dto);

        verify(driverRepository).save(argThat(d -> d.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE));
    }

    // ---------- getAvailableDrivers (the interservice contract endpoint) ----------

    @Test
    void getAvailableDrivers_shouldReturnList_whenDriversExistInArea() {
        savedDriver.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        when(driverRepository.findByAvailabilityStatusAndServiceArea(AvailabilityStatus.AVAILABLE, "Colombo"))
                .thenReturn(List.of(savedDriver));

        List<DriverResponseDTO> result = driverService.getAvailableDrivers("Colombo");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getServiceArea()).isEqualTo("Colombo");
    }

    @Test
    void getAvailableDrivers_shouldThrowException_whenNoDriversFound() {
        when(driverRepository.findByAvailabilityStatusAndServiceArea(AvailabilityStatus.AVAILABLE, "Jaffna"))
                .thenReturn(Collections.emptyList());

        assertThrows(NoAvailableDriverException.class, () -> driverService.getAvailableDrivers("Jaffna"));
    }

    @Test
    void getAvailableDrivers_shouldSearchAllAreas_whenServiceAreaNotProvided() {
        when(driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE))
                .thenReturn(Collections.emptyList());

        assertThrows(NoAvailableDriverException.class, () -> driverService.getAvailableDrivers(null));
        verify(driverRepository).findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        verify(driverRepository, never()).findByAvailabilityStatusAndServiceArea(any(), anyString());
    }
}
