package com.ridelink.accountservice.security;

import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    // Same default the Driver & Vehicle Service uses, so the two work together out of the box.
    private static final String SECRET = "dev-only-secret-change-me-please-1234567890";

    private JwtService jwtService;
    private Account driver;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
        driver = new Account();
        driver.setId("acc-123");
        driver.setName("Kasun Perera");
        driver.setEmail("kasun@example.com");
        driver.setRole(Role.DRIVER);
    }

    @Test
    void generateToken_shouldCarrySubjectRoleAndEmail() {
        String token = jwtService.generateToken(driver);

        Claims claims = jwtService.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("acc-123");
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
        assertThat(claims.get("email", String.class)).isEqualTo("kasun@example.com");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    /**
     * CONTRACT TEST with Driver & Vehicle Service: its JwtUtil builds the key from the UTF-8
     * bytes of the shared secret and reads "sub" (userId) and the "role" claim. Doing exactly
     * that here proves a token issued by this service is accepted by the driver service.
     */
    @Test
    void token_shouldBeAcceptedByDriverServiceStyleVerification() {
        SecretKey driverServiceKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser().verifyWith(driverServiceKey).build()
                .parseSignedClaims(jwtService.generateToken(driver)).getPayload();

        assertThat(claims.getSubject()).isEqualTo("acc-123");
        assertThat(claims.get("role", String.class)).isEqualTo("DRIVER");
    }

    @Test
    void isValid_shouldBeTrueForFreshToken() {
        assertThat(jwtService.isValid(jwtService.generateToken(driver))).isTrue();
    }

    @Test
    void isValid_shouldBeFalseForTokenSignedWithDifferentSecret() {
        JwtService other = new JwtService("a-completely-different-secret-key-0987654321", 60_000);

        assertThat(jwtService.isValid(other.generateToken(driver))).isFalse();
    }

    @Test
    void isValid_shouldBeFalseForExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000);

        assertThat(jwtService.isValid(shortLived.generateToken(driver))).isFalse();
    }

    @Test
    void isValid_shouldBeFalseForGarbageToken() {
        assertThat(jwtService.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void constructor_shouldRejectTooShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short-secret", 60_000));
    }
}
