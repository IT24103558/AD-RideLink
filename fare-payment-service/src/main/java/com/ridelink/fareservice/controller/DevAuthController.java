package com.ridelink.fareservice.controller;

import com.ridelink.fareservice.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Convenience controller for generating signed JWTs during local testing,
 * Postman evaluation, and viva demonstrations without needing the Account Service running.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Development Authentication Helper", description = "Generates signed mock JWT tokens for local testing")
public class DevAuthController {

    private final JwtUtil jwtUtil;

    @GetMapping("/dev-token")
    @Operation(summary = "Generate a signed mock JWT token",
            description = "Issues an HS256 JWT signed with the shared JWT_SECRET for testing Swagger UI or Postman directly.")
    public ResponseEntity<Map<String, Object>> generateDevToken(
            @RequestParam(defaultValue = "test-user-01") String userId,
            @RequestParam(defaultValue = "PASSENGER") String role) {

        // 24 hour token for testing
        String token = jwtUtil.generateToken(userId, role, 86_400_000L);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer",
                "userId", userId,
                "role", role.toUpperCase(),
                "expiresInSeconds", 86400,
                "instructions", "Copy this token and use 'Bearer ' + token in Authorization header, or paste into Swagger 'Authorize' dialog"
        ));
    }
}
