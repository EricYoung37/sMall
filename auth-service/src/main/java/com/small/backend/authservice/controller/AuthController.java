package com.small.backend.authservice.controller;

import com.small.backend.authservice.dto.*;
import com.small.backend.authservice.entity.UserCredential;
import com.small.backend.authservice.service.AuthService;
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
import util.AppConstants;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RestTemplate restTemplate;
    private final ModelMapper modelMapper;

    private final String internalToken;
    private final String internalAuthHeader;

    public AuthController(AuthService authService,
                          RestTemplate restTemplate,
                          ModelMapper modelMapper,
                          @Value("${internal.auth.token}") String internalToken,
                          @Value("${internal.auth.header}") String internalAuthHeader) {
        this.authService = authService;
        this.restTemplate = restTemplate;
        this.modelMapper = modelMapper;
        this.internalToken = internalToken;
        this.internalAuthHeader = internalAuthHeader;
    }

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
    // The frontend retrieves them and provide them to the backend.

    // The access token is handled by the JwtAuthenticationFilter.
    // The refresh token is parsed here.
    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(@RequestHeader(AppConstants.REFRESH_TOKEN_HEADER) @Valid String refreshToken,
                                                 @RequestBody @Valid UpdatePasswordRequest request,
                                                 Authentication auth) {
        String email = auth.getName();
        authService.updatePassword(email, request.getNewPassword(), refreshToken);
        return ResponseEntity.ok("Password updated successfully.");
    }

    // The access token may have expired, which is the very reason the client is calling /refresh.
    // Hence, the client must provide the refresh token.
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestHeader(AppConstants.REFRESH_TOKEN_HEADER) @Valid String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }

    // The logout operation invalidates the refresh token.
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(AppConstants.REFRESH_TOKEN_HEADER) @Valid String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok("Logout successful.");
    }
}