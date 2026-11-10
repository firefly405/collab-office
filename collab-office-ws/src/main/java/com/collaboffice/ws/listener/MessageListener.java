package com.collaboffice.ws.listener;

import com.collaboffice.common.message.MessageType;
import com.collaboffice.common.message.WebSocketMessage;
import com.collaboffice.ws.broadcaster.MessageBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "#{chatTempQueue.name}")
    public void onChatMessage(Map<String, Object> payload) {
        log.info("WebSocket节点收到MQ聊天广播: {}", payload);
        try {
            String sessionId = (String) payload.get("sessionId");
            String msgId = (String) payload.get("msgId");
            Object senderIdObj = payload.get("senderId");
            String content = (String) payload.get("content");
            Integer msgType = (Integer) payload.get("msgType");
            String sendTime = (String) payload.get("sendTime");

            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("sessionId", sessionId);
            dataNode.put("msgId", msgId);
            dataNode.put("senderId", senderIdObj != null ? senderIdObj.toString() : "");
            dataNode.put("senderName", java.util.Objects.toString(payload.get("senderName"), "用户"));
            dataNode.put("content", content);
            dataNode.put("msgType", msgType != null ? msgType : 1);
            dataNode.put("sendTime", sendTime);

            WebSocketMessage message = WebSocketMessage.builder()
                    .type(MessageType.CHAT_RECEIVE)
                    .userId(senderIdObj != null ? senderIdObj.toString() : "server")
                    .serverTimestamp(System.currentTimeMillis())
                    .data(dataNode)
                    .build();

            List<?> memberIds = (List<?>) payload.get("memberIds");
            if (memberIds != null) {
                for (Object member : memberIds) {
                    if (member != null) {
                        MessageBroadcaster.sendToUser(member.toString(), message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理MQ聊天消息推送失败: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = "#{notificationTempQueue.name}")
    public void onNotificationPush(Map<String, Object> payload) {
        log.info("WebSocket节点收到MQ通知广播: {}", payload);
        try {
            Object userIdObj = payload.get("userId");
            if (userIdObj == null) {
                log.warn("通知载荷缺少接收人userId");
                return;
            }

            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("id", payload.get("id") != null ? payload.get("id").toString() : "");
            dataNode.put("type", payload.get("type") != null ? (Integer) payload.get("type") : 1);
            dataNode.put("title", (String) payload.get("title"));
            dataNode.put("content", (String) payload.get("content"));
            dataNode.put("relatedId", (String) payload.get("relatedId"));
            dataNode.put("sendTime", payload.get("sendTime") != null ? payload.get("sendTime").toString() : "");

            WebSocketMessage message = WebSocketMessage.builder()
                    .type(MessageType.NOTIFICATION_RECEIVE)
                    .userId("server")
                    .serverTimestamp(System.currentTimeMillis())
                    .data(dataNode)
                    .build();

            MessageBroadcaster.sendToUser(userIdObj.toString(), message);
        } catch (Exception e) {
            log.error("处理MQ通知消息推送失败: {}", e.getMessage(), e);
        }
    }
}
