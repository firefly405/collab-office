package com.collaboffice.user.service.impl;

import com.collaboffice.user.entity.SysUser;
import com.collaboffice.user.mapper.SysUserMapper;
import com.collaboffice.user.service.MfaService;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * MFA TOTP 多因素认证实现（基于 dev.samstevens.totp）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final SysUserMapper userMapper;

    private static final String ISSUER = "CollabOffice";
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    @Override
    public Map<String, String> setupMfa(Long userId) {
        SysUser user = findUser(userId);

        // 生成新的 TOTP 密钥
        String secret = secretGenerator.generate();
        user.setMfaSecret(secret);
        // 暂不激活，等待 verify 后再激活
        userMapper.updateById(user);

        // 构造 otpauth:// URI 用于生成二维码
        QrData qrData = new QrData.Builder()
                .label(user.getUsername())
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeUri = qrData.getUri();

        // 生成内联 Base64 PNG 二维码（前端直接 <img src='...'/> 渲染）
        String qrCodeBase64 = "";
        try {
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(qrData);
            qrCodeBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            log.warn("QR code generation failed, returning URI only: {}", e.getMessage());
        }

        Map<String, String> result = new HashMap<>();
        result.put("secret", secret);
        result.put("qrCodeUri", qrCodeUri);
        result.put("qrCodeBase64", qrCodeBase64);
        return result;
    }

    @Override
    public boolean verifyAndActivateMfa(Long userId, String code) {
        SysUser user = findUser(userId);
        if (user.getMfaSecret() == null) {
            throw new RuntimeException("请先调用 /mfa/setup 初始化 MFA");
        }
        boolean valid = codeVerifier.isValidCode(user.getMfaSecret(), code);
        if (valid) {
            user.setMfaEnabled(1);
            userMapper.updateById(user);
            log.info("MFA activated for userId={}", userId);
        }
        return valid;
    }

    @Override
    public boolean verifyMfaCode(Long userId, String code) {
        SysUser user = findUser(userId);
        if (user.getMfaEnabled() == null || user.getMfaEnabled() == 0 || user.getMfaSecret() == null) {
            // 用户未启用 MFA，直接通过
            return true;
        }
        return codeVerifier.isValidCode(user.getMfaSecret(), code);
    }

    private SysUser findUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在: " + userId);
        return user;
    }
}
