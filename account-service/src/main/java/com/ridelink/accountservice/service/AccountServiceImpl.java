package com.ridelink.accountservice.service;

import com.ridelink.accountservice.dto.*;
import com.ridelink.accountservice.exception.AccountNotActiveException;
import com.ridelink.accountservice.exception.AccountNotFoundException;
import com.ridelink.accountservice.exception.DuplicateEmailException;
import com.ridelink.accountservice.exception.InvalidCredentialsException;
import com.ridelink.accountservice.exception.InvalidRoleException;
import com.ridelink.accountservice.model.Account;
import com.ridelink.accountservice.model.AccountStatus;
import com.ridelink.accountservice.model.Role;
import com.ridelink.accountservice.repository.AccountRepository;
import com.ridelink.accountservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AccountResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new InvalidRoleException("ADMIN accounts cannot be self-registered; choose PASSENGER or DRIVER");
        }

        String email = normalizeEmail(request.getEmail());
        if (accountRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("An account with email " + email + " already exists");
        }

        Account account = new Account();
        account.setName(request.getName().trim());
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setPhoneNumber(request.getPhoneNumber());
        account.setRole(request.getRole());
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        return toResponse(accountRepository.save(account));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // Same error for "unknown email" and "wrong password" so emails cannot be probed.
        Account account = accountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("This account is " + account.getStatus() + " and cannot log in");
        }

        return LoginResponse.builder()
                .token(jwtService.generateToken(account))
                .tokenType("Bearer")
                .expiresInMs(jwtService.getExpirationMs())
                .id(account.getId())
                .name(account.getName())
                .email(account.getEmail())
                .role(account.getRole().name())
                .build();
    }

    @Override
    public AccountResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public AccountResponse updateProfile(String id, UpdateProfileRequest request) {
        Account account = findOrThrow(id);
        if (request.getName() != null) {
            account.setName(request.getName().trim());
        }
        if (request.getPhoneNumber() != null) {
            account.setPhoneNumber(request.getPhoneNumber());
        }
        account.setUpdatedAt(LocalDateTime.now());
        return toResponse(accountRepository.save(account));
    }

    @Override
    public void changePassword(String id, ChangePasswordRequest request) {
        Account account = findOrThrow(id);
        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    @Override
    public List<AccountResponse> listAccounts(Role role) {
        List<Account> accounts = (role != null) ? accountRepository.findByRole(role) : accountRepository.findAll();
        return accounts.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public AccountResponse updateRole(String id, Role role) {
        Account account = findOrThrow(id);
        account.setRole(role);
        account.setUpdatedAt(LocalDateTime.now());
        return toResponse(accountRepository.save(account));
    }

    @Override
    public AccountResponse updateStatus(String id, AccountStatus status) {
        Account account = findOrThrow(id);
        account.setStatus(status);
        account.setUpdatedAt(LocalDateTime.now());
        return toResponse(accountRepository.save(account));
    }

    @Override
    public void delete(String id) {
        accountRepository.delete(findOrThrow(id));
    }

    // ------------------------------------------------------------------ helpers

    private Account findOrThrow(String id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .email(account.getEmail())
                .phoneNumber(account.getPhoneNumber())
                .role(account.getRole())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
