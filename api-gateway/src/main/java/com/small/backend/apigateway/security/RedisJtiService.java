package com.small.backend.apigateway.security;

import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisJtiService {
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisJtiService(@Qualifier("customRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isJtiValid(String jti) {
        try {
            return redisTemplate.hasKey(jti);
        } catch (JwtException e) {
            // Signature invalid, malformed, expired, etc.
            return false;
        }
    }
}