package com.small.backend.apigateway.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import security.AbstractJwtUtilBase;

@Component
public class JwtUtil extends AbstractJwtUtilBase {

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    public void init() {
        initKey(secret);
    }
}