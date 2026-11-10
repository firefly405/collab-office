package com.collaboffice.doc.service;

import com.collaboffice.doc.entity.MindmapNode;
import java.util.List;
import java.util.Map;

public interface MindmapService {
    String createMindmap(Long userId, String title, Long spaceId);
    Map<String, Object> getMindmapData(String mapId);
    void addNode(String mapId, Long parentId, String content);
    void updateNode(Long nodeId, String content, String styleJson);
    void deleteNode(Long nodeId);
    void moveNode(Long nodeId, Long targetParentId, Integer sortOrder);
    void saveAll(String mapId, List<MindmapNode> nodes);
}
