package com.ridelink.rideservice.repository;

import com.ridelink.rideservice.model.Ride;
import com.ridelink.rideservice.model.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface RideRepository extends MongoRepository<Ride, String> {

    boolean existsByPassengerIdAndStatusIn(String passengerId, Collection<RideStatus> statuses);

    List<Ride> findByStatusIn(Collection<RideStatus> statuses);

    List<Ride> findByPassengerIdOrderByRequestedAtDesc(String passengerId);

    List<Ride> findByDriverUserIdOrderByRequestedAtDesc(String driverUserId);
}
