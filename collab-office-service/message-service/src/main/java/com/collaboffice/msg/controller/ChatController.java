package com.collaboffice.msg.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.msg.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @PostMapping("/sessions")
    public CommonResult<String> createSession(@RequestBody Map<String,Object> body) {
        Long space = access.requireSpace(access.currentSpace(), true);
        String name = Objects.toString(body.get("name"), "").trim();
        int type = ((Number)body.getOrDefault("type",2)).intValue();
        if (name.isBlank() || name.length()>100 || (type!=1 && type!=2)) throw new com.collaboffice.common.exception.BusinessException(400,"请填写会话名称并选择会话类型");
        List<?> values = (List<?>)body.getOrDefault("memberIds",List.of());
        Set<Long> members = new LinkedHashSet<>();
        for (Object value : values) {
            Long id = Long.valueOf(value.toString());
            if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=?",Integer.class,space,id)==0) throw new com.collaboffice.common.exception.BusinessException(403,"只能选择当前团队成员");
            if (!id.equals(access.userId())) members.add(id);
        }
        if (members.isEmpty() || members.size()>100 || type==1 && members.size()!=1) throw new com.collaboffice.common.exception.BusinessException(400,"请选择有效的会话成员");
        String id=chatService.createSession(access.userId(),type,name,new ArrayList<>(members));
        jdbc.update("UPDATE chat_session SET space_id=? WHERE session_id=?",space,id);
        return CommonResult.success(id);
    }

    @GetMapping("/conversations")
    public CommonResult<List<Map<String, Object>>> getConversations(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(chatService.getConversations(userId));
    }

    @GetMapping("/messages/{sessionId}")
    public CommonResult<List<Map<String, Object>>> getMessages(@PathVariable String sessionId,
                                                                @RequestParam(defaultValue = "1") int page,
                                                                @RequestParam(defaultValue = "50") int size) {
        return CommonResult.success(chatService.getMessages(sessionId, page, size));
    }

    @PostMapping("/send")
    public CommonResult<Void> sendMessage(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam String sessionId,
                                          @RequestParam String content,
                                          @RequestParam(defaultValue = "1") Integer msgType) {
        chatService.sendMessage(sessionId, userId, content, msgType);
        return CommonResult.success(null);
    }

    @PostMapping("/recall/{msgId}")
    public CommonResult<Void> recallMessage(@RequestHeader("X-User-Id") Long userId, @PathVariable String msgId) {
        chatService.recallMessage(userId, msgId);
        return CommonResult.success(null);
    }

    @PostMapping("/read/{sessionId}")
    public CommonResult<Void> markRead(@RequestHeader("X-User-Id") Long userId, @PathVariable String sessionId) {
        chatService.markRead(userId, sessionId);
        return CommonResult.success(null);
    }

    @GetMapping("/search")
    public CommonResult<List<Map<String, Object>>> searchMessages(@RequestHeader("X-User-Id") Long userId,
                                                                   @RequestParam String keyword) {
        return CommonResult.success(chatService.searchMessages(userId, keyword));
    }

    @GetMapping("/internal/offline")
    public CommonResult<List<Map<String, Object>>> getOfflineMessages(
            @RequestParam Long userId,
            @RequestParam Long lastSeqId) {
        return CommonResult.success(chatService.getOfflineMessages(userId, lastSeqId));
    }

    @GetMapping("/comments")
    public CommonResult<List<Map<String, Object>>> getComments(
            @RequestParam String sourceType,
            @RequestParam String sourceId) {
        return CommonResult.success(chatService.getComments(sourceType, sourceId));
    }

    @PostMapping("/comments")
    public CommonResult<Void> sendComment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam String content,
            @RequestParam(defaultValue = "1") Integer msgType,
            @RequestParam String sourceType,
            @RequestParam String sourceId) {
        chatService.sendMessage(sessionId, userId, content, msgType, sourceType, sourceId);
        return CommonResult.success(null);
    }
}
