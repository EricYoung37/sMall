package com.small.backend.authservice.service;

import com.small.backend.authservice.dto.CredentialDto;
import com.small.backend.authservice.entity.UserCredential;
import dto.AccountDto;

public interface AuthService {
    UserCredential register(CredentialDto request);
    void registerUserWithAccount(CredentialDto credentialDto, AccountDto accountDto);;
    String login(CredentialDto request);
    void updatePassword(String token, String email, String newPassword);
    void logout(String token);
    void deleteCredential(String email); // Roll back if account-service fails to create an account.
}
