package com.small.backend.accountservice.service.impl;

import com.small.backend.accountservice.dao.UserAccountRepository;
import com.small.backend.accountservice.security.JwtUtil;
import com.small.backend.accountservice.security.RedisJtiService;
import dto.CreateAccountRequest;
import com.small.backend.accountservice.dto.UpdateAccountRequest;
import com.small.backend.accountservice.entity.UserAccount;
import com.small.backend.accountservice.service.AccountService;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserAccountRepository repository;
    private final JwtUtil jwtUtil;
    private final RedisJtiService redisJtiService;

    @Autowired
    public AccountServiceImpl(UserAccountRepository repository, JwtUtil jwtUtil, RedisJtiService redisJtiService) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
        this.redisJtiService = redisJtiService;
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
    public UserAccount getAccountByEmail(String token, String email) {
        if(!redisJtiService.isJtiValid(jwtUtil.extractJti(token))) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }
        return repository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("User with email " + email + " not found")
        );
    }

    @Override
    public UserAccount getAccountById(String token, UUID id) {
        if(!redisJtiService.isJtiValid(jwtUtil.extractJti(token))) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("User with ID " + id + " not found")
        );
    }

    @Override
    public UserAccount updateAccount(String token, UUID id, UpdateAccountRequest request) {
        UserAccount user = getAccountById(token, id);
        user.setUsername(request.getUsername());
        user.setShippingAddress(request.getShippingAddress());
        user.setBillingAddress(request.getBillingAddress());
        user.setPaymentMethod(request.getPaymentMethod());
        return repository.save(user);
    }
}