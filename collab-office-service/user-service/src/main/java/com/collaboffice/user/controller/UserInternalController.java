package com.collaboffice.user.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user/internal")
@RequiredArgsConstructor
public class UserInternalController {

    private final SysUserMapper sysUserMapper;

    @PostMapping("/search")
    public CommonResult<List<Map<String, Object>>> searchInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return CommonResult.success(Collections.emptyList());
        }
        var users = sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.collaboffice.user.entity.SysUser>()
                        .like(com.collaboffice.user.entity.SysUser::getUsername, keyword)
                        .or()
                        .like(com.collaboffice.user.entity.SysUser::getRealName, keyword)
                        .last("LIMIT 5"));
        return CommonResult.success(users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("title", u.getRealName() != null ? u.getRealName() : u.getUsername());
            m.put("type", "user");
            return m;
        }).collect(Collectors.toList()));
    }

    @GetMapping("/{userId}")
    public CommonResult<Map<String, Object>> getUserInternal(
            @RequestHeader("X-Internal-Token") String token,
            @PathVariable Long userId) {
        var user = sysUserMapper.selectById(userId);
        if (user == null) {
            return CommonResult.failed(404, "用户不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("email", user.getEmail());
        m.put("phone", user.getPhone());
        return CommonResult.success(m);
    }
}
