package com.collaboffice.user.service;

import com.collaboffice.user.dto.SpaceCreateRequest;
import com.collaboffice.user.dto.SpaceUpdateRequest;
import com.collaboffice.user.vo.SpaceVO;

import java.util.List;

public interface SpaceService {

    SpaceVO createSpace(Long userId, SpaceCreateRequest request);

    SpaceVO updateSpace(Long spaceId, Long userId, SpaceUpdateRequest request);

    void deleteSpace(Long spaceId, Long userId);

    SpaceVO getSpaceInfo(Long spaceId);

    List<SpaceVO> listMySpaces(Long userId);
}
