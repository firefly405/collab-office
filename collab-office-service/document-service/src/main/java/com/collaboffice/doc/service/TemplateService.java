package com.collaboffice.doc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.DocumentCreateRequest;
import com.collaboffice.doc.entity.TemplateTemplate;
import com.collaboffice.doc.mapper.TemplateTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateTemplateMapper templates;
    private final DocumentService documents;
    private final DocumentContentService contents;
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public TemplateTemplate require(String id,boolean write) {
        var tpl=templates.selectOne(new LambdaQueryWrapper<TemplateTemplate>().eq(TemplateTemplate::getTemplateId,id));
        if(tpl==null) throw new BusinessException(404,"模板不存在");
        if(!Integer.valueOf(1).equals(tpl.getIsOfficial())) access.requireSpace(tpl.getSpaceId(),write);
        if(write && (!Objects.equals(tpl.getCreatorId(),access.userId()) || Integer.valueOf(1).equals(tpl.getIsOfficial())))
            throw new BusinessException(403,"只有创建者可管理自定义模板");
        return tpl;
    }

    @Transactional
    public String capture(String docId,String name,String category) {
        access.requireSpace(access.currentSpace(),true); access.requireDocument(docId,false);
        if(name==null||name.isBlank()||name.length()>100||category!=null&&category.length()>50) throw new BusinessException(400,"模板名称或分类无效");
        var doc=access.row("SELECT type,space_id FROM doc_document WHERE doc_id=? AND deleted=0",docId);
        if(!Objects.equals(((Number)doc.get("space_id")).longValue(),access.currentSpace())) throw new BusinessException(403,"请选择当前团队文档");
        int type=((Number)doc.get("type")).intValue();
        var snapshot=snapshot(docId,type);
        var tpl=new TemplateTemplate();
        tpl.setTemplateId(UUID.randomUUID().toString().replace("-","")); tpl.setName(name.trim());
        tpl.setType(type); tpl.setCategory(category); tpl.setSpaceId(access.currentSpace());
        tpl.setCreatorId(access.userId()); tpl.setIsOfficial(0); tpl.setStatus(1); tpl.setUsageCount(0);
        try { tpl.setContentJson(json.writeValueAsString(snapshot)); } catch(Exception e) { throw new BusinessException(400,"模板内容无效"); }
        templates.insert(tpl); return tpl.getTemplateId();
    }

    private Map<String,Object> snapshot(String docId,int type) {
        var snapshot=new LinkedHashMap<String,Object>();
        snapshot.put("documentContent",contents.getContent(docId).getContent());
        if(type==2) {
            var sheets=jdbc.queryForList("SELECT id,sheet_name,sheet_index,row_count,col_count,sort_order FROM sheet_sheet WHERE spreadsheet_id=? ORDER BY sheet_index",docId);
            for(var sheet:sheets) sheet.put("cells",jdbc.queryForList("SELECT cell_key,row_index,col_index,content,CAST(format_json AS CHAR) format_json,formula,result_value FROM sheet_cell WHERE sheet_id=?",sheet.get("id")));
            snapshot.put("sheets",sheets);
        } else if(type==3) {
            snapshot.put("nodes",jdbc.queryForList("SELECT id,parent_id,node_key,content,CAST(style_json AS CHAR) style_json,icon,note,link,sort_order FROM mindmap_node WHERE map_id=? ORDER BY id",docId));
            snapshot.put("mapMeta",jdbc.queryForMap("SELECT layout_type,CAST(theme_json AS CHAR) theme_json FROM mindmap_map WHERE map_id=?",docId));
        }
        else if(type==4) snapshot.put("elements",jdbc.queryForList("SELECT element_type,CAST(position_json AS CHAR) position_json,CAST(size_json AS CHAR) size_json,CAST(style_json AS CHAR) style_json,content,z_index FROM whiteboard_element WHERE board_id=?",docId));
        return snapshot;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String use(String id) {
        var tpl=require(id,false);
        Map<String,Object> data;
        try { data=json.readValue(tpl.getContentJson(),Map.class); } catch(Exception e) { throw new BusinessException(400,"模板内容无效"); }
        if(!data.containsKey("documentContent")) data.put("documentContent",tpl.getContentJson());
        String doc=instantiate(tpl.getName(),tpl.getType(),data);
        jdbc.update("UPDATE template_template SET usage_count=usage_count+1 WHERE id=?",tpl.getId());
        return doc;
    }

    @Transactional
    public String copyDocument(String id) {
        access.requireDocument(id,false);
        var doc=access.row("SELECT title,type,space_id FROM doc_document WHERE doc_id=? AND deleted=0 AND status=1",id);
        if(!Objects.equals(((Number)doc.get("space_id")).longValue(),access.currentSpace())) throw new BusinessException(403,"请选择当前团队文档");
        int type=((Number)doc.get("type")).intValue();
        return instantiate(Objects.toString(doc.get("title"))+"（副本）",type,snapshot(id,type));
    }

    @SuppressWarnings("unchecked")
    private String instantiate(String title,int type,Map<String,Object> data) {
        Long space=access.requireSpace(access.currentSpace(),true);
        var request=new DocumentCreateRequest(); request.setTitle(title); request.setType(type); request.setSpaceId(space);
        String name=jdbc.queryForObject("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,access.userId());
        String doc=documents.create(access.userId(),name,request);
        Object content=data.get("documentContent");
        try { contents.saveContent(doc,content==null?"{}":content instanceof String?(String)content:json.writeValueAsString(content),access.userId()); }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new BusinessException(400,"模板正文无效"); }
        if(type==2 && data.get("sheets") instanceof List<?> sheets && !sheets.isEmpty()) {
            jdbc.update("DELETE FROM sheet_sheet WHERE spreadsheet_id=?",doc);
            for(Object item:sheets) {
                Map<String,Object> sheet=(Map<String,Object>)item;
                long sheetId=insert("INSERT INTO sheet_sheet(spreadsheet_id,sheet_name,sheet_index,row_count,col_count,sort_order) VALUES(?,?,?,?,?,?)",doc,sheet.get("sheet_name"),sheet.get("sheet_index"),sheet.get("row_count"),sheet.get("col_count"),sheet.get("sort_order"));
                for(var cell:(List<Map<String,Object>>)sheet.getOrDefault("cells",List.of()))
                    jdbc.update("INSERT INTO sheet_cell(sheet_id,cell_key,row_index,col_index,content,format_json,formula,result_value) VALUES(?,?,?,?,?,?,?,?)",sheetId,cell.get("cell_key"),cell.get("row_index"),cell.get("col_index"),cell.get("content"),cell.get("format_json"),cell.get("formula"),cell.get("result_value"));
            }
        } else if(type==3 && data.get("nodes") instanceof List<?> nodes && !nodes.isEmpty()) {
            if(data.get("mapMeta") instanceof Map<?,?> meta)jdbc.update("UPDATE mindmap_map SET layout_type=?,theme_json=? WHERE map_id=?",meta.get("layout_type"),meta.get("theme_json"),doc);
            jdbc.update("DELETE FROM mindmap_node WHERE map_id=?",doc);
            Map<String,Long> ids=new HashMap<>();
            for(Object item:nodes) {
                Map<String,Object> n=(Map<String,Object>)item;
                long newId=insert("INSERT INTO mindmap_node(map_id,node_key,content,style_json,icon,note,link,sort_order) VALUES(?,?,?,?,?,?,?,?)",doc,UUID.randomUUID().toString().replace("-",""),n.get("content"),n.get("style_json"),n.get("icon"),n.get("note"),n.get("link"),n.get("sort_order"));
                ids.put(Objects.toString(n.get("id")),newId);
            }
            for(Object item:nodes) {
                Map<String,Object> n=(Map<String,Object>)item;
                jdbc.update("UPDATE mindmap_node SET parent_id=? WHERE id=?",ids.get(Objects.toString(n.get("parent_id"))),ids.get(Objects.toString(n.get("id"))));
            }
        } else if(type==4 && data.get("elements") instanceof List<?> elements) {
            for(Object item:elements) {
                Map<String,Object> n=(Map<String,Object>)item;
                jdbc.update("INSERT INTO whiteboard_element(board_id,element_type,position_json,size_json,style_json,content,z_index) VALUES(?,?,?,?,?,?,?)",doc,n.get("element_type"),n.get("position_json"),n.get("size_json"),n.get("style_json"),n.get("content"),n.get("z_index"));
            }
        }
        return doc;
    }

    private long insert(String sql,Object... args) {
        var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(connection->{var statement=connection.prepareStatement(sql,java.sql.Statement.RETURN_GENERATED_KEYS);for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]);return statement;},key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }
}
