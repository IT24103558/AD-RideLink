package com.ridelink.accountservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Account aggregate. The generated {@code id} is the STABLE identifier other
 * services refer to (it is the JWT "sub" claim). Other services store only
 * this id - they never read this collection.
 */
@Document(collection = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    private String id;

    private String name;

    /** Stored trimmed + lower-case; unique. */
    @Indexed(unique = true)
    private String email;

    /** BCrypt hash - the plain password is never stored or returned. */
    @ToString.Exclude
    private String passwordHash;

    private String phoneNumber;

    private Role role;

    private AccountStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
