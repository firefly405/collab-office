package com.collaboffice.user.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.user.dto.SpaceCreateRequest;
import com.collaboffice.user.dto.SpaceUpdateRequest;
import com.collaboffice.user.entity.SysSpace;
import com.collaboffice.user.entity.SysSpaceMember;
import com.collaboffice.user.mapper.SysSpaceMapper;
import com.collaboffice.user.mapper.SysSpaceMemberMapper;
import com.collaboffice.user.service.SpaceService;
import com.collaboffice.user.vo.SpaceVO;
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
public class SpaceServiceImpl implements SpaceService {

    private final SysSpaceMapper sysSpaceMapper;
    private final SysSpaceMemberMapper sysSpaceMemberMapper;

    private static final int ROLE_ADMIN = 1;
    private static final long DEFAULT_STORAGE_QUOTA = 10L * 1024 * 1024 * 1024; // 10GB

    @Override
    @Transactional
    public SpaceVO createSpace(Long userId, SpaceCreateRequest request) {
        SysSpace space = new SysSpace();
        space.setSpaceName(request.getSpaceName());
        space.setSpaceLogo(request.getSpaceLogo());
        space.setOwnerId(userId);
        space.setStorageQuota(DEFAULT_STORAGE_QUOTA);
        space.setStorageUsed(0L);
        space.setStatus(1);
        sysSpaceMapper.insert(space);

        // 创建者自动成为管理员
        SysSpaceMember member = new SysSpaceMember();
        member.setSpaceId(space.getId());
        member.setUserId(userId);
        member.setRoleType(ROLE_ADMIN);
        member.setJoinTime(LocalDateTime.now());
        sysSpaceMemberMapper.insert(member);

        return toSpaceVO(space, 1);
    }

    @Override
    public SpaceVO updateSpace(Long spaceId, Long userId, SpaceUpdateRequest request) {
        SysSpaceMember member = findMemberOrFail(spaceId, userId);
        if (member.getRoleType() != ROLE_ADMIN) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        SysSpace space = sysSpaceMapper.selectById(spaceId);
        if (space == null) throw new BusinessException(ResultCode.NOT_FOUND);

        if (request.getSpaceName() != null) space.setSpaceName(request.getSpaceName());
        if (request.getSpaceLogo() != null) space.setSpaceLogo(request.getSpaceLogo());
        if (request.getStorageQuota() != null) space.setStorageQuota(request.getStorageQuota());
        sysSpaceMapper.updateById(space);

        int memberCount = sysSpaceMemberMapper.countMembers(spaceId);
        return toSpaceVO(space, memberCount);
    }

    @Override
    @Transactional
    public void deleteSpace(Long spaceId, Long userId) {
        SysSpace space = sysSpaceMapper.selectById(spaceId);
        if (space == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!space.getOwnerId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        space.setStatus(0);
        sysSpaceMapper.updateById(space);
    }

    @Override
    public SpaceVO getSpaceInfo(Long spaceId) {
        SysSpace space = sysSpaceMapper.selectById(spaceId);
        if (space == null) throw new BusinessException(ResultCode.NOT_FOUND);
        int memberCount = sysSpaceMemberMapper.countMembers(spaceId);
        return toSpaceVO(space, memberCount);
    }

    @Override
    public List<SpaceVO> listMySpaces(Long userId) {
        List<SysSpaceMember> memberships = sysSpaceMemberMapper.selectByUserId(userId);
        return memberships.stream().filter(m->{var s=sysSpaceMapper.selectById(m.getSpaceId());return s!=null&&Integer.valueOf(1).equals(s.getStatus());}).map(m -> {
            SysSpace space = sysSpaceMapper.selectById(m.getSpaceId());
            int count = sysSpaceMemberMapper.countMembers(m.getSpaceId());
            return toSpaceVO(space, count);
        }).collect(Collectors.toList());
    }

    private SysSpaceMember findMemberOrFail(Long spaceId, Long userId) {
        return sysSpaceMemberMapper.selectBySpaceId(spaceId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.FORBIDDEN.getCode(), "不是空间成员"));
    }

    private SpaceVO toSpaceVO(SysSpace space, int memberCount) {
        SpaceVO vo = new SpaceVO();
        vo.setSpaceId(space.getId());
        vo.setSpaceName(space.getSpaceName());
        vo.setSpaceLogo(space.getSpaceLogo());
        vo.setOwnerId(space.getOwnerId());
        vo.setMemberCount(memberCount);
        vo.setStorageQuota(space.getStorageQuota());
        vo.setStorageUsed(space.getStorageUsed());
        vo.setStatus(space.getStatus());
        vo.setCreateTime(space.getCreateTime());
        return vo;
    }
}
