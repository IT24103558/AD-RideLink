package com.ridelink.fareservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utility for parsing and validating JSON Web Tokens issued by the Account Service.
 * Also supports mock token generation for local development and testing.
 */
@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret:dev-only-secret-change-me-please-1234567890}") String secret) {
        // Ensure secret is at least 32 bytes for HS256
        String paddedSecret = (secret.length() < 32)
                ? String.format("%-32s", secret).replace(' ', '0')
                : secret;
        this.key = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Generates a signed token for local development or automated tests.
     */
    public String generateToken(String subject, String role, long expirationMillis) {
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(key)
                .compact();
    }
}
