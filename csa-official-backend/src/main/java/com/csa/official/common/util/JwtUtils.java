package com.csa.official.common.util;

import com.csa.official.modules.sys.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtUtils {

    /** HS256 要求密钥至少 256 bit (32 字节) */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${csa.jwt.secret}")
    private String secret;

    @Value("${csa.jwt.expiration}")
    private long expiration;

    @jakarta.annotation.PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "csa.jwt.secret 必须配置且长度不少于 " + MIN_SECRET_BYTES + " 字节 (HS256 要求 256bit)");
        }
    }

    public long getExpirationMillis() {
        return expiration;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // 生成 Token (包含 userId)
    public String generateToken(User user, long expireMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roleLevel", user.getRoleLevel());
        claims.put("userId", user.getId());
        claims.put("sessionVersion", user.getSessionVersion() == null ? 0L : user.getSessionVersion());

        Date now = new Date();
        Date expirationDate = expireMillis > 0
                ? new Date(now.getTime() + expireMillis)
                : new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {

        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }
        return null; // 或者抛异常
    }

    public String getTokenIdFromToken(String token) {
        return extractAllClaims(token).getId();
    }

    public Long getSessionVersionFromToken(String token) {
        Object version = extractAllClaims(token).get("sessionVersion");
        if (version instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public long getRemainingValidityMillis(String token) {
        Date expirationDate = extractAllClaims(token).getExpiration();
        return Math.max(0, expirationDate.getTime() - System.currentTimeMillis());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
