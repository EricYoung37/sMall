package com.small.backend.accountservice.service;

import dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;

import java.util.UUID;

public interface AccountService {
    UserAccount createAccount(CreateAccountRequest request);
    UserAccount getAccountByEmail(String token, String email);
    UserAccount getAccountById(String token, UUID id);
    UserAccount updateAccount(String token, UUID id, UpdateAccountRequest request);
}
