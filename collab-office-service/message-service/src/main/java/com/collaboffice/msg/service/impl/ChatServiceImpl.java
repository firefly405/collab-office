package com.collaboffice.msg.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.msg.entity.*;
import com.collaboffice.msg.mapper.*;
import com.collaboffice.msg.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionMemberMapper sessionMemberMapper;
    private final RabbitTemplate rabbitTemplate;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.collaboffice.common.security.WorkspaceAccess access;

    private static final String CHAT_EXCHANGE = "chat-message-exchange";
    private static final String CHAT_BROADCAST_KEY = "chat.message.broadcast";

    @Override
    @Transactional
    public String createSession(Long userId, Integer type, String name, List<Long> memberIds) {
        ChatSession session = new ChatSession();
        session.setSessionId(SnowflakeIdGenerator.nextIdStr());
        session.setSessionType(type != null ? type : 1);
        session.setSessionName(name);
        session.setCreatorId(userId);
        session.setStatus(1);
        sessionMapper.insert(session);

        ChatSessionMember owner = new ChatSessionMember();
        owner.setSessionId(session.getSessionId());
        owner.setUserId(userId);
        owner.setRole("OWNER");
        owner.setJoinedTime(LocalDateTime.now());
        sessionMemberMapper.insert(owner);

        if (memberIds != null) {
            for (Long memberId : memberIds) {
                if (memberId.equals(userId)) continue;
                ChatSessionMember member = new ChatSessionMember();
                member.setSessionId(session.getSessionId());
                member.setUserId(memberId);
                member.setRole("MEMBER");
                member.setJoinedTime(LocalDateTime.now());
                sessionMemberMapper.insert(member);
            }
        }
        return session.getSessionId();
    }

    @Override
    public List<Map<String, Object>> getConversations(Long userId) {
        List<ChatSession> sessions = sessionMapper.selectByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession s : sessions) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", s.getSessionId());
            map.put("name", s.getSessionName());
            map.put("type", s.getSessionType());
            map.put("lastMsg", s.getLastMsgContent());
            map.put("lastMsgTime", s.getLastMsgTime());
            map.put("unreadCount", sessionMemberMapper.selectBySessionId(s.getSessionId()).stream().filter(m -> m.getUserId().equals(userId)).map(m -> m.getUnreadCount()==null?0:m.getUnreadCount()).findFirst().orElse(0));
            map.putAll(jdbc.queryForMap("SELECT is_pinned AS pinned,is_muted AS muted,draft,group_name AS groupName,role FROM chat_session_member WHERE session_id=? AND user_id=?",s.getSessionId(),userId));
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getMessages(String sessionId, int page, int size) {
        size = Math.max(1,Math.min(size,500));
        access.requireSession(sessionId);
        if(jdbc.queryForObject("SELECT COUNT(*) FROM chat_session WHERE session_id=? AND status=1",Integer.class,sessionId)==0) throw new com.collaboffice.common.exception.BusinessException(404,"会话已解散");
        List<ChatMessage> messages = messageMapper.selectPageHistory(sessionId,size,(Math.max(1,Math.min(10000,page))-1)*size);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("msgId", m.getMsgId());
            map.put("senderId", m.getSenderId());
            map.put("senderName", senderName(m.getSenderId()));
            map.put("content", Integer.valueOf(1).equals(m.getIsRecalled())?"此消息已撤回":m.getContent());
            map.put("isRecalled", m.getIsRecalled()); map.put("isEdited",m.getIsEdited());
            map.put("replyToId",m.getReplyToId());
            if(m.getReplyToId()!=null) {
                var quoted=messageMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getMsgId,m.getReplyToId()).eq(ChatMessage::getSessionId,sessionId));
                if(quoted!=null) map.put("quote",Integer.valueOf(1).equals(quoted.getIsRecalled())?"此消息已撤回":quoted.getContent());
            }
            map.put("readCount",jdbc.queryForObject("SELECT COUNT(*) FROM chat_session_member WHERE session_id=? AND user_id<>? AND last_read_time>=?",Integer.class,sessionId,m.getSenderId(),m.getSendTime()));
            map.put("msgType", m.getMsgType());
            map.put("sendTime", m.getSendTime());
            result.add(map);
        }
        return result;
    }

    @Override
    @Transactional
    public void sendMessage(String sessionId, Long senderId, String content, Integer msgType) {
        sendMessage(sessionId, senderId, content, msgType, null, null);
    }

    @Override
    @Transactional
    public void sendMessage(String sessionId, Long senderId, String content, Integer msgType, String sourceType, String sourceId) {
        send(sessionId,senderId,content,msgType,sourceType,sourceId,null);
    }

    @Override
    @Transactional
    public void sendReply(String sessionId,Long senderId,String content,Integer msgType,String replyToId) {
        access.requireSession(sessionId);
        if(replyToId!=null && jdbc.queryForObject("SELECT COUNT(*) FROM chat_message WHERE msg_id=? AND session_id=? AND status=1 AND COALESCE(is_recalled,0)=0",Integer.class,replyToId,sessionId)==0)
            throw new com.collaboffice.common.exception.BusinessException(400,"引用消息不存在或已撤回");
        send(sessionId,senderId,content,msgType,null,null,replyToId);
    }

    private void send(String sessionId,Long senderId,String content,Integer msgType,String sourceType,String sourceId,String replyToId) {
        if(sessionId!=null && jdbc.queryForObject("SELECT COUNT(*) FROM chat_session WHERE session_id=? AND status=1",Integer.class,sessionId)==0) throw new com.collaboffice.common.exception.BusinessException(404,"会话已解散");
        if(content==null || content.isBlank() || content.length()>10000 || msgType==null || msgType!=1 && msgType!=3) throw new com.collaboffice.common.exception.BusinessException(400,"消息内容或类型无效");
        ChatMessage msg = new ChatMessage();
        msg.setMsgId(SnowflakeIdGenerator.nextIdStr());
        msg.setSessionId(sessionId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setMsgType(msgType != null ? msgType : 1);
        msg.setSendTime(LocalDateTime.now());
        msg.setStatus(1);
        msg.setSourceType(sourceType);
        msg.setSourceId(sourceId);
        msg.setReplyToId(replyToId);
        messageMapper.insert(msg);
        if (sessionId != null) jdbc.update("UPDATE chat_session_member SET unread_count=COALESCE(unread_count,0)+1 WHERE session_id=? AND user_id<>?",sessionId,senderId);

        if (sessionId != null && !sessionId.isEmpty()) {
            ChatSession session = sessionMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatSession>()
                            .eq(ChatSession::getSessionId, sessionId));
            if (session != null) {
                session.setLastMsgContent(content.substring(0,Math.min(500,content.length())));
                session.setLastMsgTime(LocalDateTime.now());
                session.setLastMsgSenderId(senderId);
                sessionMapper.updateById(session);
            }
        }

        // Publish to RabbitMQ for WebSocket broadcast
        try {
            List<Long> memberIds = new ArrayList<>();
            if (sessionId != null && !sessionId.isEmpty()) {
                List<ChatSessionMember> members = sessionMemberMapper.selectBySessionId(sessionId);
                if (members != null) {
                    for (ChatSessionMember m : members) {
                        memberIds.add(m.getUserId());
                    }
                }
            }

            Map<String, Object> broadcast = new LinkedHashMap<>();
            broadcast.put("sessionId", sessionId);
            broadcast.put("msgId", msg.getMsgId());
            broadcast.put("senderId", senderId);
            broadcast.put("senderName", senderName(senderId));
            broadcast.put("content", content);
            broadcast.put("msgType", msgType);
            broadcast.put("sendTime", msg.getSendTime().toString());
            broadcast.put("memberIds", memberIds);
            if (sourceType != null) broadcast.put("sourceType", sourceType);
            if (sourceId != null) broadcast.put("sourceId", sourceId);
            rabbitTemplate.convertAndSend(CHAT_EXCHANGE, CHAT_BROADCAST_KEY, broadcast);
        } catch (Exception e) {
            log.warn("Failed to publish chat broadcast: {}", e.getMessage());
        }

        // Parse @mentions
        if (content != null && content.contains("@")) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(content);
            Set<Long> mentionedUserIds = new HashSet<>();
            if(sessionId!=null && content.contains("@all")) mentionedUserIds.addAll(jdbc.queryForList("SELECT user_id FROM chat_session_member WHERE session_id=? AND user_id<>?",Long.class,sessionId,senderId));
            while (matcher.find()) {
                try {
                    mentionedUserIds.add(Long.parseLong(matcher.group(1)));
                } catch (Exception ignored) {}
            }

            for (Long mentionedUserId : mentionedUserIds) {
                if(sessionId!=null && jdbc.queryForObject("SELECT COUNT(*) FROM chat_session_member WHERE session_id=? AND user_id=?",Integer.class,sessionId,mentionedUserId)==0) continue;
                try {
                    Map<String, Object> notificationPayload = new LinkedHashMap<>();
                    notificationPayload.put("userId", mentionedUserId);
                    notificationPayload.put("type", 1); // 1 = System Notification
                    notificationPayload.put("title", "提及通知");
                    notificationPayload.put("content", "您在讨论中被 @ 了: " + (content.length() > 50 ? content.substring(0, 50) + "..." : content));
                    notificationPayload.put("relatedId", sourceId != null ? sourceId : sessionId);

                    rabbitTemplate.convertAndSend("notification-exchange", "notification.mention", notificationPayload);
                    log.info("Sent @mention notification to user {} via RabbitMQ", mentionedUserId);
                } catch (Exception e) {
                    log.error("Failed to send mention notification for user {}", mentionedUserId, e);
                }
            }
        }
    }

    @Override
    public void recallMessage(Long userId, String msgId) {
        ChatMessage msg = messageMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getMsgId, msgId));
        if(msg==null) throw new com.collaboffice.common.exception.BusinessException(404,"消息不存在");
        if(!msg.getSenderId().equals(userId)) throw new com.collaboffice.common.exception.BusinessException(403,"只能撤回自己的消息");
        if(msg.getSessionId()!=null) access.requireSession(msg.getSessionId());
        if(Integer.valueOf(1).equals(msg.getIsRecalled())) return;
        if(msg.getSendTime()==null||msg.getSendTime().isBefore(LocalDateTime.now().minusMinutes(2))) throw new com.collaboffice.common.exception.BusinessException(400,"只能撤回两分钟内的消息");
        if (msg.getSenderId().equals(userId)) {
            msg.setIsRecalled(1);
            msg.setRecallTime(LocalDateTime.now());
            messageMapper.updateById(msg);
            if(msg.getSessionId()!=null) jdbc.update("UPDATE chat_session SET last_msg_content='此消息已撤回' WHERE session_id=? AND last_msg_time=? AND last_msg_sender_id=?",msg.getSessionId(),msg.getSendTime(),userId);
        }
    }

    @Override
    public void deleteConversation(Long userId, String sessionId) {
        ChatSession session = sessionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getSessionId, sessionId));
        if (session != null) {
            session.setStatus(0);
            sessionMapper.updateById(session);
        }
    }

    @Override
    public void markRead(Long userId, String sessionId) {
        jdbc.update("UPDATE chat_session_member SET last_read_time=NOW() WHERE session_id=? AND user_id=?",sessionId,userId);
        List<ChatSessionMember> members = sessionMemberMapper.selectBySessionId(sessionId);
        for (ChatSessionMember m : members) {
            if (m.getUserId().equals(userId)) {
                m.setUnreadCount(0);
                sessionMemberMapper.updateById(m);
                break;
            }
        }
    }

    @Override
    public List<Map<String, Object>> searchMessages(Long userId, String keyword) {
        if(keyword==null||keyword.isBlank()||keyword.length()>100) throw new com.collaboffice.common.exception.BusinessException(400,"搜索词为1至100字");
        return jdbc.queryForList("SELECT m.msg_id AS msgId,m.session_id AS sessionId,m.sender_id AS senderId,m.content,m.msg_type AS msgType,m.send_time AS sendTime,COALESCE(u.real_name,u.username) AS senderName FROM chat_message m JOIN chat_session_member sm ON sm.session_id=m.session_id AND sm.user_id=? LEFT JOIN sys_user u ON u.id=m.sender_id WHERE m.status=1 AND COALESCE(m.is_recalled,0)=0 AND m.content LIKE ? ORDER BY m.send_time DESC LIMIT 100",userId,"%"+keyword.trim()+"%");
    }

    private String senderName(Long id) {
        List<String> names = jdbc.queryForList("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,id);
        return names.isEmpty()?"用户":names.get(0);
    }

    @Override
    public List<Map<String, Object>> getOfflineMessages(Long userId, Long lastSeqId) {
        List<ChatSessionMember> members = sessionMemberMapper.selectByUserId(userId);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sessionIds = members.stream().map(ChatSessionMember::getSessionId).collect(Collectors.toList());
        List<ChatMessage> messages = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>()
                        .in(ChatMessage::getSessionId, sessionIds)
                        .gt(ChatMessage::getSeqId, lastSeqId)
                        .orderByAsc(ChatMessage::getSeqId)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("msgId", m.getMsgId());
            map.put("sessionId", m.getSessionId());
            map.put("senderId", m.getSenderId());
            map.put("content", Integer.valueOf(1).equals(m.getIsRecalled()) ? "此消息已撤回" : m.getContent());
            map.put("isRecalled", m.getIsRecalled());
            map.put("msgType", m.getMsgType());
            map.put("sendTime", m.getSendTime());
            map.put("seqId", m.getSeqId());
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getComments(String sourceType, String sourceId) {
        List<ChatMessage> messages = messageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSourceType, sourceType)
                        .eq(ChatMessage::getSourceId, sourceId)
                        .orderByAsc(ChatMessage::getSendTime)
        );
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("msgId", m.getMsgId());
            map.put("senderId", m.getSenderId());
            map.put("senderName", m.getSenderName());
            map.put("senderAvatar", m.getSenderAvatar());
            map.put("content", Integer.valueOf(1).equals(m.getIsRecalled()) ? "此消息已撤回" : m.getContent());
            map.put("isRecalled", m.getIsRecalled());
            map.put("msgType", m.getMsgType());
            map.put("sendTime", m.getSendTime());
            map.put("sourceType", m.getSourceType());
            map.put("sourceId", m.getSourceId());
            result.add(map);
        }
        return result;
    }
}
