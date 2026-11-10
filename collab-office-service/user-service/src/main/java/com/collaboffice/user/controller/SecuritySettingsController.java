package com.collaboffice.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.entity.SysLog;
import com.collaboffice.user.entity.SysUser;
import com.collaboffice.user.entity.UserLoginDevice;
import com.collaboffice.user.mapper.SysLogMapper;
import com.collaboffice.user.mapper.SysUserMapper;
import com.collaboffice.user.mapper.UserLoginDeviceMapper;
import com.collaboffice.user.service.MfaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class SecuritySettingsController {

    private final SysUserMapper sysUserMapper;
    private final SysLogMapper sysLogMapper;
    private final UserLoginDeviceMapper deviceMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MfaService mfaService;

    // === MFA ===

    @PostMapping("/mfa/setup")
    public CommonResult<Map<String, String>> setupMFA(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(mfaService.setupMfa(userId));
    }

    @PostMapping("/mfa/enable")
    public CommonResult<Map<String, String>> enableMFA(@RequestHeader("X-User-Id") Long userId,
                                                        @RequestBody Map<String, String> body) {
        String code = body.get("code");
        boolean verified = mfaService.verifyAndActivateMfa(userId, code);
        if (!verified) {
            return CommonResult.failed(400, "TOTP验证码错误，请重试");
        }
        Map<String, String> result = new LinkedHashMap<>();
        result.put("status", "activated");
        return CommonResult.success(result);
    }

    @PostMapping("/mfa/verify")
    public CommonResult<Void> verifyMFA(@RequestHeader("X-User-Id") Long userId,
                                        @RequestBody Map<String, String> body) {
        String code = body.get("code");
        boolean valid = mfaService.verifyMfaCode(userId, code);
        if (!valid) {
            return CommonResult.failed(400, "TOTP验证码错误");
        }
        return CommonResult.success(null);
    }

    @PostMapping("/mfa/disable")
    public CommonResult<Void> disableMFA(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(null);
    }

    // === Device Management ===

    @GetMapping("/devices")
    public CommonResult<List<Map<String, Object>>> getDevices(@RequestHeader("X-User-Id") Long userId) {
        List<UserLoginDevice> devices = deviceMapper.selectByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserLoginDevice d : devices) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getDeviceName());
            m.put("type", d.getDeviceType());
            m.put("ip", d.getIpAddress());
            m.put("lastActive", d.getLastLoginTime() != null ? d.getLastLoginTime().toString() : null);
            m.put("isCurrent", d.getIsCurrent() != null && d.getIsCurrent() == 1);
            result.add(m);
        }
        return CommonResult.success(result);
    }

    @PostMapping("/device/{deviceId}/revoke")
    public CommonResult<Void> revokeDevice(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable Long deviceId) {
        UserLoginDevice device = deviceMapper.selectById(deviceId);
        if (device != null && device.getUserId().equals(userId)) {
            // Blacklist the token
            if (device.getTokenId() != null) {
                redisTemplate.opsForValue().set("token:blacklist:" + device.getTokenId(), "1", 7, TimeUnit.DAYS);
            }
            deviceMapper.deleteById(deviceId);
        }
        return CommonResult.success(null);
    }

    @DeleteMapping("/devices/{deviceId}")
    public CommonResult<Void> deleteDevice(@RequestHeader("X-User-Id") Long userId,
                                           @PathVariable Long deviceId) {
        return revokeDevice(userId, deviceId);
    }

    // === Login History ===

    @GetMapping("/login-history")
    public CommonResult<List<SysLog>> loginHistory(@RequestHeader("X-User-Id") Long userId) {
        List<SysLog> logs = sysLogMapper.selectList(
            new LambdaQueryWrapper<SysLog>()
                .eq(SysLog::getUserId, userId)
                .eq(SysLog::getModule, "auth")
                .orderByDesc(SysLog::getCreateTime)
                .last("LIMIT 20")
        );
        return CommonResult.success(logs);
    }
}
