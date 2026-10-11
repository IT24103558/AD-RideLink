package com.ridelink.accountservice.repository;

import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Account> findByRole(Role role);
}
