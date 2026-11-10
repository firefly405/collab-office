package com.collaboffice.user.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.common.util.JwtTokenUtil;
import com.collaboffice.user.dto.TokenPairDTO;
import com.collaboffice.user.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtTokenUtil jwtTokenUtil;

    private static final String REFRESH_PREFIX = "token:refresh:";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final long ACCESS_EXPIRE_SECONDS = 7200L;
    private static final long REFRESH_EXPIRE_DAYS = 7L;

    @Override
    public TokenPairDTO generateTokenPair(Long userId) {
        String accessToken = jwtTokenUtil.generateAccessToken(userId.toString(), ACCESS_EXPIRE_SECONDS * 1000);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userId.toString());

        redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, refreshToken, REFRESH_EXPIRE_DAYS, TimeUnit.DAYS);

        return TokenPairDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expireIn(ACCESS_EXPIRE_SECONDS)
                .build();
    }

    @Override
    public String refreshAccessToken(String refreshToken) {
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        String storedRefreshToken = (String) redisTemplate.opsForValue().get(REFRESH_PREFIX + userId);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }

        return jwtTokenUtil.generateAccessToken(userId, ACCESS_EXPIRE_SECONDS * 1000);
    }

    @Override
    public void revokeToken(String accessToken) {
        if (accessToken == null || accessToken.startsWith("force_logout_")) {
            return; // 非真实Token，无需加入黑名单
        }
        String userId = jwtTokenUtil.getUserIdFromToken(accessToken);
        if (userId != null) {
            var expireTime = jwtTokenUtil.getExpireTimeFromToken(accessToken);
            if (expireTime != null) {
                long remaining = expireTime.getTime() - System.currentTimeMillis();
                if (remaining > 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", remaining, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public boolean validateRefreshToken(String refreshToken) {
        String userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        if (userId == null) return false;
        String stored = (String) redisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
        return refreshToken.equals(stored);
    }
}
