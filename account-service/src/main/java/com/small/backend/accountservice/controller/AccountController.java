package com.small.backend.accountservice.controller;

import com.small.backend.accountservice.security.JwtUtil;
import dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;
import com.small.backend.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import util.AppConstants;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;
    private final JwtUtil jwtUtil;

    private final String AUTHORIZATION_HEADER = AppConstants.AUTHORIZATION_HEADER;
    private final String BEARER_PREFIX = AppConstants.BEARER_PREFIX;

    @Autowired
    public AccountController(AccountService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping // internal authentication enforced by auth-service
    public ResponseEntity<UserAccount> create(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.ok(service.createAccount(request));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Object> getAccountByEmail(@PathVariable("email") String pathEmail,
                                                    @RequestHeader(AUTHORIZATION_HEADER) String authHeader,
                                                    Authentication auth) {
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (!jwtUtil.validateToken(token)) {
                throw new AccessDeniedException("Invalid access token.");
            }
            validateUser(auth.getName(), jwtUtil.extractEmail(token), pathEmail);
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }

        UserAccount userAccount = service.getAccountByEmail(token, pathEmail);
        return ResponseEntity.ok(userAccount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable("id") UUID id,
                                          @RequestHeader(AUTHORIZATION_HEADER) String authHeader,
                                          Authentication auth) {
        String token = authHeader.substring(BEARER_PREFIX.length());
        UserAccount userAccount = service.getAccountById(token, id);

        try {
            if (!jwtUtil.validateToken(token)) {
                throw new AccessDeniedException("Invalid access token.");
            }
            validateUser(auth.getName(), jwtUtil.extractEmail(token), userAccount.getEmail());
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }

        return ResponseEntity.ok(userAccount);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") UUID id,
                                         @RequestHeader(AUTHORIZATION_HEADER) String authHeader,
                                         @RequestBody @Valid UpdateAccountRequest request,
                                         Authentication auth) {
        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            if (!jwtUtil.validateToken(token)) {
                throw new AccessDeniedException("Invalid access token.");
            }
            validateUser(auth.getName(), jwtUtil.extractEmail(token), service.getAccountById(token, id).getEmail());
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }

        return ResponseEntity.ok(service.updateAccount(token, id, request));
    }

    private void validateUser(String authEmail, String tokenEmail, String targetEmail) {
        // Ensure the user is using their own token.
        if (!authEmail.equals(tokenEmail)) {
            throw new AccessDeniedException("Access Denied.");
        }

        // Ensure the user is accessing their own REST URI.
        if (!authEmail.equals(targetEmail)) {
            throw new AccessDeniedException("Access Denied.");
        }
    }
}