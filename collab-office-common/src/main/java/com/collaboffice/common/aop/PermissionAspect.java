package com.collaboffice.common.aop;

import com.collaboffice.common.annotation.RequirePermission;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Before("@annotation(com.collaboffice.common.annotation.RequirePermission)")
    public void doPermissionCheck(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return;
        }

        String requiredPerm = annotation.value();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权限访问");
        }

        HttpServletRequest request = attributes.getRequest();
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String redisKey = "user:permissions:" + userIdStr;
        Boolean hasPerm = redisTemplate.opsForSet().isMember(redisKey, requiredPerm);
        if (hasPerm == null || !hasPerm) {
            log.warn("Permission denied for user={}: requires '{}'", userIdStr, requiredPerm);
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "权限不足，拒绝访问");
        }
    }
}
