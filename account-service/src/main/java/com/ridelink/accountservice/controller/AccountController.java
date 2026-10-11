package com.ridelink.accountservice.controller;

import com.ridelink.accountservice.dto.*;
import com.ridelink.accountservice.model.Role;
import com.ridelink.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Service", description = "Registration, login (JWT), profile, role and status management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ------------------------------------------------------------ public

    @Operation(summary = "Register a PASSENGER or DRIVER account")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.register(request));
    }

    @Operation(summary = "Log in and receive a JWT (sub = account id, role claim). "
            + "Send it as 'Authorization: Bearer <token>' to every RideLink service.")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(accountService.login(request));
    }

    // ------------------------------------------------------------ own account / admin

    @Operation(summary = "Profile of the logged-in account")
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> me(Authentication authentication) {
        return ResponseEntity.ok(accountService.getById(authentication.getName()));
    }

    @Operation(summary = "Get an account by id (own account, or ADMIN)")
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getById(@PathVariable String id, Authentication authentication) {
        requireOwnerOrAdmin(id, authentication);
        return ResponseEntity.ok(accountService.getById(id));
    }

    @Operation(summary = "Update name / phone number (own account, or ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateProfile(@PathVariable String id,
                                                         @Valid @RequestBody UpdateProfileRequest request,
                                                         Authentication authentication) {
        requireOwnerOrAdmin(id, authentication);
        return ResponseEntity.ok(accountService.updateProfile(id, request));
    }

    @Operation(summary = "Change your own password")
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable String id,
                                               @Valid @RequestBody ChangePasswordRequest request,
                                               Authentication authentication) {
        if (!id.equals(authentication.getName())) {
            throw new AccessDeniedException("Only the account owner can change the password");
        }
        accountService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ ADMIN only (also enforced in SecurityConfig)

    @Operation(summary = "ADMIN: list accounts, optionally filtered by role")
    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(@RequestParam(required = false) Role role) {
        return ResponseEntity.ok(accountService.listAccounts(role));
    }

    @Operation(summary = "ADMIN: change an account's role")
    @PatchMapping("/{id}/role")
    public ResponseEntity<AccountResponse> updateRole(@PathVariable String id,
                                                      @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateRole(id, request.getRole()));
    }

    @Operation(summary = "ADMIN: set account status (ACTIVE / SUSPENDED / DEACTIVATED)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateStatus(@PathVariable String id,
                                                        @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateStatus(id, request.getStatus()));
    }

    @Operation(summary = "ADMIN: delete an account")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------ helpers

    private void requireOwnerOrAdmin(String id, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && !id.equals(authentication.getName())) {
            throw new AccessDeniedException("You can only access your own account");
        }
    }
}
