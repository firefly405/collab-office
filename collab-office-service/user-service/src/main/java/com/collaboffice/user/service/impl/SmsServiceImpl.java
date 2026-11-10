package com.collaboffice.user.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.user.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CODE_KEY_PREFIX = "sms:code:";
    private static final String LIMIT_KEY_PREFIX = "sms:limit:";
    private static final long CODE_EXPIRE_MINUTES = 5;
    private static final long LIMIT_SECONDS = 60;

    @Override
    public void sendCode(String phone) {
        String limitKey = LIMIT_KEY_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            throw new BusinessException(ResultCode.RATE_LIMITED.getCode(), "发送过于频繁，请60秒后再试");
        }

        String code = String.format("%06d", new Random().nextInt(1000000));
        log.info("向手机号 {} 发送验证码: {}", phone, code);

        String codeKey = CODE_KEY_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(limitKey, "1", LIMIT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String codeKey = CODE_KEY_PREFIX + phone;
        Object storedCode = redisTemplate.opsForValue().get(codeKey);
        if (storedCode == null) {
            return false;
        }
        if (storedCode.toString().equals(code)) {
            redisTemplate.delete(codeKey);
            return true;
        }
        return false;
    }
}
