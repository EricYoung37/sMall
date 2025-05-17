package com.small.backend.authservice.controller;

import com.small.backend.authservice.dto.*;
import com.small.backend.authservice.entity.UserCredential;
import com.small.backend.authservice.service.AuthService;
import com.small.backend.authservice.security.JwtUtil;
import com.small.backend.authservice.dto.RegistrationRequest;
import dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RestTemplate restTemplate;
    private final ModelMapper modelMapper;

    @Autowired
    public AuthController(AuthService authService, RestTemplate restTemplate, ModelMapper modelMapper) {
        this.authService = authService;
        this.restTemplate = restTemplate;
        this.modelMapper = modelMapper;
    }

    @Value("${internal.api.token}")
    private String internalToken;

    @Value("${internal.auth.header}")
    private String internalAuthHeader;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegistrationRequest request) {
        // 1. Create user credential
        UserCredential savedUser = authService.register(modelMapper.map(request, LoginRequest.class))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register user credentials."));

        // 2. Create user account in account-service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(internalAuthHeader, internalToken);
            HttpEntity<CreateAccountRequest> entity = new HttpEntity<>(
                    modelMapper.map(request, CreateAccountRequest.class),
                    headers);

            restTemplate.postForEntity("http://localhost:8081/api/v1/accounts", entity, Void.class);
        } catch (RestClientException e) {
            // Compensate: delete the created user credential to keep data consistent
            if (savedUser != null) {
                authService.deleteCredential(savedUser.getEmail());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create user account. Registration rolled back.");
        }
        // We may do this creation sequence within a transaction, but that's expensive.

        // We may also try creating an account before the credential and delete the account if credential creation fails.
        // However, this may result in an account without a credential.

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully.");
    }

    @PostMapping("/login")
    public ResponseEntity<RefreshTokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Tokens are stored on the client side (memory or Cookie),
    // and the frontend retrieves them and properly provide them to the backend.

    // A valid access token is required in the Authorization: Bearer <token> header.
    // A valid refresh token is required in the request body (see class UpdatePasswordRequest for the reason).
    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(@RequestBody @Valid UpdatePasswordRequest request,
                                                 Authentication auth) {
        String email = auth.getName();
        authService.updatePassword(email, request.getNewPassword(), request.getRefreshToken());
        return ResponseEntity.ok("Password updated successfully.");
    }

    // The access token may have expired, which is the very reason the client is calling /refresh.
    // Hence, the refresh token needs to be presented in the request body.
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    // The logout operation invalidates the refresh token,
    // so this token needs to be presented in the request body.
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok("Logout successful.");
    }
}