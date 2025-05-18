package com.small.backend.authservice.security;

import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class RedisJtiService {
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public RedisJtiService(@Qualifier("customRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeJti(String jti, Date expiration) {
        long ttlMs = expiration.getTime() - System.currentTimeMillis();
        try {
            redisTemplate.opsForValue().set(jti, "valid", ttlMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // ttlMs < 0
            e.printStackTrace();
        }
    }

    public boolean isJtiValid(String jti) {
        try {
            return redisTemplate.hasKey(jti);
        } catch (JwtException e) {
            // Signature invalid, malformed, expired, etc.
            return false;
        }
    }

    public void revokeJti(String jti) {
        redisTemplate.delete(jti);
    }
}
