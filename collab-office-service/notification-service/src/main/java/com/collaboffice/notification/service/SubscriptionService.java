package com.collaboffice.notification.service;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service @RequiredArgsConstructor
public class SubscriptionService {
    private final JdbcTemplate jdbc;
    private final WorkspaceAccess access;
    private final NotificationService notifications;
    public List<Map<String,Object>> list(){Long actor=access.userId(),space=access.currentSpace();var rows=jdbc.queryForList("SELECT id,kind,resource_id AS resourceId,frequency,updated_at AS updatedAt FROM user_subscription WHERE user_id=? AND space_id=? ORDER BY id DESC",actor,space);return rows.stream().filter(r->allowed(r.get("kind").toString(),r.get("resourceId").toString(),actor,space)).map(r->{r.put("title",title(r.get("kind").toString(),r.get("resourceId").toString()));return r;}).toList();}
    public void save(Map<String,String> body){String kind=body.get("kind"),id=body.get("resourceId"),frequency=body.getOrDefault("frequency","realtime");validate(kind,id,frequency);Long actor=access.userId(),space=access.currentSpace();if(!allowed(kind,id,actor,space))throw new BusinessException(403,"没有订阅此资源的权限");jdbc.update("INSERT INTO user_subscription(user_id,space_id,kind,resource_id,frequency,last_signature) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE frequency=VALUES(frequency)",actor,space,kind,id,frequency,signature(kind,id));}
    public void remove(Long id){jdbc.update("DELETE FROM user_subscription WHERE id=? AND user_id=?",id,access.userId());}
    public static void validate(String kind,String id,String frequency){if(kind==null||!Set.of("document","project","task").contains(kind)||id==null||!id.matches("[A-Za-z0-9_-]{1,32}")||!Set.of("realtime","daily").contains(frequency))throw new BusinessException(400,"订阅类型、资源或频率无效");}
    boolean allowed(String kind,String id,Long actor,Long space){
        if(jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member m JOIN sys_space s ON s.id=m.space_id WHERE m.user_id=? AND m.space_id=? AND s.status=1 AND s.deleted=0",Integer.class,actor,space)==0)return false;
        if(kind.equals("document")){if(jdbc.queryForObject("SELECT COUNT(*) FROM doc_document WHERE doc_id=? AND space_id=? AND status=1 AND deleted=0",Integer.class,id,space)==0)return false;try{access.requireDocument(id,actor,false);return true;}catch(BusinessException e){return false;}}
        return jdbc.queryForObject(kind.equals("task")?"SELECT COUNT(*) FROM task_task t JOIN task_project p ON p.project_id=t.project_id WHERE t.task_id=? AND p.space_id=? AND p.deleted=0 AND t.deleted=0":"SELECT COUNT(*) FROM task_project WHERE project_id=? AND space_id=? AND deleted=0",Integer.class,id,space)>0;
    }
    private String title(String kind,String id){var list=jdbc.queryForList(switch(kind){case "document"->"SELECT title FROM doc_document WHERE doc_id=?";case "task"->"SELECT title FROM task_task WHERE task_id=?";default->"SELECT name FROM task_project WHERE project_id=?";},String.class,id);return list.isEmpty()?id:list.get(0);}
    private String signature(String kind,String id){
        var content=new ArrayList<Object>();content.add(jdbc.queryForList(switch(kind){case "document"->"SELECT title,current_version,status,tags FROM doc_document WHERE doc_id=?";case "task"->"SELECT * FROM task_task WHERE task_id=?";default->"SELECT * FROM task_project WHERE project_id=?";},id));
        if(kind.equals("project"))content.add(jdbc.queryForList("SELECT * FROM task_task WHERE project_id=? ORDER BY id",id));
        if(kind.equals("document")){content.add(jdbc.queryForList("SELECT sc.* FROM sheet_cell sc JOIN sheet_sheet ss ON ss.id=sc.sheet_id WHERE ss.spreadsheet_id=? ORDER BY sc.id",id));content.add(jdbc.queryForList("SELECT * FROM mindmap_node WHERE map_id=? ORDER BY id",id));}
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.toJSONBytes(content)));}catch(Exception e){throw new IllegalStateException(e);}
    }
    @Scheduled(fixedDelay=60000) public void poll(){for(var row:jdbc.queryForList("SELECT * FROM user_subscription ORDER BY id LIMIT 2000"))try{
        String kind=row.get("kind").toString(),id=row.get("resource_id").toString();Long actor=((Number)row.get("user_id")).longValue(),space=((Number)row.get("space_id")).longValue();if(!allowed(kind,id,actor,space))continue;String signature=signature(kind,id);if(signature.equals(row.get("last_signature")))continue;
        if(row.get("frequency").equals("realtime")){notifications.sendNotification(actor,kind.equals("document")?2:3,"订阅动态 · "+title(kind,id),"你订阅的内容已更新",kind.equals("project")?null:id);jdbc.update("UPDATE user_subscription SET last_signature=?,pending=0 WHERE id=?",signature,row.get("id"));}
        else jdbc.update("UPDATE user_subscription SET last_signature=?,pending=1 WHERE id=?",signature,row.get("id"));
    }catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Subscription {} update failed",row.get("id"),e);}}
    @Scheduled(cron="0 0 17 * * *",zone="Asia/Shanghai") public void digest(){
        var groups=new LinkedHashMap<Long,List<Map<String,Object>>>();for(var row:jdbc.queryForList("SELECT * FROM user_subscription WHERE frequency='daily' AND pending=1 ORDER BY id")){Long actor=((Number)row.get("user_id")).longValue();if(allowed(row.get("kind").toString(),row.get("resource_id").toString(),actor,((Number)row.get("space_id")).longValue()))groups.computeIfAbsent(actor,k->new ArrayList<>()).add(row);}
        groups.forEach((actor,rows)->{notifications.sendNotification(actor,2,"每日订阅汇总",String.join("、",rows.stream().map(r->title(r.get("kind").toString(),r.get("resource_id").toString())).toList()),null);for(var r:rows)jdbc.update("UPDATE user_subscription SET pending=0 WHERE id=?",r.get("id"));});
    }
}
