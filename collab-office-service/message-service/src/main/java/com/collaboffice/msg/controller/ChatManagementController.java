package com.collaboffice.msg.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.msg.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatManagementController {
    private final JdbcTemplate jdbc;
    private final WorkspaceAccess access;
    private final ChatService chat;

    @PostMapping("/compose")
    public CommonResult<Void> compose(@RequestBody Map<String,String> body) {
        access.requireSession(body.get("sessionId"));
        chat.sendReply(body.get("sessionId"),access.userId(),body.get("content"),Integer.parseInt(body.getOrDefault("msgType","1")),body.get("replyToId"));
        return CommonResult.success(null);
    }
    @GetMapping("/sessions/{id}")
    public CommonResult<Map<String,Object>> detail(@PathVariable String id) {
        access.requireSession(id);
        var session=access.row("SELECT session_name AS name,session_avatar AS avatar,announcement,space_id AS spaceId FROM chat_session WHERE session_id=? AND status=1",id);
        session.put("members",jdbc.queryForList("SELECT m.user_id AS userId,COALESCE(u.real_name,u.username) AS name,m.role FROM chat_session_member m LEFT JOIN sys_user u ON u.id=m.user_id WHERE m.session_id=?",id));
        session.put("files",jdbc.queryForList("SELECT msg_id AS msgId,content,send_time AS sendTime FROM chat_message WHERE session_id=? AND msg_type=3 AND status=1 AND COALESCE(is_recalled,0)=0 ORDER BY send_time DESC LIMIT 100",id));
        session.put("pins",jdbc.queryForList("SELECT m.msg_id AS msgId,m.content FROM chat_pinned_message p JOIN chat_message m ON BINARY m.msg_id=BINARY p.msg_id WHERE p.session_id=? AND COALESCE(m.is_recalled,0)=0 ORDER BY p.pinned_at DESC",id));
        return CommonResult.success(session);
    }
    @PutMapping("/pin/{msgId}")
    public CommonResult<Void> pin(@PathVariable String msgId,@RequestParam boolean value) {
        var message=access.row("SELECT session_id FROM chat_message WHERE msg_id=? AND status=1 AND COALESCE(is_recalled,0)=0",msgId);
        String session=Objects.toString(message.get("session_id")); requireManager(session);
        if(value) jdbc.update("INSERT IGNORE INTO chat_pinned_message(session_id,msg_id,pinned_by) VALUES(?,?,?)",session,msgId,access.userId());
        else jdbc.update("DELETE FROM chat_pinned_message WHERE session_id=? AND msg_id=?",session,msgId);
        return CommonResult.success(null);
    }
    @DeleteMapping("/sessions/{id}")
    public CommonResult<Void> dissolve(@PathVariable String id) {
        requireOwner(id);
        jdbc.update("UPDATE chat_session SET status=0 WHERE session_id=?",id);
        return CommonResult.success(null);
    }
    @PutMapping("/sessions/{id}/preferences")
    public CommonResult<Void> preferences(@PathVariable String id,@RequestBody Map<String,Object> body) {
        access.requireSession(id);
        if(body.containsKey("pinned")) jdbc.update("UPDATE chat_session_member SET is_pinned=? WHERE session_id=? AND user_id=?",Boolean.TRUE.equals(body.get("pinned"))?1:0,id,access.userId());
        if(body.containsKey("muted")) jdbc.update("UPDATE chat_session_member SET is_muted=? WHERE session_id=? AND user_id=?",Boolean.TRUE.equals(body.get("muted"))?1:0,id,access.userId());
        if(body.containsKey("draft")) jdbc.update("UPDATE chat_session_member SET draft=? WHERE session_id=? AND user_id=?",text(body,"draft",10000,false),id,access.userId());
        if(body.containsKey("groupName")) jdbc.update("UPDATE chat_session_member SET group_name=? WHERE session_id=? AND user_id=?",text(body,"groupName",50,false),id,access.userId());
        return CommonResult.success(null);
    }
    @PutMapping("/sessions/{id}")
    public CommonResult<Void> update(@PathVariable String id,@RequestBody Map<String,Object> body) {
        requireManager(id);
        if(body.containsKey("name")) jdbc.update("UPDATE chat_session SET session_name=? WHERE session_id=?",text(body,"name",100,true),id);
        if(body.containsKey("announcement")) jdbc.update("UPDATE chat_session SET announcement=? WHERE session_id=?",text(body,"announcement",5000,false),id);
        if(body.containsKey("avatar")) {
            String avatar=text(body,"avatar",255,false);
            if(!avatar.isBlank()&&!avatar.matches("/api/v1/file/preview/\\d+")) throw new BusinessException(400,"请选择上传的群头像");
            jdbc.update("UPDATE chat_session SET session_avatar=? WHERE session_id=?",avatar,id);
        }
        return CommonResult.success(null);
    }
    @PostMapping("/sessions/{id}/members")
    @Transactional
    public CommonResult<Void> addMember(@PathVariable String id,@RequestBody Map<String,String> body) {
        requireManager(id);
        var session=access.row("SELECT space_id FROM chat_session WHERE session_id=?",id);
        if(session.get("space_id")==null) throw new BusinessException(400,"历史会话未绑定团队，请新建团队群聊");
        Long member=Long.valueOf(body.get("userId"));
        if(jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=?",Integer.class,session.get("space_id"),member)==0) throw new BusinessException(403,"只能邀请群所属团队成员");
        jdbc.update("INSERT IGNORE INTO chat_session_member(session_id,user_id,role,joined_time) VALUES(?,?,'MEMBER',NOW())",id,member);
        return CommonResult.success(null);
    }
    @PutMapping("/sessions/{id}/members/{member}")
    public CommonResult<Void> role(@PathVariable String id,@PathVariable Long member,@RequestBody Map<String,String> body) {
        requireOwner(id);
        String role=body.get("role");
        if(!List.of("ADMIN","MEMBER").contains(Objects.toString(role,""))) throw new BusinessException(400,"角色无效");
        jdbc.update("UPDATE chat_session_member SET role=? WHERE session_id=? AND user_id=? AND role<>'OWNER'",role,id,member);
        return CommonResult.success(null);
    }
    @DeleteMapping("/sessions/{id}/members/{member}")
    public CommonResult<Void> removeMember(@PathVariable String id,@PathVariable Long member) {
        if(member.equals(access.userId())) {
            access.requireSession(id);
            if("OWNER".equals(role(id))) throw new BusinessException(400,"群主请先转让群组");
        } else requireManager(id);
        var targets=jdbc.queryForList("SELECT role FROM chat_session_member WHERE session_id=? AND user_id=?",id,member);
        if(targets.isEmpty()) throw new BusinessException(404,"成员不在群内");
        var target=targets.get(0);
        if("OWNER".equals(target.get("role"))||!member.equals(access.userId())&&"ADMIN".equals(target.get("role"))&&!"OWNER".equals(role(id))) throw new BusinessException(403,"不能移除群主或同级管理员");
        jdbc.update("DELETE FROM chat_session_member WHERE session_id=? AND user_id=?",id,member);
        return CommonResult.success(null);
    }
    @PutMapping("/sessions/{id}/owner/{member}")
    @Transactional
    public CommonResult<Void> transfer(@PathVariable String id,@PathVariable Long member) {
        requireOwner(id);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM chat_session_member WHERE session_id=? AND user_id=?",Integer.class,id,member)==0) throw new BusinessException(400,"新群主必须是群成员");
        jdbc.update("UPDATE chat_session_member SET role='MEMBER' WHERE session_id=? AND role='OWNER'",id);
        jdbc.update("UPDATE chat_session_member SET role='OWNER' WHERE session_id=? AND user_id=?",id,member);
        return CommonResult.success(null);
    }
    @PutMapping("/edit/{msgId}")
    public CommonResult<Void> edit(@PathVariable String msgId,@RequestBody Map<String,Object> body) {
        var message=access.row("SELECT session_id,sender_id,msg_type,is_recalled FROM chat_message WHERE msg_id=? AND status=1",msgId);
        access.requireSession(Objects.toString(message.get("session_id")));
        if(!Objects.equals(((Number)message.get("sender_id")).longValue(),access.userId())||((Number)message.get("msg_type")).intValue()!=1||message.get("is_recalled")!=null&&((Number)message.get("is_recalled")).intValue()==1) throw new BusinessException(403,"只能编辑自己未撤回的文字消息");
        String content=text(body,"content",10000,true);
        jdbc.update("UPDATE chat_message SET content=?,is_edited=1 WHERE msg_id=?",content,msgId);
        return CommonResult.success(null);
    }
    @PostMapping("/forward/{msgId}")
    public CommonResult<Void> forward(@PathVariable String msgId,@RequestBody Map<String,String> body) {
        var message=access.row("SELECT session_id,content,msg_type,is_recalled FROM chat_message WHERE msg_id=? AND status=1",msgId);
        access.requireSession(Objects.toString(message.get("session_id"))); access.requireSession(body.get("sessionId"));
        if(message.get("is_recalled")!=null&&((Number)message.get("is_recalled")).intValue()==1) throw new BusinessException(400,"不能转发已撤回消息");
        chat.sendMessage(body.get("sessionId"),access.userId(),Objects.toString(message.get("content")),((Number)message.get("msg_type")).intValue());
        return CommonResult.success(null);
    }
    private String role(String id) { return jdbc.queryForObject("SELECT role FROM chat_session_member WHERE session_id=? AND user_id=?",String.class,id,access.userId()); }
    private void requireManager(String id) {
        access.requireSession(id);
        if(jdbc.queryForObject("SELECT session_type FROM chat_session WHERE session_id=?",Integer.class,id)!=2) throw new BusinessException(400,"仅群聊支持群管理");
        if(!List.of("OWNER","ADMIN").contains(role(id))) throw new BusinessException(403,"需要群管理员权限");
    }
    private void requireOwner(String id) { access.requireSession(id); if(!"OWNER".equals(role(id))) throw new BusinessException(403,"需要群主权限"); }
    private String text(Map<String,?> body,String key,int max,boolean required) {
        String value=Objects.toString(body.get(key),"").trim();
        if(value.length()>max||required&&value.isBlank()) throw new BusinessException(400,"内容为空或超出长度限制"); return value;
    }
}
