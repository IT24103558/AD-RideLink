package com.ridelink.fareservice.repository;

import com.ridelink.fareservice.model.Receipt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends MongoRepository<Receipt, String> {

    Optional<Receipt> findByPaymentId(String paymentId);

    Optional<Receipt> findByRideId(String rideId);

    boolean existsByPaymentId(String paymentId);
}
