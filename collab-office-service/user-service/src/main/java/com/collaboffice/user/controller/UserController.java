package com.collaboffice.user.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.dto.ChangePasswordRequest;
import com.collaboffice.user.dto.ResetPasswordRequest;
import com.collaboffice.user.dto.UpdateProfileRequest;
import com.collaboffice.user.service.UserService;
import com.collaboffice.user.vo.UserInfoVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @GetMapping("/directory")
    public CommonResult<java.util.List<java.util.Map<String,Object>>> directory(@RequestParam(defaultValue="") String keyword) {
        return CommonResult.success(jdbc.queryForList("SELECT id AS userId,username,real_name AS realName FROM sys_user WHERE deleted=0 AND status=1 AND (username LIKE ? OR real_name LIKE ?) ORDER BY id LIMIT 100", "%"+keyword+"%", "%"+keyword+"%"));
    }

    @GetMapping("/info")
    public CommonResult<UserInfoVO> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(userService.getUserInfo(userId));
    }

    @PutMapping("/profile")
    public CommonResult<Void> updateProfile(@RequestHeader("X-User-Id") Long userId,
                                            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(userId, request);
        return CommonResult.success(null);
    }

    @PutMapping("/password")
    public CommonResult<Void> changePassword(@RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return CommonResult.success("密码修改成功", null);
    }

}
