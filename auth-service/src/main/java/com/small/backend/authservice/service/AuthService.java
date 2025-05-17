package com.small.backend.authservice.service;

import com.small.backend.authservice.dto.LoginRequest;
import com.small.backend.authservice.dto.RefreshTokenResponse;
import com.small.backend.authservice.entity.UserCredential;
import com.small.backend.authservice.dto.RegistrationRequest;

import java.util.Optional;

public interface AuthService {
    RefreshTokenResponse login(LoginRequest request);
    void logout(String token);
    Optional<UserCredential> register(LoginRequest request);
    void updatePassword(String email, String newPassword, String refreshToken);
    RefreshTokenResponse refreshToken(String refreshToken);

    void deleteCredential(String email); // for roll-back if account-service fails to create an account
}
