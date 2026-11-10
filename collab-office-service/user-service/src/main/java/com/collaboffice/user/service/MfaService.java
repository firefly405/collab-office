package com.collaboffice.user.service;

import java.util.Map;

/**
 * MFA TOTP 多因素认证服务
 */
public interface MfaService {

    /**
     * 为用户生成 TOTP 密钥并返回二维码信息
     * @param userId 用户ID
     * @return 包含 secret（Base32）和 qrCodeUri（otpauth://...）的 Map
     */
    Map<String, String> setupMfa(Long userId);

    /**
     * 验证 TOTP 6位码并激活 MFA
     * @param userId 用户ID
     * @param code   6位 TOTP 验证码
     * @return true=验证通过
     */
    boolean verifyAndActivateMfa(Long userId, String code);

    /**
     * 验证 TOTP 6位码（登录时二次验证）
     * @param userId 用户ID
     * @param code   6位 TOTP 验证码
     * @return true=验证通过
     */
    boolean verifyMfaCode(Long userId, String code);
}
