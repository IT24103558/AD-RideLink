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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account stored;

    @BeforeEach
    void setUp() {
        stored = new Account();
        stored.setId("acc-1");
        stored.setName("Nimal Silva");
        stored.setEmail("nimal@example.com");
        stored.setPasswordHash("HASH");
        stored.setRole(Role.PASSENGER);
        stored.setStatus(AccountStatus.ACTIVE);
    }

    private RegisterRequest registerRequest(Role role) {
        RegisterRequest r = new RegisterRequest();
        r.setName("Nimal Silva");
        r.setEmail("  Nimal@Example.com ");
        r.setPassword("Passw0rd!");
        r.setRole(role);
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail("Nimal@Example.com");
        r.setPassword("Passw0rd!");
        return r;
    }

    // ---------------- register ----------------

    @Test
    void register_shouldHashPassword_normalizeEmail_andActivateAccount() {
        when(accountRepository.existsByEmail("nimal@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("HASH");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId("acc-1");
            return a;
        });

        AccountResponse response = accountService.register(registerRequest(Role.DRIVER));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(captor.getValue().getEmail()).isEqualTo("nimal@example.com");
        assertThat(response.getId()).isEqualTo("acc-1");
        assertThat(response.getRole()).isEqualTo(Role.DRIVER);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void register_shouldRejectAdminRole() {
        assertThrows(InvalidRoleException.class, () -> accountService.register(registerRequest(Role.ADMIN)));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        when(accountRepository.existsByEmail("nimal@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> accountService.register(registerRequest(Role.PASSENGER)));
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ---------------- login ----------------

    @Test
    void login_shouldReturnTokenAndAccountDetails_whenCredentialsAreValid() {
        when(accountRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);
        when(jwtService.generateToken(stored)).thenReturn("jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        LoginResponse response = accountService.login(loginRequest());

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getId()).isEqualTo("acc-1");
        assertThat(response.getRole()).isEqualTo("PASSENGER");
        assertThat(response.getExpiresInMs()).isEqualTo(3_600_000L);
    }

    @Test
    void login_shouldFail_whenPasswordIsWrong() {
        when(accountRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(loginRequest()));
        verify(jwtService, never()).generateToken(any(Account.class));
    }

    @Test
    void login_shouldFailWithSameError_whenEmailIsUnknown() {
        when(accountRepository.findByEmail("nimal@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(loginRequest()));
        verify(jwtService, never()).generateToken(any(Account.class));
    }

    @Test
    void login_shouldBeBlocked_whenAccountIsSuspended() {
        stored.setStatus(AccountStatus.SUSPENDED);
        when(accountRepository.findByEmail("nimal@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);

        assertThrows(AccountNotActiveException.class, () -> accountService.login(loginRequest()));
        verify(jwtService, never()).generateToken(any(Account.class));
    }

    // ---------------- read / update ----------------

    @Test
    void getById_shouldReturnAccountWithoutPassword() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));

        AccountResponse response = accountService.getById("acc-1");

        assertThat(response.getEmail()).isEqualTo("nimal@example.com");
        assertThat(response.toString()).doesNotContain("HASH");
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.getById("missing"));
    }

    @Test
    void updateProfile_shouldChangeOnlySuppliedFields() {
        stored.setPhoneNumber("0771234567");
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName("  New Name ");

        AccountResponse response = accountService.updateProfile("acc-1", request);

        assertThat(response.getName()).isEqualTo("New Name");
        assertThat(response.getPhoneNumber()).isEqualTo("0771234567");
    }

    // ---------------- password ----------------

    @Test
    void changePassword_shouldStoreNewHash_whenCurrentPasswordIsCorrect() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("Passw0rd!", "HASH")).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd")).thenReturn("NEWHASH");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Passw0rd!");
        request.setNewPassword("NewPassw0rd");

        accountService.changePassword("acc-1", request);

        verify(accountRepository).save(argThat(a -> "NEWHASH".equals(a.getPasswordHash())));
    }

    @Test
    void changePassword_shouldFail_whenCurrentPasswordIsWrong() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("NewPassw0rd");

        assertThrows(InvalidCredentialsException.class, () -> accountService.changePassword("acc-1", request));
        verify(accountRepository, never()).save(any(Account.class));
    }

    // ---------------- admin operations ----------------

    @Test
    void listAccounts_shouldFilterByRole_whenRoleGiven() {
        when(accountRepository.findByRole(Role.PASSENGER)).thenReturn(List.of(stored));

        assertThat(accountService.listAccounts(Role.PASSENGER)).hasSize(1);
        verify(accountRepository, never()).findAll();
    }

    @Test
    void listAccounts_shouldReturnEveryone_whenNoRoleGiven() {
        when(accountRepository.findAll()).thenReturn(List.of(stored));

        assertThat(accountService.listAccounts(null)).hasSize(1);
        verify(accountRepository, never()).findByRole(any(Role.class));
    }

    @Test
    void updateStatus_shouldSuspendAccount() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse response = accountService.updateStatus("acc-1", AccountStatus.SUSPENDED);

        assertThat(response.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void updateRole_shouldChangeRole() {
        when(accountRepository.findById("acc-1")).thenReturn(Optional.of(stored));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(accountService.updateRole("acc-1", Role.DRIVER).getRole()).isEqualTo(Role.DRIVER);
    }

    @Test
    void delete_shouldThrow_whenAccountDoesNotExist() {
        when(accountRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.delete("missing"));
        verify(accountRepository, never()).delete(any(Account.class));
    }
}
