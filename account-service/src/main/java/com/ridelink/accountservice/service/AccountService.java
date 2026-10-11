package com.ridelink.accountservice.service;

import com.ridelink.accountservice.dto.*;
import com.ridelink.accountservice.model.AccountStatus;
import com.ridelink.accountservice.model.Role;

import java.util.List;

public interface AccountService {

    AccountResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    AccountResponse getById(String id);

    AccountResponse updateProfile(String id, UpdateProfileRequest request);

    void changePassword(String id, ChangePasswordRequest request);

    /** All accounts, or only those with the given role when {@code role} is not null. */
    List<AccountResponse> listAccounts(Role role);

    AccountResponse updateRole(String id, Role role);

    AccountResponse updateStatus(String id, AccountStatus status);

    void delete(String id);
}
