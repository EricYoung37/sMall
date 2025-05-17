package com.small.backend.authservice.security;

import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import security.AbstractJwtUtilBase;

import java.util.Date;

@Component
public class JwtUtil extends AbstractJwtUtilBase {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.exp}")
    private long accessExpMs;

    @Value("${jwt.refresh.exp}")
    private long refreshExpMs;

    @PostConstruct
    public void init() {
        initKey(secret);
    }

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpMs))
                .signWith(key)
                .compact();
    }
}