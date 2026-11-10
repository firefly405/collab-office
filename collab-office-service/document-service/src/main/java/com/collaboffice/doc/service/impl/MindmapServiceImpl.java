package com.collaboffice.doc.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.doc.entity.MindmapMap;
import com.collaboffice.doc.entity.MindmapNode;
import com.collaboffice.doc.mapper.MindmapMapMapper;
import com.collaboffice.doc.mapper.MindmapNodeMapper;
import com.collaboffice.doc.service.MindmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapServiceImpl implements MindmapService {

    private final MindmapMapMapper mindmapMapMapper;
    private final MindmapNodeMapper mindmapNodeMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final com.collaboffice.doc.mapper.MindmapSnapshotMapper snapshots;

    @Override
    @Transactional
    public String createMindmap(Long userId, String title, Long spaceId) {
        spaceId=access.requireSpace(spaceId==null?access.currentSpace():spaceId,true);
        String mapId = SnowflakeIdGenerator.nextIdStr();
        MindmapMap map = new MindmapMap();
        map.setMapId(mapId);
        map.setTitle(title);
        map.setLayoutType("mindmap");
        map.setSpaceId(spaceId);
        map.setCreatorId(userId);
        map.setStatus(1);
        mindmapMapMapper.insert(map);

        MindmapNode rootNode = new MindmapNode();
        rootNode.setMapId(mapId);
        rootNode.setNodeKey(UUID.randomUUID().toString().substring(0, 8));
        rootNode.setContent(title);
        rootNode.setSortOrder(0);
        mindmapNodeMapper.insert(rootNode);

        return mapId;
    }

    @Override
    public Map<String, Object> getMindmapData(String mapId) {
        MindmapMap map = findByMapId(mapId);
        List<MindmapNode> nodes = mindmapNodeMapper.selectByMapId(mapId);
        validateTree(nodes);

        Map<Long, List<MindmapNode>> childrenMap = nodes.stream()
                .collect(Collectors.groupingBy(n -> n.getParentId() != null ? n.getParentId() : 0L));

        MindmapNode root = nodes.stream().filter(n -> n.getParentId() == null).findFirst().orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mapId", mapId);
        result.put("title", map.getTitle());
        result.put("layoutType", map.getLayoutType());
        result.put("themeJson",map.getThemeJson());
        result.put("rootNode", root != null ? buildNodeTree(root, childrenMap) : null);
        return result;
    }

    @Override
    @Transactional
    public void addNode(String mapId, Long parentId, String content) {
        lock(mapId);validateContent(content);
        var existing=mindmapNodeMapper.selectByMapId(mapId);if(existing.size()>=1000)throw new BusinessException(400,"单图最多1000节点");
        if(parentId==null&&!existing.isEmpty())throw new BusinessException(400,"思维导图只能有一个根节点");
        if(parentId!=null&&!mapId.equals(findByNodeId(parentId).getMapId()))throw new BusinessException(400,"上级节点不属于此导图");
        snapshot(mapId,"添加节点");
        MindmapNode node = new MindmapNode();
        node.setMapId(mapId);
        node.setParentId(parentId);
        node.setNodeKey(UUID.randomUUID().toString().substring(0, 8));
        node.setContent(content);
        node.setSortOrder(0);
        node.setId(-1L);var proposed=new ArrayList<>(existing);proposed.add(node);validateTree(proposed);node.setId(null);
        mindmapNodeMapper.insert(node);
    }

    @Override
    @Transactional
    public void updateNode(Long nodeId, String content, String styleJson) {
        MindmapNode node = findByNodeId(nodeId);
        lock(node.getMapId());node=findByNodeId(nodeId);if(content!=null)validateContent(content);if(styleJson!=null)com.collaboffice.doc.service.SheetWorkbookService.format(styleJson);snapshot(node.getMapId(),"编辑节点");
        if (content != null) node.setContent(content);
        if (styleJson != null) node.setStyleJson(styleJson);
        mindmapNodeMapper.updateById(node);
    }

    @Override
    @Transactional
    public void deleteNode(Long nodeId) {
        MindmapNode node = findByNodeId(nodeId);
        lock(node.getMapId());if(node.getParentId()==null)throw new BusinessException(400,"不能删除根节点");snapshot(node.getMapId(),"删除节点");
        deleteNodeRecursive(nodeId);
    }

    private void deleteNodeRecursive(Long nodeId) {
        List<MindmapNode> children = mindmapNodeMapper.selectChildrenByParentId(nodeId);
        for (MindmapNode child : children) {
            deleteNodeRecursive(child.getId());
        }
        mindmapNodeMapper.deleteById(nodeId);
    }

    @Override
    @Transactional
    public void moveNode(Long nodeId, Long targetParentId, Integer sortOrder) {
        MindmapNode node = findByNodeId(nodeId);
        lock(node.getMapId());node=findByNodeId(nodeId);
        if(node.getParentId()==null)throw new BusinessException(400,"不能移动根节点到其他节点下");
        var target=findByNodeId(targetParentId);if(!node.getMapId().equals(target.getMapId()))throw new BusinessException(400,"不能跨导图移动节点");
        var all=mindmapNodeMapper.selectByMapId(node.getMapId());var parents=new HashMap<Long,Long>();for(var n:all)parents.put(n.getId(),n.getParentId());
        Long current=targetParentId;Set<Long> visited=new HashSet<>();while(current!=null){if(current.equals(nodeId)||!visited.add(current))throw new BusinessException(400,"不能移动到自身或子节点内");current=parents.get(current);}
        snapshot(node.getMapId(),"移动节点");
        node.setParentId(targetParentId);
        for(var n:all)if(n.getId().equals(nodeId))n.setParentId(targetParentId);validateTree(all);
        if (sortOrder != null) node.setSortOrder(sortOrder);
        mindmapNodeMapper.updateById(node);
    }

    @Override
    @Transactional
    public void saveAll(String mapId, List<MindmapNode> nodes) {
        lock(mapId);validateTree(nodes);
        for(var node:nodes)if(node.getId()!=null){var stored=mindmapNodeMapper.selectById(node.getId());if(stored!=null&&!stored.getMapId().equals(mapId))throw new BusinessException(400,"快照包含其他导图节点");}
        snapshot(mapId,"恢复快照");
        List<MindmapNode> existing = mindmapNodeMapper.selectByMapId(mapId);
        Map<Long, MindmapNode> existingById = new HashMap<>();
        for (MindmapNode n : existing) {
            if (n.getId() != null) {
                existingById.put(n.getId(), n);
            }
        }

        Set<Long> incomingIds = new HashSet<>();
        for (MindmapNode node : nodes) {
            node.setMapId(mapId);
            if (node.getNodeKey() == null) {
                node.setNodeKey(UUID.randomUUID().toString().substring(0, 8));
            }

            if (node.getId() != null && existingById.containsKey(node.getId())) {
                incomingIds.add(node.getId());
                MindmapNode exist = existingById.get(node.getId());
                exist.setContent(node.getContent());
                exist.setParentId(node.getParentId());
                exist.setStyleJson(node.getStyleJson());
                exist.setIcon(node.getIcon());
                exist.setNote(node.getNote());
                exist.setLink(node.getLink());
                exist.setSortOrder(node.getSortOrder());
                if (exist.getNodeKey() == null) {
                    exist.setNodeKey(node.getNodeKey());
                }
                mindmapNodeMapper.updateById(exist);
            } else {
                mindmapNodeMapper.insert(node);
                if (node.getId() != null) {
                    incomingIds.add(node.getId());
                }
            }
        }

        // Delete nodes no longer present
        for (MindmapNode old : existing) {
            if (old.getId() != null && !incomingIds.contains(old.getId())) {
                mindmapNodeMapper.deleteById(old.getId());
            }
        }
    }

    private Map<String, Object> buildNodeTree(MindmapNode node, Map<Long, List<MindmapNode>> childrenMap) {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("id", node.getId());
        tree.put("nodeKey", node.getNodeKey());
        tree.put("content", node.getContent());
        tree.put("styleJson", node.getStyleJson());
        tree.put("icon", node.getIcon());
        tree.put("note", node.getNote());
        tree.put("link",node.getLink());tree.put("sortOrder",node.getSortOrder());tree.put("updateTime",node.getUpdateTime());

        List<MindmapNode> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
        tree.put("children", children.stream().map(c -> buildNodeTree(c, childrenMap)).collect(Collectors.toList()));
        return tree;
    }

    private MindmapMap findByMapId(String mapId) {
        MindmapMap map = mindmapMapMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MindmapMap>()
                        .eq(MindmapMap::getMapId, mapId));
        if (map == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return map;
    }

    private MindmapNode findByNodeId(Long id) {
        MindmapNode node = mindmapNodeMapper.selectById(id);
        if (node == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return node;
    }
    private void lock(String mapId){access.requireTypedDocument("mindmap",mapId,true);jdbc.queryForObject("SELECT id FROM mindmap_map WHERE map_id=? AND deleted=0 FOR UPDATE",Long.class,mapId);}
    private void validateContent(String content){if(content==null||content.isBlank()||content.length()>500)throw new BusinessException(400,"节点内容须为1到500字");}
    public static void validateTree(List<MindmapNode> nodes) {
        if(nodes==null||nodes.isEmpty()||nodes.size()>1000)throw new BusinessException(400,"导图须为1到1000个节点");
        Map<Long,MindmapNode> byId=new HashMap<>();int roots=0;
        for(var n:nodes){if(n.getId()==null||byId.put(n.getId(),n)!=null)throw new BusinessException(400,"节点ID缺失或重复");if(n.getParentId()==null)roots++;if(n.getContent()==null||n.getContent().isBlank()||n.getContent().length()>500)throw new BusinessException(400,"节点正文无效");}
        if(roots!=1)throw new BusinessException(400,"导图必须有一个根节点");
        for(var n:nodes){Set<Long> visited=new HashSet<>();var current=n;while(current!=null){if(!visited.add(current.getId())||visited.size()>50)throw new BusinessException(400,"节点形成循环或层级超过50层");Long parent=current.getParentId();if(parent!=null&&!byId.containsKey(parent))throw new BusinessException(400,"上级节点不存在");current=parent==null?null:byId.get(parent);}}
    }
    private void snapshot(String mapId,String label){var nodes=mindmapNodeMapper.selectByMapId(mapId);if(nodes.isEmpty())return;var snap=new com.collaboffice.doc.entity.MindmapSnapshot();snap.setMindmapId(mapId);snap.setLabel(label+" · 用户"+access.userId());snap.setSnapshotData(com.alibaba.fastjson2.JSON.toJSONString(nodes));snapshots.insert(snap);}
}
