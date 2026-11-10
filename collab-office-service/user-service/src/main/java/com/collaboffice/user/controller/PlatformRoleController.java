package com.collaboffice.user.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/roles")
public class PlatformRoleController {
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;
    private final org.springframework.data.redis.core.RedisTemplate<String,Object> redis;

    @GetMapping("/users")
    public CommonResult<List<Map<String,Object>>> users(@RequestParam(defaultValue="") String keyword) {
        access.requireSystemAdmin();
        if(keyword.length()>50) throw new BusinessException(400,"搜索词最多50字");
        var users=jdbc.queryForList("SELECT id AS userId,username,real_name AS realName,status FROM sys_user WHERE deleted=0 AND (username LIKE ? OR real_name LIKE ?) ORDER BY id LIMIT 200","%"+keyword+"%","%"+keyword+"%");
        for(var user:users) user.put("platformRole",access.platformRole(((Number)user.get("userId")).longValue()));
        return CommonResult.success(users);
    }

    @PutMapping("/users/{id}")
    @Transactional
    public CommonResult<Void> change(@PathVariable Long id,@RequestBody Map<String,String> body) {
        access.requireSystemAdmin();
        String role=body.get("platformRole");
        if(role==null || !Set.of("SYSTEM_ADMIN","PROJECT_ADMIN","MEMBER").contains(role)) throw new BusinessException(400,"身份无效");
        // Lock administrators before changes so concurrent demotions cannot remove the last one.
        jdbc.queryForList("SELECT id FROM sys_role WHERE LOWER(role_code) IN ('admin','role_admin') FOR UPDATE");
        var administrators=jdbc.queryForList("SELECT ur.user_id FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id JOIN sys_user u ON u.id=ur.user_id WHERE LOWER(r.role_code) IN ('admin','role_admin') AND r.deleted=0 AND r.status=1 AND u.deleted=0 AND u.status=1 FOR UPDATE",Long.class);
        if(!administrators.contains(access.userId())) throw new BusinessException(403,"系统身份已变更，请重新登录");
        if(jdbc.queryForList("SELECT id FROM sys_user WHERE id=? AND deleted=0 FOR UPDATE",id).isEmpty()) throw new BusinessException(404,"账号不存在");
        if(id.equals(access.userId())) throw new BusinessException(400,"不能修改自己的系统身份");
        if(administrators.contains(id) && !"SYSTEM_ADMIN".equals(role) && jdbc.queryForObject("SELECT COUNT(*) FROM sys_space WHERE owner_id=? AND status=1 AND deleted=0",Integer.class,id)>0) throw new BusinessException(409,"该账号仍持有团队，请先转让或解散团队");
        jdbc.update("DELETE ur FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE ur.user_id=? AND LOWER(r.role_code) IN ('admin','role_admin','project_admin','member','role_member')",id);
        jdbc.update("INSERT INTO sys_user_role(user_id,role_id) SELECT ?,id FROM sys_role WHERE role_code=? AND deleted=0 AND status=1",id,switch(role){case "SYSTEM_ADMIN"->"admin";case "PROJECT_ADMIN"->"project_admin";default->"member";});
        redis.delete("user:permissions:"+id);
        return CommonResult.success(null);
    }
}
