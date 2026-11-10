package com.collaboffice.user.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.user.dto.MemberInviteRequest;
import com.collaboffice.user.dto.MemberRoleUpdateRequest;
import com.collaboffice.user.entity.SysSpace;
import com.collaboffice.user.entity.SysSpaceMember;
import com.collaboffice.user.entity.SysUser;
import com.collaboffice.user.mapper.SysSpaceMapper;
import com.collaboffice.user.mapper.SysSpaceMemberMapper;
import com.collaboffice.user.mapper.SysUserMapper;
import com.collaboffice.user.service.SpaceMemberService;
import com.collaboffice.user.vo.SpaceMemberVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpaceMemberServiceImpl implements SpaceMemberService {

    private final SysSpaceMapper sysSpaceMapper;
    private final SysSpaceMemberMapper sysSpaceMemberMapper;
    private final SysUserMapper sysUserMapper;

    private static final int ROLE_ADMIN = 1;

    @Override
    @Transactional
    public void inviteMembers(Long inviterId, MemberInviteRequest request) {
        checkAdminPermission(request.getSpaceId(), inviterId);

        for (Long userId : request.getMemberIds()) {
            SysUser user = sysUserMapper.selectById(userId);
            if (user == null) {
                log.warn("邀请成员失败: 用户{}不存在", userId);
                continue;
            }

            boolean alreadyMember = sysSpaceMemberMapper.selectBySpaceId(request.getSpaceId())
                    .stream().anyMatch(m -> m.getUserId().equals(userId));
            if (alreadyMember) continue;

            SysSpaceMember member = new SysSpaceMember();
            member.setSpaceId(request.getSpaceId());
            member.setUserId(userId);
            member.setRoleType(request.getRoleType() != null ? request.getRoleType() : 3);
            member.setJoinTime(LocalDateTime.now());
            sysSpaceMemberMapper.insert(member);
        }
    }

    @Override
    @Transactional
    public void removeMember(Long operatorId, Long spaceId, Long userId) {
        checkAdminPermission(spaceId, operatorId);

        SysSpace space = sysSpaceMapper.selectById(spaceId);
        if (space != null && space.getOwnerId().equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不能移除空间创建者");
        }

        sysSpaceMemberMapper.selectBySpaceId(spaceId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .ifPresent(m -> sysSpaceMemberMapper.deleteById(m.getId()));
    }

    @Override
    @Transactional
    public void changeRole(Long operatorId, MemberRoleUpdateRequest request) {
        checkAdminPermission(request.getSpaceId(), operatorId);
        SysSpace space = sysSpaceMapper.selectById(request.getSpaceId());
        if (space!=null && space.getOwnerId().equals(request.getUserId())) throw new BusinessException(400,"不能修改团队创建者的角色");

        sysSpaceMemberMapper.selectBySpaceId(request.getSpaceId()).stream()
                .filter(m -> m.getUserId().equals(request.getUserId()))
                .findFirst()
                .ifPresent(m -> {
                    m.setRoleType(request.getRoleType());
                    sysSpaceMemberMapper.updateById(m);
                });
    }

    @Override
    public List<SpaceMemberVO> listMembers(Long spaceId) {
        return sysSpaceMemberMapper.selectBySpaceId(spaceId).stream().map(m -> {
            SysUser user = sysUserMapper.selectById(m.getUserId());
            SpaceMemberVO vo = new SpaceMemberVO();
            vo.setUserId(m.getUserId());
            vo.setUserName(user != null ? (user.getRealName()!=null?user.getRealName():user.getUsername()) : "未知");
            vo.setAvatar(user != null ? user.getAvatar() : null);
            vo.setRoleType(m.getRoleType());
            vo.setJoinTime(m.getJoinTime());
            vo.setOnlineStatus(user != null ? user.getOnlineStatus() : 0);
            return vo;
        }).collect(Collectors.toList());
    }

    private void checkAdminPermission(Long spaceId, Long userId) {
        boolean isAdmin = sysSpaceMemberMapper.selectBySpaceId(spaceId).stream()
                .anyMatch(m -> m.getUserId().equals(userId) && m.getRoleType() == ROLE_ADMIN);
        if (!isAdmin) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "需要管理员权限");
        }
    }
}
