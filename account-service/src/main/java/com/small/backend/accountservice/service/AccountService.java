package com.small.backend.accountservice.service;

import dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;

import java.util.UUID;

public interface AccountService {
    UserAccount createAccount(CreateAccountRequest request);
    UserAccount updateAccount(UUID id, UpdateAccountRequest request);
    UserAccount getAccountById(UUID id);
    UserAccount getAccountByEmail(String email);
}
