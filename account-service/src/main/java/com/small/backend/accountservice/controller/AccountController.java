package com.small.backend.accountservice.controller;

import com.small.backend.accountservice.dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;
import com.small.backend.accountservice.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<UserAccount> create(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.ok(service.createAccount(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserAccount> update(@PathVariable UUID id, @RequestBody @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(service.updateAccount(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAccount> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getAccountById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserAccount> getAccountByEmail(@PathVariable String email) {
        UserAccount userAccount = service.getAccountByEmail(email);
        return ResponseEntity.ok(userAccount);
    }
}