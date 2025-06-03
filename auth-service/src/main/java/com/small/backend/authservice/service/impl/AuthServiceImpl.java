package com.small.backend.authservice.service.impl;

import com.small.backend.authservice.dao.UserCredentialRepository;
import com.small.backend.authservice.dto.CredentialDto;
import com.small.backend.authservice.entity.UserCredential;
import com.small.backend.authservice.security.RedisJtiService;
import com.small.backend.authservice.service.AuthService;
import com.small.backend.authservice.security.JwtUtil;
import dto.AccountDto;
import exception.ResourceAlreadyExistsException;
import exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserCredentialRepository repository;
    private final RestTemplate restTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final RedisJtiService redisJtiService;

    private final String internalToken;
    private final String internalAuthHeader;

    @Autowired
    public AuthServiceImpl(UserCredentialRepository repository,
                           PasswordEncoder passwordEncoder,
                           RestTemplate restTemplate,
                           JwtUtil jwtUtil,
                           AuthenticationManager authManager,
                           RedisJtiService redisJtiService,
                           @Value("${internal.auth.token}") String internalToken,
                           @Value("${internal.auth.header}") String internalAuthHeader) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authManager = authManager;
        this.redisJtiService = redisJtiService;
        this.internalToken = internalToken;
        this.internalAuthHeader = internalAuthHeader;
    }

    @Override
    public UserCredential register(CredentialDto request) {
        UserCredential user = new UserCredential();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        try {
            return repository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResourceAlreadyExistsException("User email " + request.getEmail() + " already exists.");
        }
    }

    @Override
    public void registerUserWithAccount(CredentialDto credentialDto, AccountDto accountDto) {
        UserCredential savedUser = register(credentialDto);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(internalAuthHeader, internalToken);
            HttpEntity<AccountDto> entity = new HttpEntity<>(accountDto, headers);

            restTemplate.postForEntity("http://account-service/accounts", entity, Void.class);
        } catch (Exception e) {
            if (savedUser != null) {
                deleteCredential(savedUser.getEmail());
            }
            throw new RuntimeException("Failed to create user account. Registration rolled back.", e);
        }
    }

    @Override
    public String login(CredentialDto request) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        // This calls UserDetailsService.loadUserByUsername(email),
        // verifies the password matches the stored password hash,
        // throws an exception if credentials are invalid.
        // This happens before any JWT exists.
        // JwtAuthenticationFilter handles subsequent requests regarding JWT validation.

        UserCredential user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User email " + request.getEmail() + " not found."));

        String token = jwtUtil.generateToken(user.getEmail());
        redisJtiService.storeJti(jwtUtil.extractJti(token), jwtUtil.extractExpiration(token)); // save token JTI to Redis

        repository.save(user);

        return token;
    }

    @Override
    public void updatePassword(String token, String email, String newPassword) {
        UserCredential user = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User email " + email + " not found."));

        // Only logged-in user can update the password.
        // Check if token JTI exists in Redis.
        if(!redisJtiService.isJtiValid(jwtUtil.extractJti(token))) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    @Override
    public void logout(String token) {
        String jti = jwtUtil.extractJti(token);

        // Only logged-in user can log out.
        if(!redisJtiService.isJtiValid(jti)) {
            throw new AccessDeniedException("Invalid or expired access token.");
        }
        redisJtiService.revokeJti(jti);
    }

    // Roll back if account-service fails to create an account.
    @Override
    public void deleteCredential(String email) {
        repository.findByEmail(email).ifPresent(repository::delete);
    }
}