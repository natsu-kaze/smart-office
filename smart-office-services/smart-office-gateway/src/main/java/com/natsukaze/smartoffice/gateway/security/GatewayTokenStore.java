package com.natsukaze.smartoffice.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class GatewayTokenStore {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${smart-office.auth.token.enabled:true}")
    private boolean enabled;

    @Value("${smart-office.auth.token.key-prefix:smart-office:auth:token}")
    private String keyPrefix;

    public GatewayTokenStore(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> isActive(String token) {
        if (!enabled) {
            return Mono.just(true);
        }
        if (!StringUtils.hasText(token)) {
            return Mono.just(false);
        }
        return redisTemplate.hasKey(key(token)).onErrorReturn(false);
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
