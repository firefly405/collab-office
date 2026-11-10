package com.collaboffice.user.service;

import com.collaboffice.user.dto.MemberInviteRequest;
import com.collaboffice.user.dto.MemberRoleUpdateRequest;
import com.collaboffice.user.vo.SpaceMemberVO;

import java.util.List;

public interface SpaceMemberService {

    void inviteMembers(Long inviterId, MemberInviteRequest request);

    void removeMember(Long operatorId, Long spaceId, Long userId);

    void changeRole(Long operatorId, MemberRoleUpdateRequest request);

    List<SpaceMemberVO> listMembers(Long spaceId);
}
