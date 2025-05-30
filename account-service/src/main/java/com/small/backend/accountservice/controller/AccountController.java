package com.small.backend.accountservice.controller;

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

import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    @Autowired
    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping // internal authentication enforced by auth-service
    public ResponseEntity<UserAccount> create(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createAccount(request));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Object> getAccountByEmail(@PathVariable("email") String pathEmail,
                                                    Authentication auth) {
        try {
            validateUser(auth.getName(), pathEmail);
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }
        UserAccount userAccount = service.getAccountByEmail(pathEmail);
        return ResponseEntity.ok(userAccount);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable("id") UUID id,
                                          Authentication auth) {
        UserAccount userAccount = service.getAccountById(id);

        try {
            validateUser(auth.getName(), userAccount.getEmail());
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }

        return ResponseEntity.ok(userAccount);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") UUID id,
                                         @RequestBody @Valid UpdateAccountRequest request,
                                         Authentication auth) {
        try {
            validateUser(auth.getName(), service.getAccountById(id).getEmail());
        } catch (AccessDeniedException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }
        return ResponseEntity.ok(service.updateAccount(id, request));
    }

    private void validateUser(String authEmail, String targetEmail) {
        // Ensure the user is accessing their own REST URI using their own token.
        // authEmail is parsed from the token by the API gateway.
        if (!authEmail.equals(targetEmail)) {
            throw new AccessDeniedException("Access Denied.");
        }
    }
}