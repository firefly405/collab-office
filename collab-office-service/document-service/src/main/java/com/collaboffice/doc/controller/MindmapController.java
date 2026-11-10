package com.collaboffice.doc.controller;
import com.alibaba.fastjson2.*;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.MindmapSnapshotMapper;
import com.collaboffice.doc.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/mindmap") @RequiredArgsConstructor
public class MindmapController {
    private final MindmapService service;
    private final MindmapWorkspaceService workspace;
    private final MindmapSnapshotMapper snapshots;
    private final DocumentService documents;
    @PostMapping("/create") public CommonResult<String> create(@RequestHeader("X-User-Id")Long actor,@RequestParam String title,@RequestParam(required=false)Long spaceId){var req=new com.collaboffice.doc.dto.DocumentCreateRequest();req.setTitle(title);req.setSpaceId(spaceId);req.setType(3);return CommonResult.success(documents.create(actor,"",req));}
    @GetMapping("/{mapId}") public CommonResult<Map<String,Object>> get(@PathVariable String mapId){return CommonResult.success(service.getMindmapData(mapId));}
    @PostMapping("/{mapId}/node") public CommonResult<Void> add(@PathVariable String mapId,@RequestParam(required=false)Long parentId,@RequestParam String content){service.addNode(mapId,parentId,content);return CommonResult.success(null);}
    @PutMapping("/node/{nodeId}") public CommonResult<Void> update(@PathVariable Long nodeId,@RequestParam(required=false)String content,@RequestParam(required=false)String styleJson){service.updateNode(nodeId,content,styleJson);return CommonResult.success(null);}
    @DeleteMapping("/node/{nodeId}") public CommonResult<Void> delete(@PathVariable Long nodeId){service.deleteNode(nodeId);return CommonResult.success(null);}
    @PutMapping("/node/{nodeId}/move") public CommonResult<Void> move(@PathVariable Long nodeId,@RequestParam Long targetParentId,@RequestParam(required=false)Integer sortOrder){service.moveNode(nodeId,targetParentId,sortOrder);return CommonResult.success(null);}
    @PutMapping("/{mapId}/nodes/{nodeId}/properties") public CommonResult<Void> properties(@PathVariable String mapId,@PathVariable Long nodeId,@RequestBody JSONObject body){workspace.properties(mapId,nodeId,body);return CommonResult.success(null);}
    @PutMapping("/{mapId}/layout") public CommonResult<Void> layout(@PathVariable String mapId,@RequestParam String type){workspace.layout(mapId,type);return CommonResult.success(null);}
    @PutMapping("/{mapId}/save") public CommonResult<Void> save(@PathVariable String mapId,@RequestBody List<MindmapNode> nodes){service.saveAll(mapId,nodes);return CommonResult.success(null);}
    @GetMapping("/{mapId}/snapshots") public CommonResult<List<MindmapSnapshot>> snapshots(@PathVariable String mapId){var list=snapshots.selectByMindmapId(mapId).stream().limit(100).toList();list.forEach(s->s.setSnapshotData(null));return CommonResult.success(list);}
    @PostMapping("/{mapId}/snapshots/{snapshotId}/rollback") public CommonResult<Void> rollback(@PathVariable String mapId,@PathVariable Long snapshotId){var snap=snapshots.selectById(snapshotId);if(snap==null||!mapId.equals(snap.getMindmapId()))throw new BusinessException(404,"快照不存在");service.saveAll(mapId,JSON.parseArray(snap.getSnapshotData(),MindmapNode.class));return CommonResult.success(null);}
    @GetMapping("/{mapId}/presentation/state") public CommonResult<Map<String,Object>> state(@PathVariable String mapId){return CommonResult.success(workspace.presentation(mapId));}
    @PostMapping("/{mapId}/presentation/control") public CommonResult<Map<String,Object>> control(@PathVariable String mapId,@RequestBody JSONObject body){return CommonResult.success(workspace.control(mapId,body));}
    @PostMapping("/{mapId}/presentation/start") public CommonResult<Map<String,Object>> start(@PathVariable String mapId){return CommonResult.success(workspace.control(mapId,JSONObject.of("action","start")));}
    @PostMapping("/{mapId}/presentation/{sessionId}/next") public CommonResult<Map<String,Object>> next(@PathVariable String mapId,@PathVariable String sessionId){verifySession(mapId,sessionId);return CommonResult.success(workspace.control(mapId,JSONObject.of("action","next")));}
    @PostMapping("/{mapId}/presentation/{sessionId}/goto") public CommonResult<Map<String,Object>> go(@PathVariable String mapId,@PathVariable String sessionId,@RequestParam String nodeId){verifySession(mapId,sessionId);List<String> ids=new ArrayList<>();flatten((Map)service.getMindmapData(mapId).get("rootNode"),ids);int index=ids.indexOf(nodeId);if(index<0)throw new BusinessException(404,"节点不存在");return CommonResult.success(workspace.control(mapId,JSONObject.of("action","goto","step",index)));}
    private void verifySession(String mapId,String sessionId){if(!sessionId.equals(workspace.presentation(mapId).get("sessionId")))throw new BusinessException(404,"演示已结束");}
    @SuppressWarnings("unchecked") private void flatten(Map<String,Object> node,List<String> ids){ids.add(node.get("id").toString());for(var child:(List<Map<String,Object>>)node.getOrDefault("children",List.of()))flatten(child,ids);}
    @PostMapping("/{mapId}/export") public CommonResult<Map<String,String>> export(@PathVariable String mapId,@RequestBody JSONObject body) throws Exception{return CommonResult.success(Map.of("base64",workspace.pdf(body.getString("image"))));}
}
