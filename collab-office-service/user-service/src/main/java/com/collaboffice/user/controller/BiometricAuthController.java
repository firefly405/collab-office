package com.collaboffice.user.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.service.UserService;
import com.collaboffice.user.vo.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Android 生物识别登录接口
 */
@RestController
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.biometric.enabled", havingValue="true")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class BiometricAuthController {

    private final UserService userService;

    @PostMapping("/biometric/login")
    public CommonResult<LoginResponse> biometricLogin(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String biometricToken = (String) body.get("biometricToken");
        // In production: verify biometricToken with Android BiometricPrompt server-side validation
        // For now: trust the userId after Gateway HMAC signature verification
        return CommonResult.success(userService.completeMfaLogin(userId));
    }
}
