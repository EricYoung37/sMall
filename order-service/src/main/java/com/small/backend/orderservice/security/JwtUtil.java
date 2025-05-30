package com.small.backend.orderservice.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import security.AbstractJwtUtilBase;

public class JwtUtil extends AbstractJwtUtilBase {
    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    public void init() {
        initKey(secret);
    }
}
