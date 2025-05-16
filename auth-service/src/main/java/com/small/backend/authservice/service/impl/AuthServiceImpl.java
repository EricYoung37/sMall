package com.small.backend.authservice.service.impl;

import com.small.backend.authservice.dao.UserCredentialRepository;
import com.small.backend.authservice.dto.LoginRequest;
import com.small.backend.authservice.dto.RefreshTokenResponse;
import com.small.backend.authservice.dto.RegisterRequest;
import com.small.backend.authservice.entity.UserCredential;
import com.small.backend.authservice.service.AuthService;
import com.small.backend.authservice.security.JwtUtil;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    @Autowired
    public AuthServiceImpl(UserCredentialRepository repository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuthenticationManager authManager) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
    }

    @Override
    public RefreshTokenResponse login(LoginRequest request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        // This calls UserDetailsService.loadUserByUsername(email),
        // verifies the password matches the stored password hash,
        // throws an exception if credentials are invalid.
        // This happens before any JWT exists.
        // JwtAuthenticationFilter handles subsequent requests regarding JWT validation.

        UserCredential user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User email " + request.getEmail() + " not found."));

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        user.setRefreshToken(refreshToken);
        repository.save(user);

        return new RefreshTokenResponse(accessToken, refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        UserCredential user = repository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AccessDeniedException("Invalid or expired refresh token"));
        user.setRefreshToken(null);
        repository.save(user);
    }

    @Override
    public void register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        UserCredential user = new UserCredential();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        repository.save(user);
    }

    @Override
    public void updatePassword(String email, String newPassword, String refreshToken) {
        UserCredential user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User email " + email + " not found."));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AccessDeniedException("Invalid refresh token.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    @Override
    public RefreshTokenResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AccessDeniedException("Invalid refresh token.");
        }

        String email = jwtUtil.extractEmail(refreshToken);

        UserCredential user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User email " + email + " not found."));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new AccessDeniedException("Invalid refresh token.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        user.setRefreshToken(newRefreshToken);
        repository.save(user);

        return new RefreshTokenResponse(newAccessToken, newRefreshToken);
    }
}