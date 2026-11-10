package com.collaboffice.doc.service;

import com.alibaba.fastjson2.*;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.*;
import java.time.Duration;

@Service @RequiredArgsConstructor
public class MindmapWorkspaceService {
    private final WorkspaceAccess access;
    private final MindmapNodeMapper nodes;
    private final MindmapMapMapper maps;
    private final MindmapService service;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    @Transactional
    public void properties(String mapId,Long nodeId,JSONObject body) {
        access.requireTypedDocument("mindmap",mapId,true);lock(mapId);
        var node=nodes.selectById(nodeId);if(node==null||!node.getMapId().equals(mapId))throw new BusinessException(404,"节点不存在");
        if(body.containsKey("expectedContent")&&(!Objects.equals(node.getContent(),body.getString("expectedContent"))||!SheetWorkbookService.format(node.getStyleJson()).equals(SheetWorkbookService.format(body.getString("expectedStyleJson")))))throw new BusinessException(409,"节点已被其他人修改，请重新打开属性");
        for(String field:List.of("Note","Link","Icon"))if(body.containsKey("expected"+field)){
            String actual=switch(field){case "Note"->node.getNote();case "Link"->node.getLink();default->node.getIcon();};
            if(!Objects.toString(actual,"").equals(Objects.toString(body.getString("expected"+field),"")))throw new BusinessException(409,"节点已被其他人修改，请重新打开属性");
        }
        if(body.containsKey("expectedParentId")&&!Objects.equals(node.getParentId(),body.getLong("expectedParentId")))throw new BusinessException(409,"节点上级已被修改，请重新打开属性");
        String style=body.getString("styleJson");var parsed=SheetWorkbookService.format(style);
        for(String color:List.of("color","background","borderColor","lineColor"))if(parsed.getString(color)!=null&&!parsed.getString(color).matches("#[0-9a-fA-F]{6}"))throw new BusinessException(400,"颜色须为HEX格式");
        if(parsed.containsKey("fontSize")&&(parsed.getIntValue("fontSize")<10||parsed.getIntValue("fontSize")>24))throw new BusinessException(400,"字号须为10到24");
        String note=Objects.toString(body.getString("note"),""),link=Objects.toString(body.getString("link"),""),icon=Objects.toString(body.getString("icon"),"");
        if(note.length()>4000||link.length()>500||icon.length()>50)throw new BusinessException(400,"节点备注、链接或图标过长");
        if(!link.isBlank()&&!link.matches("https?://[^\\s]+"))throw new BusinessException(400,"链接须为http或https");
        if(body.containsKey("parentId")&&!Objects.equals(node.getParentId(),body.getLong("parentId"))){
            if(body.getLong("parentId")==null)throw new BusinessException(400,"不能创建第二个根节点");
            service.moveNode(nodeId,body.getLong("parentId"),null);
        }
        service.updateNode(nodeId,body.getString("content"),style);
        node=nodes.selectById(nodeId);node.setNote(note);node.setLink(link);node.setIcon(icon);nodes.updateById(node);
    }
    @Transactional public void layout(String mapId,String layout) {
        access.requireTypedDocument("mindmap",mapId,true);if(!Set.of("mindmap","org","fishbone").contains(layout))throw new BusinessException(400,"不支持的布局");lock(mapId);
        var map=maps.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MindmapMap>().eq(MindmapMap::getMapId,mapId));map.setLayoutType(layout);maps.updateById(map);
    }
    private void lock(String mapId){jdbc.queryForObject("SELECT id FROM mindmap_map WHERE map_id=? AND deleted=0 FOR UPDATE",Long.class,mapId);}
    public Map<String,Object> presentation(String mapId) {
        access.requireTypedDocument("mindmap",mapId,false);String value=redis.opsForValue().get("mindmap:presentation:"+mapId);return value==null?Map.of("active",false):JSON.parseObject(value);
    }
    public Map<String,Object> control(String mapId,JSONObject body) {
        access.requireTypedDocument("mindmap",mapId,true);String key="mindmap:presentation:"+mapId,guard=key+":lock",token=UUID.randomUUID().toString();
        if(!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(guard,token,Duration.ofSeconds(5))))throw new BusinessException(409,"演示状态正在更新");
        try {
            String current=redis.opsForValue().get(key);JSONObject state=current==null?new JSONObject():JSON.parseObject(current);Long actor=access.userId();String action=body.getString("action");
            if(state.getBooleanValue("active")&&!Objects.equals(state.getLong("presenterId"),actor))throw new BusinessException(403,"只有演示者可控制演示");
            if("stop".equals(action)){state.put("active",false);redis.delete(key);return state;}
            var data=service.getMindmapData(mapId);List<String> ids=new ArrayList<>();collect((Map)data.get("rootNode"),ids);
            if("start".equals(action)){state.put("sessionId",UUID.randomUUID().toString());state.put("presenterId",actor);state.put("presenterName",jdbc.queryForObject("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,actor));state.put("step",0);state.put("active",true);}
            if(!state.getBooleanValue("active"))throw new BusinessException(400,"尚未开始演示");
            int step=state.getIntValue("step");switch(Objects.toString(action,"")){case "next"->step++;case "previous"->step--;case "goto"->step=body.getIntValue("step");case "annotate"-> {String note=body.getString("annotation");if(note!=null&&note.length()>1000)throw new BusinessException(400,"演示标注最多1000字");state.put("annotation",note);}case "start"->{}default->throw new BusinessException(400,"未知演示操作");}
            step=Math.max(0,Math.min(ids.size()-1,step));state.put("step",step);state.put("currentNodeId",ids.get(step));state.put("total",ids.size());state.put("mapId",mapId);redis.opsForValue().set(key,state.toJSONString(),Duration.ofHours(2));return state;
        }finally{redis.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>("if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end",Long.class),List.of(guard),token);}
    }
    @SuppressWarnings("unchecked") private void collect(Map<String,Object> node,List<String> ids){if(node==null)return;ids.add(node.get("id").toString());for(var child:(List<Map<String,Object>>)node.getOrDefault("children",List.of()))collect(child,ids);}
    public String pdf(String base64) throws Exception {
        if(base64==null||base64.length()>12_000_000)throw new BusinessException(400,"图片大小超过限制");byte[] png;try{png=Base64.getDecoder().decode(base64);}catch(Exception e){throw new BusinessException(400,"图片编码无效");}
        try(var input=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(png))) {
            var readers=javax.imageio.ImageIO.getImageReaders(input);if(!readers.hasNext())throw new BusinessException(400,"无效图片");var reader=readers.next();try{reader.setInput(input);if(!"png".equalsIgnoreCase(reader.getFormatName())||reader.getWidth(0)>4096||reader.getHeight(0)>4096)throw new BusinessException(400,"仅支持4096像素内PNG");}finally{reader.dispose();}
        }
        var out=new java.io.ByteArrayOutputStream();var doc=new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());com.lowagie.text.pdf.PdfWriter.getInstance(doc,out);doc.open();var image=com.lowagie.text.Image.getInstance(png);image.scaleToFit(doc.getPageSize().getWidth()-72,doc.getPageSize().getHeight()-72);doc.add(image);doc.close();return Base64.getEncoder().encodeToString(out.toByteArray());
    }
}
