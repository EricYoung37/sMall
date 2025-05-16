package com.small.backend.authservice.service;

import com.small.backend.authservice.dto.LoginRequest;
import com.small.backend.authservice.dto.RefreshTokenResponse;
import com.small.backend.authservice.dto.RegisterRequest;

public interface AuthService {
    RefreshTokenResponse login(LoginRequest request);
    void logout(String token);
    void register(RegisterRequest request);
    void updatePassword(String email, String newPassword, String refreshToken);
    RefreshTokenResponse refreshToken(String refreshToken);
}
