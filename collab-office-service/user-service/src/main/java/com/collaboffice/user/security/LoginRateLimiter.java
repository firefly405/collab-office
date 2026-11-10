package com.collaboffice.user.security;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 30;

    public void recordFailedAttempt(String account) {
        String key = FAIL_KEY_PREFIX + account;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count >= MAX_ATTEMPTS) {
            redisTemplate.expire(key, LOCK_MINUTES, TimeUnit.MINUTES);
            throw new BusinessException(ResultCode.RATE_LIMITED.getCode(),
                    "登录失败超过" + MAX_ATTEMPTS + "次，账号已锁定" + LOCK_MINUTES + "分钟");
        }
        redisTemplate.expire(key, LOCK_MINUTES, TimeUnit.MINUTES);
    }

    public boolean isLocked(String account) {
        String key = FAIL_KEY_PREFIX + account;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null && Integer.parseInt(count.toString()) >= MAX_ATTEMPTS;
    }

    public void resetAttempt(String account) {
        redisTemplate.delete(FAIL_KEY_PREFIX + account);
    }
}
