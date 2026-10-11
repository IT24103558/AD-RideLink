package com.ridelink.accountservice.config;

import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.model.AccountStatus;
import com.ridelink.accountservice.model.Role;
import com.ridelink.accountservice.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Creates the first ADMIN from the ADMIN_EMAIL / ADMIN_PASSWORD environment
 * variables (nothing is hard-coded in the repository). Does nothing when the
 * variables are not set or the account already exists.
 */
@Slf4j
@Component
public class AdminBootstrap implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${admin.email:}") String adminEmail,
                          @Value("${admin.password:}") String adminPassword) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }
        if (adminPassword.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters - admin account not created");
            return;
        }
        String email = adminEmail.trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsByEmail(email)) {
            return;
        }

        Account admin = new Account();
        admin.setName("System Admin");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(admin);
        log.info("Bootstrap ADMIN account created for {}", email);
    }
}
