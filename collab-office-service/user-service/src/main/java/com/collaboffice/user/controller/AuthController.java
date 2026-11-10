package com.collaboffice.user.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.util.JwtTokenUtil;
import com.collaboffice.user.dto.*;
import com.collaboffice.user.service.TokenService;
import com.collaboffice.user.service.UserService;
import com.collaboffice.user.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final TokenService tokenService;
    private final JwtTokenUtil jwt;

    @PostMapping("/register")
    public CommonResult<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return CommonResult.success(userService.register(request));
    }
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return CommonResult.success(userService.login(request));
    }
    @PostMapping("/refresh-token")
    public CommonResult<String> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return CommonResult.success(tokenService.refreshAccessToken(request.getRefreshToken()));
    }
    @PostMapping("/logout")
    public CommonResult<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : "";
        if (!jwt.validateToken(token)) return CommonResult.failed(401,"登录已过期");
        userService.logout(Long.valueOf(jwt.getUserIdFromToken(token)),token);
        return CommonResult.success(null);
    }
}
