package com.collaboffice.user.controller;

import com.collaboffice.common.annotation.RequirePermission;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.dto.*;
import com.collaboffice.user.service.SpaceMemberService;
import com.collaboffice.user.service.SpaceService;
import com.collaboffice.user.vo.SpaceMemberVO;
import com.collaboffice.user.vo.SpaceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/space")
@RequiredArgsConstructor
public class SpaceController {

    private final SpaceService spaceService;
    private final SpaceMemberService spaceMemberService;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @PostMapping("/{spaceId}/members")
    @org.springframework.transaction.annotation.Transactional
    public CommonResult<Void> addMembers(@PathVariable Long spaceId, @RequestBody java.util.List<Long> users) {
        access.requireSpace(spaceId, true);
        if (!access.isAdmin(spaceId)) throw new com.collaboffice.common.exception.BusinessException(403,"仅管理员可以邀请成员");
        for (Long user : new java.util.HashSet<>(users)) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id=? AND deleted=0 AND status=1",Integer.class,user)==0) throw new com.collaboffice.common.exception.BusinessException(400,"用户不存在");
            jdbc.update("INSERT IGNORE INTO sys_space_member(space_id,user_id,role_type) VALUES (?,?,'2')",spaceId,user);
        }
        return CommonResult.success(null);
    }

    @PostMapping("/create")
    public CommonResult<SpaceVO> createSpace(@RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody SpaceCreateRequest request) {
        return CommonResult.success(spaceService.createSpace(userId, request));
    }

    @RequirePermission("space:write")
    @PutMapping("/{spaceId}")
    public CommonResult<SpaceVO> updateSpace(@PathVariable Long spaceId,
                                             @RequestHeader("X-User-Id") Long userId,
                                             @Valid @RequestBody SpaceUpdateRequest request) {
        return CommonResult.success(spaceService.updateSpace(spaceId, userId, request));
    }

    @RequirePermission("space:admin")
    @DeleteMapping("/{spaceId}")
    public CommonResult<Void> deleteSpace(@PathVariable Long spaceId,
                                          @RequestHeader("X-User-Id") Long userId) {
        spaceService.deleteSpace(spaceId, userId);
        return CommonResult.success(null);
    }

    @GetMapping("/{spaceId}")
    public CommonResult<SpaceVO> getSpaceInfo(@PathVariable Long spaceId) {
        access.requireSpace(spaceId,false);
        return CommonResult.success(spaceService.getSpaceInfo(spaceId));
    }

    @GetMapping("/my")
    public CommonResult<List<SpaceVO>> listMySpaces(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(spaceService.listMySpaces(userId));
    }

    @RequirePermission("space:admin")
    @PostMapping("/{spaceId}/members/invite")
    public CommonResult<Void> inviteMembers(@RequestHeader("X-User-Id") Long userId,
                                            @Valid @RequestBody MemberInviteRequest request) {
        spaceMemberService.inviteMembers(userId, request);
        return CommonResult.success("邀请成功", null);
    }

    @DeleteMapping("/{spaceId}/members/{userId}")
    public CommonResult<Void> removeMember(@PathVariable Long spaceId,
                                           @PathVariable Long userId,
                                           @RequestHeader("X-User-Id") Long operatorId) {
        spaceMemberService.removeMember(operatorId, spaceId, userId);
        return CommonResult.success(null);
    }

    @PutMapping("/{spaceId}/members/role")
    public CommonResult<Void> changeRole(@RequestHeader("X-User-Id") Long operatorId,
                                         @Valid @RequestBody MemberRoleUpdateRequest request) {
        spaceMemberService.changeRole(operatorId, request);
        return CommonResult.success(null);
    }

    @GetMapping("/{spaceId}/members")
    public CommonResult<List<SpaceMemberVO>> listMembers(@PathVariable Long spaceId) {
        access.requireSpace(spaceId, false);
        return CommonResult.success(spaceMemberService.listMembers(spaceId));
    }
}
