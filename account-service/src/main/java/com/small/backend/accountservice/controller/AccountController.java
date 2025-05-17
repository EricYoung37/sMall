package com.small.backend.accountservice.controller;

import dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;
import com.small.backend.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    @Autowired
    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping // internal authentication enforced by auth-service
    public ResponseEntity<UserAccount> create(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.ok(service.createAccount(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") UUID id,
                                              @RequestBody @Valid UpdateAccountRequest request,
                                              Authentication auth) {
        UserAccount userAccount = service.getAccountById(id);
        if (!auth.getName().equals(userAccount.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }
        return ResponseEntity.ok(service.updateAccount(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable("id") UUID id,
                                               Authentication auth) {
        UserAccount userAccount = service.getAccountById(id);
        if (!auth.getName().equals(userAccount.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }
        return ResponseEntity.ok(userAccount);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Object> getAccountByEmail(@PathVariable("email") String email,
                                                         Authentication auth) {
        if (!auth.getName().equals(email)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("Access Denied.");
        }
        UserAccount userAccount = service.getAccountByEmail(email);
        return ResponseEntity.ok(userAccount);
    }
}