package com.collaboffice.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenUtil {

    private final SecretKey secretKey;

    private static final long ACCESS_TOKEN_EXPIRE = 1000L * 60 * 60 * 2; // 2小时
    private static final long REFRESH_TOKEN_EXPIRE = 1000L * 60 * 60 * 24 * 7; // 7天

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TYPE = "type";

    public JwtTokenUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TYPE, "access");
        return generateToken(claims, ACCESS_TOKEN_EXPIRE);
    }

    public String generateAccessToken(String userId, long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TYPE, "access");
        return generateToken(claims, expireMs);
    }

    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TYPE, "refresh");
        return generateToken(claims, REFRESH_TOKEN_EXPIRE);
    }

    public String generateRefreshToken(String userId, long expireMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, userId);
        claims.put(CLAIM_TYPE, "refresh");
        return generateToken(claims, expireMs);
    }

    private String generateToken(Map<String, Object> claims, long expireMs) {
        Date now = new Date();
        String jti = java.util.UUID.randomUUID().toString();
        return Jwts.builder()
                .id(jti)
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(secretKey)
                .compact();
    }

    public String getJtiFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getId() : null;
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的Token格式");
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误");
        } catch (Exception e) {
            log.warn("Token校验失败");
        }
        return false;
    }

    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(CLAIM_USER_ID, String.class) : null;
    }

    public Date getExpireTimeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims == null || claims.getExpiration().before(new Date());
    }

    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(CLAIM_TYPE, String.class) : null;
    }
}
