package com.small.backend.authservice.security;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import security.AbstractJwtUtilBase;

import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil extends AbstractJwtUtilBase {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.exp.ms}")
    private long jwtExpMs;

    @PostConstruct
    public void init() {
        initKey(secret);
    }

    public String generateToken(String email) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .setSubject(email)
                .setId(jti)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpMs))
                .signWith(key)
                .compact();
    }
}