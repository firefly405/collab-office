package com.collaboffice.msg.service;

import java.util.List;
import java.util.Map;

public interface ChatService {
    String createSession(Long userId, Integer type, String name, List<Long> memberIds);
    List<Map<String, Object>> getConversations(Long userId);
    List<Map<String, Object>> getMessages(String sessionId, int page, int size);
    void sendMessage(String sessionId, Long senderId, String content, Integer msgType);
    void sendMessage(String sessionId, Long senderId, String content, Integer msgType, String sourceType, String sourceId);
    void sendReply(String sessionId, Long senderId, String content, Integer msgType, String replyToId);
    void recallMessage(Long userId, String msgId);
    void deleteConversation(Long userId, String sessionId);
    void markRead(Long userId, String sessionId);
    List<Map<String, Object>> searchMessages(Long userId, String keyword);
    List<Map<String, Object>> getOfflineMessages(Long userId, Long lastSeqId);
    List<Map<String, Object>> getComments(String sourceType, String sourceId);
}
