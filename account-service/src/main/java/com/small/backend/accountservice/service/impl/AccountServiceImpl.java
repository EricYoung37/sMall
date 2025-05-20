package com.small.backend.accountservice.service.impl;

import com.small.backend.accountservice.dao.UserAccountRepository;
import dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;
import com.small.backend.accountservice.service.AccountService;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserAccountRepository repository;

    @Autowired
    public AccountServiceImpl(UserAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserAccount createAccount(CreateAccountRequest request) {
        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setShippingAddress(request.getShippingAddress());
        user.setBillingAddress(request.getBillingAddress());
        user.setPaymentMethod(request.getPaymentMethod());
        return repository.save(user);
    }

    @Override
    public UserAccount getAccountByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User with email " + email + " not found")
        );
    }

    @Override
    public UserAccount getAccountById(UUID id) {
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User with ID " + id + " not found")
        );
    }

    @Override
    public UserAccount updateAccount(UUID id, UpdateAccountRequest request) {
        UserAccount user = getAccountById(id);
        user.setUsername(request.getUsername());
        user.setShippingAddress(request.getShippingAddress());
        user.setBillingAddress(request.getBillingAddress());
        user.setPaymentMethod(request.getPaymentMethod());
        return repository.save(user);
    }
}