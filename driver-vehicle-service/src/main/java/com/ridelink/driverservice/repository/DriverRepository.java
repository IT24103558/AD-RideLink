package com.ridelink.driverservice.repository;

import com.ridelink.driverservice.model.AvailabilityStatus;
import com.ridelink.driverservice.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends MongoRepository<Driver, String> {

    Optional<Driver> findByUserId(String userId);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByUserId(String userId);

    List<Driver> findByAvailabilityStatus(AvailabilityStatus status);

    List<Driver> findByAvailabilityStatusAndServiceArea(AvailabilityStatus status, String serviceArea);
}
