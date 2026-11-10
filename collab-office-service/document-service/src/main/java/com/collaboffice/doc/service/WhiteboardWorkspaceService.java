package com.collaboffice.doc.service;

import com.alibaba.fastjson2.*;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class WhiteboardWorkspaceService {
 private final JdbcTemplate jdbc;private final WorkspaceAccess access;
 public List<Map<String,Object>> operations(String board){access.requireTypedDocument("whiteboard",board,false);return jdbc.queryForList("SELECT id,element_type AS type,content,position_json AS position,style_json AS style,style_json AS metadata,create_time FROM whiteboard_element WHERE board_id=? ORDER BY z_index,id",board);}
 public void requireExport(String board){access.requireDocumentOperation(board,"download");}
 private void lock(String board){jdbc.queryForObject("SELECT id FROM whiteboard_board WHERE board_id=? AND deleted=0 FOR UPDATE",Long.class,board);}
 @Transactional public List<Map<String,Object>> append(String board,JSONObject input){
  access.requireTypedDocument("whiteboard",board,true);JSONObject op=validate(input);lock(board);
  var existing=operations(board);
  for(var row:existing)if("operation".equals(row.get("type"))){
   JSONObject old=JSON.parseObject(row.get("content").toString());
   if(op.getString("id").equals(old.getString("id"))){
    JSONObject meta=JSON.parseObject(row.get("metadata").toString());
    if(!Objects.equals(meta.getLong("actorId"),access.userId())||!validate(old).equals(op))throw new BusinessException(409,"操作标识冲突，请保留本机备份后重试");
    return existing;
   }
  }
  if(existing.size()>=2000)throw new BusinessException(400,"单板最多2000操作，请复制为新白板继续");
  String meta=JSON.toJSONString(Map.of("actorId",access.userId(),"active",true));
  jdbc.update("INSERT INTO whiteboard_element(board_id,element_type,position_json,style_json,content,z_index) VALUES(?,'operation','{}',?,?,?)",board,meta,op.toJSONString(),existing.size());
  jdbc.update("UPDATE whiteboard_board SET last_edit_time=NOW() WHERE board_id=?",board);return operations(board);
 }
 @Transactional public List<Map<String,Object>> active(String board,Long element,boolean active){
  access.requireTypedDocument("whiteboard",board,true);lock(board);
  var rows=jdbc.queryForList("SELECT style_json,element_type FROM whiteboard_element WHERE id=? AND board_id=?",element,board);if(rows.isEmpty())throw new BusinessException(404,"操作不存在");
  var row=rows.get(0);JSONObject meta=JSON.parseObject(Objects.toString(row.get("style_json"),"{}"));
  if(!"operation".equals(row.get("element_type"))||!Objects.equals(meta.getLong("actorId"),access.userId()))throw new BusinessException(403,"只能撤销自己的操作");
  meta.put("active",active);jdbc.update("UPDATE whiteboard_element SET style_json=? WHERE id=?",meta.toJSONString(),element);return operations(board);
 }
 public static JSONObject validate(JSONObject body){
  if(body==null)throw new BusinessException(400,"操作无效");
  String tool=body.getString("tool"),id=body.getString("id"),color=body.getString("color");
  if(id==null||!id.matches("[A-Za-z0-9_-]{1,64}")||tool==null||!Set.of("pen","eraser","rect","circle","triangle","arrow","line","text","clear").contains(tool))throw new BusinessException(400,"操作工具或标识无效");
  double width=body.getDoubleValue("width");if(color==null||!color.matches("#[0-9a-fA-F]{6}")||!Double.isFinite(width)||width<1||width>20)throw new BusinessException(400,"颜色或线宽无效");
  JSONObject safe=new JSONObject();safe.put("id",id);safe.put("tool",tool);safe.put("color",color);safe.put("width",width);
  for(String field:List.of("x","y","w","h")){double value=body.getDoubleValue(field);if(!Double.isFinite(value)||value<0||value>4096)throw new BusinessException(400,"画布坐标无效");safe.put(field,value);}
  if(Set.of("pen","eraser").contains(tool)){
   JSONArray points=body.getJSONArray("points");if(points==null||points.isEmpty()||points.size()>2000)throw new BusinessException(400,"单笔最多2000个点");
   for(int i=0;i<points.size();i++){JSONArray p=points.getJSONArray(i);if(p==null||p.size()!=2)throw new BusinessException(400,"画笔坐标无效");for(int j=0;j<2;j++){double v=p.getDoubleValue(j);if(!Double.isFinite(v)||v<0||v>4096)throw new BusinessException(400,"画笔坐标越界");}}
   safe.put("points",points);
  }
  if("text".equals(tool)){String text=body.getString("text");if(text==null||text.isBlank()||text.length()>500)throw new BusinessException(400,"文字为1到500字");safe.put("text",text);}
  if(safe.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>60000)throw new BusinessException(400,"单次绘制数据过大");return safe;
 }
}
