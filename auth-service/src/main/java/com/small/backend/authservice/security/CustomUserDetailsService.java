package com.small.backend.authservice.security;

import com.small.backend.authservice.dao.UserCredentialRepository;
import com.small.backend.authservice.entity.UserCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserCredentialRepository repository;

    @Autowired
    public CustomUserDetailsService(UserCredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        // This is implicitly called by the authentication manager inside login() in AuthServiceImpl.
        UserCredential user = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid token claims."));
        // Spring Security will wrap this exception internally, and CustomAuthenticationEntryPoint is triggered.
        // Therefore, GlobalExceptionHandler with @ControllerAdvice will not be triggered
        // (handleResourceNotFound(ResourceNotFoundException ex) will not be triggered),
        // and the response message will be "Bad credentials" instead of the custom one above.

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER") // returned by getAuthorities()
                .build();
    }
}
