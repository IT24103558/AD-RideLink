package com.ridelink.fareservice.repository;

import com.ridelink.fareservice.model.Payment;
import com.ridelink.fareservice.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByRideId(String rideId);

    Optional<Payment> findByRideIdAndStatus(String rideId, PaymentStatus status);

    List<Payment> findAllByRideId(String rideId);

    List<Payment> findAllByPassengerId(String passengerId);

    boolean existsByRideIdAndStatus(String rideId, PaymentStatus status);
}
