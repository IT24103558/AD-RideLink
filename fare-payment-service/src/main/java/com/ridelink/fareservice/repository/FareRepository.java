package com.ridelink.fareservice.repository;

import com.ridelink.fareservice.model.Fare;
import com.ridelink.fareservice.model.FareStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FareRepository extends MongoRepository<Fare, String> {

    Optional<Fare> findByRideId(String rideId);

    Optional<Fare> findByRideIdAndStatus(String rideId, FareStatus status);

    List<Fare> findAllByRideId(String rideId);

    boolean existsByRideIdAndStatus(String rideId, FareStatus status);
}
