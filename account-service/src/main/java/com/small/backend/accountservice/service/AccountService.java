package com.small.backend.accountservice.service;

import dto.AccountDto;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;

import java.util.UUID;

public interface AccountService {
    UserAccount createAccount(AccountDto request);
    UserAccount getAccountByEmail(String email);
    UserAccount getAccountById(UUID id);
    UserAccount updateAccount(UUID id, UpdateAccountRequest request);
}
