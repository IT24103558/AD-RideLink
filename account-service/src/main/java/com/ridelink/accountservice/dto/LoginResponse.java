package com.ridelink.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String tokenType;
    private long expiresInMs;

    /** Account id = JWT "sub". Use it as the driver profile's userId in Driver & Vehicle Service. */
    private String id;
    private String name;
    private String email;
    private String role;
}
