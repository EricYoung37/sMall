package com.small.backend.authservice.controller;

import com.small.backend.authservice.dto.*;
import com.small.backend.authservice.security.JwtUtil;
import com.small.backend.authservice.service.AuthService;
import com.small.backend.authservice.dto.RegistrationRequest;
import dto.AccountDto;
import exception.ResourceAlreadyExistsException;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import util.AppConstants;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final ModelMapper modelMapper;
    private final JwtUtil jwtUtil;

    private final String AUTHORIZATION_HEADER = AppConstants.AUTHORIZATION_HEADER;
    private final String BEARER_PREFIX = AppConstants.BEARER_PREFIX;

    @Autowired
    public AuthController(AuthService authService,
                          ModelMapper modelMapper,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.modelMapper = modelMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegistrationRequest request) {
        try {
            authService.registerUserWithAccount(
                    modelMapper.map(request, CredentialDto.class),
                    modelMapper.map(request, AccountDto.class));
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully.");
        } catch (ResourceAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid CredentialDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(@RequestHeader(AUTHORIZATION_HEADER) String authHeader,
                                                 @RequestBody @Valid UpdatePasswordRequest request,
                                                 Authentication auth) {
        // No need to check header nullness or prefix because they've been checked by the JWT auth filter.
        String token = authHeader.substring(BEARER_PREFIX.length());
        String currentEmail = auth.getName();

        // A user can only update their own password.
        if (!jwtUtil.validateToken(token) || !currentEmail.equals(jwtUtil.extractEmail(token))) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }

        authService.updatePassword(token, currentEmail, request.getNewPassword());
        return ResponseEntity.ok("Password updated successfully.");
    }

    // The logout operation invalidates the token.
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(AUTHORIZATION_HEADER) String authHeader,
                                         Authentication auth) {
        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.validateToken(token) || !auth.getName().equals(jwtUtil.extractEmail(token))) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }

        authService.logout(token);
        return ResponseEntity.ok("Logout successful.");
    }
}