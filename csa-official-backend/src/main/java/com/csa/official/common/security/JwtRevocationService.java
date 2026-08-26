package com.csa.official.common.security;

import com.csa.official.common.cache.KeyValueStore;
import com.csa.official.common.util.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
public class JwtRevocationService {

    private static final String REVOKED_TOKEN_PREFIX = "auth:jwt:revoked:";

    private final KeyValueStore keyValueStore;
    private final JwtUtils jwtUtils;

    public JwtRevocationService(KeyValueStore keyValueStore, JwtUtils jwtUtils) {
        this.keyValueStore = keyValueStore;
        this.jwtUtils = jwtUtils;
    }

    public void revoke(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        long remainingMillis = jwtUtils.getRemainingValidityMillis(token);
        if (remainingMillis <= 0) {
            return;
        }

        keyValueStore.setString(revocationKey(token), "1", remainingMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isRevoked(String token) {
        return StringUtils.hasText(token) && keyValueStore.hasKey(revocationKey(token));
    }

    private String revocationKey(String token) {
        String tokenId = jwtUtils.getTokenIdFromToken(token);
        if (StringUtils.hasText(tokenId)) {
            return REVOKED_TOKEN_PREFIX + "jti:" + tokenId;
        }

        return REVOKED_TOKEN_PREFIX + "sha256:" + sha256(token);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
