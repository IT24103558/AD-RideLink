package com.ridelink.accountservice.security;

import com.ridelink.accountservice.model.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and validates the JWTs used by every RideLink service.
 *
 * TOKEN CONTRACT (what Driver & Vehicle / Ride Management rely on):
 *   - signed with HS256 using the UTF-8 bytes of the shared secret (JWT_SECRET)
 *   - "sub"  = the account id (stable identifier, e.g. the driver profile's userId)
 *   - "role" = PASSENGER | DRIVER | ADMIN   (they turn it into ROLE_<role>)
 *   - also carries "email" and "name" for convenience
 */
@Component
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret (environment variable JWT_SECRET) must be at least "
                            + MIN_SECRET_BYTES + " characters long");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(Account account) {
        Date now = new Date();
        return Jwts.builder()
                .subject(account.getId())
                .claim("role", account.getRole().name())
                .claim("email", account.getEmail())
                .claim("name", account.getName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
