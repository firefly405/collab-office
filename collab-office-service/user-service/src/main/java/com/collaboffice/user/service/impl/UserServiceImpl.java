package com.collaboffice.user.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.user.dto.*;
import com.collaboffice.user.entity.SysUser;
import com.collaboffice.user.mapper.SysUserMapper;
import com.collaboffice.user.security.LoginRateLimiter;
import com.collaboffice.user.service.SmsService;
import com.collaboffice.user.service.TokenService;
import com.collaboffice.user.service.UserService;
import com.collaboffice.user.vo.LoginResponse;
import com.collaboffice.user.vo.RegisterResponse;
import com.collaboffice.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final com.collaboffice.user.service.SpaceService spaceService;
    private final SmsService smsService;
    private final TokenService tokenService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordEncoder passwordEncoder;
    private final com.collaboffice.user.mapper.UserLoginDeviceMapper userLoginDeviceMapper;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;
    private final com.collaboffice.common.util.JwtTokenUtil jwtTokenUtil;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final com.collaboffice.user.mapper.SysUserRoleMapper sysUserRoleMapper;
    private final com.collaboffice.user.mapper.SysRoleMapper sysRoleMapper;
    private final com.collaboffice.user.mapper.SysPermissionMapper sysPermissionMapper;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w+$");

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.getPhone() != null) sysUserMapper.selectByPhone(request.getPhone()).ifPresent(u -> {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "手机号已被注册");
        });

        sysUserMapper.selectByUsername(request.getUsername()).ifPresent(u -> {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "用户名已被占用");
        });

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRealName(request.getUsername());
        user.setStatus(1);
        user.setOnlineStatus(0);

        sysUserMapper.insert(user);

        TokenPairDTO tokens = tokenService.generateTokenPair(user.getId());

        return RegisterResponse.builder()
                .userId(user.getId())
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expireIn(tokens.getExpireIn())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        if (loginRateLimiter.isLocked(request.getAccount())) {
            throw new BusinessException(ResultCode.RATE_LIMITED);
        }

        SysUser user;
        if (PHONE_PATTERN.matcher(request.getAccount()).matches()) {
            user = sysUserMapper.selectByPhone(request.getAccount())
                    .orElseThrow(() -> handleFailedLogin(request.getAccount(), "账号不存在"));
        } else if (EMAIL_PATTERN.matcher(request.getAccount()).matches()) {
            user = sysUserMapper.selectByEmail(request.getAccount())
                    .orElseThrow(() -> handleFailedLogin(request.getAccount(), "账号不存在"));
        } else {
            user = sysUserMapper.selectByUsername(request.getAccount())
                    .orElseThrow(() -> handleFailedLogin(request.getAccount(), "账号不存在"));
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw handleFailedLogin(request.getAccount(), "密码错误");
        }

        loginRateLimiter.resetAttempt(request.getAccount());

        user.setLastLoginTime(LocalDateTime.now());
        user.setOnlineStatus(1);
        sysUserMapper.updateById(user);

        TokenPairDTO tokens = tokenService.generateTokenPair(user.getId());

        // Record device info
        try {
            String userAgent = httpServletRequest.getHeader("User-Agent");
            String ip = httpServletRequest.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpServletRequest.getRemoteAddr();
            }
            
            String deviceName = "Unknown Device";
            String deviceType = "WEB";
            
            if (userAgent != null) {
                deviceName = userAgent;
                if (deviceName.length() > 100) {
                    deviceName = deviceName.substring(0, 100);
                }
                
                String uaLower = userAgent.toLowerCase();
                if (uaLower.contains("android")) {
                    deviceType = "ANDROID";
                } else if (uaLower.contains("iphone") || uaLower.contains("ipad")) {
                    deviceType = "IOS";
                }
            }
            
            String tokenId = jwtTokenUtil.getJtiFromToken(tokens.getAccessToken());
            
            // Mark other devices as not current
            var otherDevices = userLoginDeviceMapper.selectCurrentDevices(user.getId());
            if (otherDevices != null) {
                for (var od : otherDevices) {
                    od.setIsCurrent(0);
                    userLoginDeviceMapper.updateById(od);
                }
            }
            
            com.collaboffice.user.entity.UserLoginDevice device = new com.collaboffice.user.entity.UserLoginDevice();
            device.setUserId(user.getId());
            device.setDeviceName(deviceName);
            device.setDeviceType(deviceType);
            device.setIpAddress(ip);
            device.setLastLoginTime(LocalDateTime.now());
            device.setTokenId(tokenId);
            device.setIsCurrent(1);
            device.setCreateTime(LocalDateTime.now());
            userLoginDeviceMapper.insert(device);
            
        } catch (Exception e) {
            log.error("Failed to record user login device", e);
        }

        // Load permissions to Redis for AOP verification
        try {
            loadUserPermissionsToRedis(user.getId());
        } catch (Exception e) {
            log.error("Failed to load user permissions to Redis", e);
        }

        UserInfoVO userInfo = toUserInfoVO(user);

        return LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expireIn(tokens.getExpireIn())
                .tokenType("Bearer")
                .userInfo(userInfo)
                .build();
    }

    private BusinessException handleFailedLogin(String account, String message) {
        loginRateLimiter.recordFailedAttempt(account);
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }

    @Override
    public void logout(Long userId, String accessToken) {
        tokenService.revokeToken(accessToken);
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            user.setOnlineStatus(0);
            sysUserMapper.updateById(user);
        }
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toUserInfoVO(user);
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = new SysUser();
        user.setId(userId);
        if (request.getNickname() != null) user.setRealName(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getGender() != null) user.setGender(request.getGender());
        sysUserMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "旧密码不正确");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);

        // 使所有Token失效
        tokenService.revokeToken("force_logout_" + userId);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!smsService.verifyCode(request.getPhone(), request.getCode())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "验证码错误或已过期");
        }

        SysUser user = sysUserMapper.selectByPhone(request.getPhone())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    private UserInfoVO toUserInfoVO(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPlatformRole(access.platformRole(user.getId()));
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setOnlineStatus(user.getOnlineStatus());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setLastLoginTime(user.getLastLoginTime());
        return vo;
    }

    private void loadUserPermissionsToRedis(Long userId) {
        String redisKey = "user:permissions:" + userId;
        redisTemplate.delete(redisKey);
        
        List<com.collaboffice.user.entity.SysUserRole> userRoles = sysUserRoleMapper.selectByUserId(userId);
        boolean isAdmin = false;
        
        if (userRoles != null) {
            for (var ur : userRoles) {
                com.collaboffice.user.entity.SysRole role = sysRoleMapper.selectById(ur.getRoleId());
                if (role != null) {
                    redisTemplate.opsForSet().add(redisKey, role.getRoleCode());
                    if ("admin".equalsIgnoreCase(role.getRoleCode()) || "ROLE_ADMIN".equalsIgnoreCase(role.getRoleCode())) {
                        isAdmin = true;
                    }
                }
            }
        }
        
        if (isAdmin) {
            List<com.collaboffice.user.entity.SysPermission> allPerms = sysPermissionMapper.selectList(null);
            if (allPerms != null) {
                for (var p : allPerms) {
                    redisTemplate.opsForSet().add(redisKey, p.getPermissionCode());
                }
            }
            redisTemplate.opsForSet().add(redisKey, "ROLE_ADMIN");
            redisTemplate.opsForSet().add(redisKey, "PERMISSION:AUDIT_LOG:READ");
        } else {
            redisTemplate.opsForSet().add(redisKey, "ROLE_MEMBER");
        }
        redisTemplate.expire(redisKey, 2, TimeUnit.HOURS);
    }

    @Override
    public LoginResponse completeMfaLogin(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);

        user.setLastLoginTime(LocalDateTime.now());
        user.setOnlineStatus(1);
        sysUserMapper.updateById(user);

        TokenPairDTO tokens = tokenService.generateTokenPair(userId);
        UserInfoVO userInfo = toUserInfoVO(user);
        return LoginResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .expireIn(tokens.getExpireIn())
                .tokenType("Bearer")
                .userInfo(userInfo)
                .build();
    }
}
