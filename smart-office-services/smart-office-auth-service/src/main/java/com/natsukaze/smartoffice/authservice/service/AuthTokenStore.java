package com.natsukaze.smartoffice.authservice.service;

import com.natsukaze.smartoffice.authservice.security.UserPrincipal;
import com.natsukaze.smartoffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthTokenStore {

    private final StringRedisTemplate redisTemplate;

    @Value("${smart-office.auth.token.enabled:true}")
    private boolean enabled;

    @Value("${smart-office.auth.token.key-prefix:smart-office:auth:token}")
    private String keyPrefix;

    public void store(String token, UserPrincipal principal, long expiresInMillis) {
        if (!enabled) {
            return;
        }
        try {
            String value = principal.getUserId() + ":" + principal.getUsername();
            redisTemplate.opsForValue().set(key(token), value, Duration.ofMillis(expiresInMillis));
        } catch (RuntimeException ex) {
            throw new BusinessException("store login token failed");
        }
    }

    public boolean isActive(String token) {
        if (!enabled) {
            return true;
        }
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public void revoke(String token) {
        if (!enabled || !StringUtils.hasText(token)) {
            return;
        }
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return keyPrefix + ":" + sha256(token);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }
}
