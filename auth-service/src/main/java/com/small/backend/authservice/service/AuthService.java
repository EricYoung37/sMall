package com.small.backend.authservice.service;

import com.small.backend.authservice.dto.LoginRequest;
import com.small.backend.authservice.entity.UserCredential;

import java.util.Optional;

public interface AuthService {
    UserCredential register(LoginRequest request);
    String login(LoginRequest request);
    void updatePassword(String token, String email, String newPassword);
    void logout(String token);

    void deleteCredential(String email); // Roll back if account-service fails to create an account.
}
