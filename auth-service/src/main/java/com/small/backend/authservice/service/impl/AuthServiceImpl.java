package com.small.backend.authservice.service.impl;

import com.small.backend.authservice.dao.UserCredentialRepository;
import com.small.backend.authservice.dto.LoginRequest;
import com.small.backend.authservice.entity.UserCredential;
import com.small.backend.authservice.service.AuthService;
import com.small.backend.authservice.security.JwtUtil;
import exception.ResourceNotFoundException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public AuthServiceImpl(UserCredentialRepository repository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           AuthenticationManager authManager,
                           @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<UserCredential> register(LoginRequest request) {
        try {if (repository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
            UserCredential user = new UserCredential();
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            return Optional.of(repository.save(user));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public String login(LoginRequest request) {
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

        String token = jwtUtil.generateToken(user.getEmail());
        storeJti(jwtUtil.extractJti(token), jwtUtil.extractExpiration(token)); // save token JTI to Redis

        repository.save(user);

        return token;
    }

    @Override
    public void updatePassword(String token, String email, String newPassword) {
        UserCredential user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User email " + email + " not found."));

        // Only logged-in user can update the password.
        // Check if token JTI exists in Redis.
        if(!isJtiValid(jwtUtil.extractJti(token))) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    @Override
    public String refreshToken(String oldToken) {
        String jti = jwtUtil.extractJti(oldToken);

        // Only logged-in user can refresh the token.
        if(!isJtiValid(jti)) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }
        revokeJti(jti); // Invalidate token JTI in Redis.
        String newToken = jwtUtil.generateToken(jwtUtil.extractEmail(oldToken));
        storeJti(jwtUtil.extractJti(newToken), jwtUtil.extractExpiration(newToken));
        return newToken;
    }

    @Override
    public void logout(String token) {
        String jti = jwtUtil.extractJti(token);

        // Only logged-in user can log out.
        if(!isJtiValid(jti)) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }
        revokeJti(jti);
    }

    // Roll back if account-service fails to create an account.
    @Override
    public void deleteCredential(String email) {
        repository.findByEmail(email).ifPresent(repository::delete);
    }


    // Redis is used to store tokens.

    public void storeJti(String jti, Date expiration) {
        long ttlMs = expiration.getTime() - System.currentTimeMillis();
        try {
            redisTemplate.opsForValue().set(jti, "valid", ttlMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // ttlMs < 0
            e.printStackTrace();
        }
    }

    public boolean isJtiValid(String jti) {
        try {
            return redisTemplate.hasKey(jti);
        } catch (JwtException e) {
            // Signature invalid, malformed, expired, etc.
            return false;
        }
    }

    public void revokeJti(String jti) {
        redisTemplate.delete(jti);
    }
}