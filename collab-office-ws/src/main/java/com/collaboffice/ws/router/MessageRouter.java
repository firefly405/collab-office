package com.collaboffice.ws.router;

import com.collaboffice.common.message.MessageType;
import com.collaboffice.common.message.WebSocketMessage;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.ws.broadcaster.MessageBroadcaster;
import com.collaboffice.ws.manager.ConnectionManager;
import com.collaboffice.ws.manager.RoomManager;
import com.collaboffice.ws.server.WebSocketAuthHandler;
import com.collaboffice.ws.util.WebSocketJson;
import com.collaboffice.ws.client.CollaborationClient;
import com.collaboffice.ws.client.DocumentClient;
import com.collaboffice.ws.client.MessageClient;
import com.collaboffice.ws.client.NotificationClient;
import com.collaboffice.ws.client.MeetingClient;
import io.netty.channel.Channel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageRouter {

    private static final String DOC_OPERATION_EXCHANGE = "doc-operation-exchange";
    private static final String YJS_PERSIST_KEY = "doc.operation.yjs_update";

    private final MessageClient messageClient;
    private final NotificationClient notificationClient;
    private final MeetingClient meetingClient;
    private final DocumentClient documentClient;
    private final CollaborationClient collaborationClient;
    private final RabbitTemplate rabbitTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Value("${app.internal-token:default-secret-token}")
    private String internalToken;

    @Value("${app.collab.history-sync-limit:200}")
    private int historySyncLimit;

    public void route(Channel channel, WebSocketMessage message) {
        MessageType type = message.getType();
        if (type == null) {
            log.warn("未知消息类型");
            return;
        }

        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        String targetUserId = message.getUserId(); // 保存原始接收者ID
        message.setUserId(userId);
        if(type==MessageType.CHAT_SEND || type==MessageType.TASK_UPDATE || type==MessageType.MEETING_SIGNAL || type==MessageType.MEETING_MEDIA) return;
        if(type==MessageType.SHEET_CELL_UPDATE || type==MessageType.MINDMAP_NODE_UPDATE || type==MessageType.WHITEBOARD_DRAW || type==MessageType.DOC_CURSOR) {
            try {
                var allowed=documentClient.checkDocPermission(internalToken,message.getRoomId(),Long.valueOf(userId),type!=MessageType.DOC_CURSOR);
                if(allowed==null || allowed.getCode()!=200 || !Boolean.TRUE.equals(allowed.getData())) return;
            } catch(Exception e) {return;}
        }

        switch (type) {
            case CONNECT -> handleConnect(channel, message);
            case DISCONNECT -> handleDisconnect(channel, message);
            case HEARTBEAT -> handleHeartbeat(channel, message);
            case DOC_JOIN, DOC_LEAVE -> handleDocRoom(channel, message);
            case DOC_OPERATION -> handleDocOperation(channel, message);
            case DOC_CURSOR -> handleDocCursor(channel, message);
            case CHAT_SEND -> handleChatSend(channel, message, targetUserId);
            case CHAT_READ -> handleChatRead(channel, message);
            case SHEET_CELL_UPDATE -> handleSheetCell(channel, message);
            case MINDMAP_NODE_UPDATE -> handleMindmapNode(channel, message);
            case WHITEBOARD_DRAW -> handleWhiteboardDraw(channel, message);
            case MEETING_JOIN, MEETING_LEAVE -> handleMeetingRoom(channel, message);
            case MEETING_SIGNAL, MEETING_MEDIA -> handleMeetingMedia(channel, message);
            case TASK_UPDATE -> handleTaskUpdate(channel, message);
            case MSG_SYNC -> handleMsgSync(channel, message);
            case MUTE_ALL, MUTE_USER, KICK_USER, GRANT_HOST -> handleMeetingControl(channel, message);
            default -> handleDefault(channel, message);
        }
    }

    private void handleConnect(Channel channel, WebSocketMessage message) {
        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        String connectionId = channel.id().asShortText();
        ConnectionManager.bindUser(connectionId, userId);

        WebSocketMessage ack = WebSocketMessage.builder()
                .type(MessageType.CONNECT_ACK)
                .userId("server")
                .build();
        channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(ack)));
    }

    private void handleDisconnect(Channel channel, WebSocketMessage message) {
        channel.close();
    }

    private void handleHeartbeat(Channel channel, WebSocketMessage message) {
        WebSocketMessage ack = WebSocketMessage.builder()
                .type(MessageType.HEARTBEAT_ACK)
                .userId("server")
                .build();
        channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(ack)));
    }

    private void handleDocRoom(Channel channel, WebSocketMessage message) {
        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        String roomId = message.getRoomId();

        if (message.getType() == MessageType.DOC_JOIN) {
            // 房间权限校验: 用户必须是文档创建者/成员, 或文档为公开
            if (!checkDocPermission(roomId, userId)) {
                log.warn("DOC_JOIN denied: user={} has no permission to doc={}", userId, roomId);
                WebSocketMessage err = WebSocketMessage.builder()
                        .type(MessageType.ERROR)
                        .userId("server")
                        .roomId(roomId)
                        .data(objectMapper.valueToTree(Map.of(
                                "message", "无文档访问权限, 无法加入房间",
                                "code", "DOC_PERMISSION_DENIED"
                        )))
                        .build();
                channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(err)));
                return;
            }
            RoomManager.joinRoom(roomId, userId);

            // 加入成功后, 拉取历史 Yjs updates 推给该客户端做初始化同步
            pushHistoricalYjsUpdates(channel, roomId, userId);
        } else {
            RoomManager.leaveRoom(roomId, userId);
        }
    }

    /**
     * 新客户端 DOC_JOIN 成功后, 调用 collaboration-service 拉取历史 Yjs updates,
     * 逐条通过 DOC_OPERATION 消息推给该客户端, 让前端 Y.Doc.applyUpdate 完成初始化同步。
     */
    private void pushHistoricalYjsUpdates(Channel channel, String docId, String userId) {
        try {
            CommonResult<List<Map<String, Object>>> res =
                    collaborationClient.getYjsUpdates(internalToken, docId, 0L, historySyncLimit);
            if (res == null || res.getCode() != 200 || res.getData() == null || res.getData().isEmpty()) {
                return;
            }
            for (Map<String, Object> update : res.getData()) {
                Map<String, Object> data = new HashMap<>();
                data.put("kind", update.getOrDefault("kind", "yjs_update"));
                data.put("clientId", update.getOrDefault("clientId", "server-history"));
                data.put("update", update.get("update"));
                data.put("seq", update.get("seq"));
                data.put("userName", "server-history");
                data.put("color", "#888888");

                WebSocketMessage historyMsg = WebSocketMessage.builder()
                        .type(MessageType.DOC_OPERATION_BROADCAST)
                        .userId("server")
                        .roomId(docId)
                        .data(objectMapper.valueToTree(data))
                        .build();
                channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(historyMsg)));
            }
            log.info("Pushed {} historical Yjs updates to user={} for doc={}",
                    res.getData().size(), userId, docId);
        } catch (Exception e) {
            log.warn("Failed to push historical Yjs updates for doc={} user={}: {}",
                    docId, userId, e.getMessage());
        }
    }

    /**
     * 调用 document-service 校验用户对文档的访问权限
     */
    private boolean checkDocPermission(String docId, String userId) {
        if (userId == null || docId == null) return false;
        try {
            Long uid = Long.valueOf(userId);
            CommonResult<Boolean> res = documentClient.checkDocPermission(internalToken, docId, uid, false);
            return res != null && res.getCode() == 200 && Boolean.TRUE.equals(res.getData());
        } catch (Exception e) {
            log.error("Failed to check doc permission for user={} doc={}: {}", userId, docId, e.getMessage());
            // 服务端调用失败时拒绝, 防止绕过
            return false;
        }
    }

    private void handleDocOperation(Channel channel, WebSocketMessage message) {
        String docId = message.getRoomId();
        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();

        // 1. 实时广播给房间内其他连接（CRDT 中继）
        try {
            var allowed = documentClient.checkDocPermission(internalToken, docId, Long.valueOf(userId), true);
            if (allowed == null || allowed.getCode()!=200 || !Boolean.TRUE.equals(allowed.getData())) return;
        } catch (Exception e) { return; }
        message.setType(MessageType.DOC_OPERATION_BROADCAST);
        MessageBroadcaster.broadcastToRoomExceptConnection(docId, message, channel.id().asShortText());

        // 2. 异步转发到 collaboration-service 做持久化（不影响实时广播）
        forwardYjsUpdateToPersist(docId, userId, message.getData());
    }

    /**
     * 将 Yjs update 转发到 RabbitMQ 的 doc-yjs-persist-queue,
     * 由 collaboration-service 消费并落库（分配 seq + 写入 doc_yjs_update 表）。
     */
    private void forwardYjsUpdateToPersist(String docId, String userId,
                                            com.fasterxml.jackson.databind.JsonNode dataNode) {
        if (docId == null || dataNode == null) return;
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("docId", docId);
            payload.put("userId", userId);
            payload.put("clientId", asText(dataNode.get("clientId")));
            payload.put("update", asText(dataNode.get("update")));
            payload.put("kind", asText(dataNode.get("kind")));
            rabbitTemplate.convertAndSend(DOC_OPERATION_EXCHANGE, YJS_PERSIST_KEY, payload);
        } catch (Exception e) {
            log.warn("Failed to forward Yjs update to persist queue for doc={}: {}", docId, e.getMessage());
        }
    }

    private static String asText(com.fasterxml.jackson.databind.JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private void handleDocCursor(Channel channel, WebSocketMessage message) {
        message.setType(MessageType.DOC_CURSOR_BROADCAST);
        MessageBroadcaster.broadcastToRoomExceptConnection(message.getRoomId(), message, channel.id().asShortText());
    }

    private void handleChatSend(Channel channel, WebSocketMessage message, String targetUserId) {
        if (targetUserId != null) {
            MessageBroadcaster.sendToUser(targetUserId, message);
        } else {
            MessageBroadcaster.broadcastToRoom(message.getRoomId(), message, channel.attr(WebSocketAuthHandler.USER_ID_KEY).get());
        }
    }

    private void handleChatRead(Channel channel, WebSocketMessage message) {
        MessageBroadcaster.sendToUser(message.getUserId(), message);
    }

    private void handleSheetCell(Channel channel, WebSocketMessage message) {
        message.setType(MessageType.SHEET_CELL_BROADCAST);
        MessageBroadcaster.broadcastToRoomExceptConnection(message.getRoomId(), message, channel.id().asShortText());
    }

    private void handleMindmapNode(Channel channel, WebSocketMessage message) {
        message.setType(MessageType.MINDMAP_NODE_BROADCAST);
        MessageBroadcaster.broadcastToRoomExceptConnection(message.getRoomId(), message, channel.id().asShortText());
    }

    private void handleWhiteboardDraw(Channel channel, WebSocketMessage message) {
        message.setType(MessageType.WHITEBOARD_DRAW_BROADCAST);
        MessageBroadcaster.broadcastToRoomExceptConnection(message.getRoomId(), message, channel.id().asShortText());
    }

    private void handleMeetingRoom(Channel channel, WebSocketMessage message) {
        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        if (message.getType() == MessageType.MEETING_JOIN) {
            // 房间权限校验: 用户必须是主持人或已加入会议的参与者
            if (!checkMeetingPermission(message.getRoomId(), userId)) {
                log.warn("MEETING_JOIN denied: user={} has no permission to meeting={}", userId, message.getRoomId());
                WebSocketMessage err = WebSocketMessage.builder()
                        .type(MessageType.ERROR)
                        .userId("server")
                        .roomId(message.getRoomId())
                        .data(objectMapper.valueToTree(Map.of(
                                "message", "无会议参与权限, 无法加入房间",
                                "code", "MEETING_PERMISSION_DENIED"
                        )))
                        .build();
                channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(err)));
                return;
            }
            RoomManager.joinRoom(message.getRoomId(), userId);
        } else {
            RoomManager.leaveRoom(message.getRoomId(), userId);
        }
        MessageBroadcaster.broadcastToRoom(message.getRoomId(), message, userId);
    }

    /**
     * 调用 meeting-service 校验用户对会议的访问权限
     */
    private boolean checkMeetingPermission(String meetingId, String userId) {
        if (userId == null || meetingId == null) return false;
        try {
            Long uid = Long.valueOf(userId);
            CommonResult<Boolean> res = meetingClient.checkMeetingPermission(internalToken, meetingId, uid);
            return res != null && res.getCode() == 200 && Boolean.TRUE.equals(res.getData());
        } catch (Exception e) {
            log.error("Failed to check meeting permission for user={} meeting={}: {}", userId, meetingId, e.getMessage());
            return false;
        }
    }

    private void handleMeetingMedia(Channel channel, WebSocketMessage message) {
        MessageBroadcaster.broadcastToRoomExceptConnection(message.getRoomId(), message, channel.id().asShortText());
    }

    private void handleTaskUpdate(Channel channel, WebSocketMessage message) {
        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        MessageBroadcaster.broadcastToRoom(message.getRoomId(), message, userId);
    }

    private void handleMsgSync(Channel channel, WebSocketMessage message) {
        String userId = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        if (userId == null) return;
        long lastSeqId = 0L;
        if (message.getData() != null && message.getData().get("lastSeqId") != null) {
            try {
                lastSeqId = Long.parseLong(message.getData().get("lastSeqId").toString());
            } catch (Exception e) {
                log.warn("Failed to parse lastSeqId: {}", message.getData().get("lastSeqId"));
            }
        }
        log.info("MSG_SYNC requested by user={}, lastSeq={}", userId, lastSeqId);

        // 1. Sync messages
        try {
            var msgRes = messageClient.getOfflineMessages(internalToken, Long.valueOf(userId), lastSeqId);
            if (msgRes != null && msgRes.getCode() == 200 && msgRes.getData() != null) {
                for (var msgMap : msgRes.getData()) {
                    WebSocketMessage syncMsg = WebSocketMessage.builder()
                            .type(MessageType.CHAT_RECEIVE)
                            .userId(msgMap.get("senderId") != null ? msgMap.get("senderId").toString() : null)
                            .roomId(msgMap.get("sessionId") != null ? msgMap.get("sessionId").toString() : null)
                            .data(objectMapper.valueToTree(msgMap))
                            .build();
                    channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(syncMsg)));
                }
            }
        } catch (Exception e) {
            log.error("Failed to query offline messages for user: {}", userId, e);
        }

        // 2. Sync notifications
        try {
            var notifRes = notificationClient.getOfflineNotifications(internalToken, Long.valueOf(userId), lastSeqId);
            if (notifRes != null && notifRes.getCode() == 200 && notifRes.getData() != null) {
                for (var notifMap : notifRes.getData()) {
                    WebSocketMessage syncNotif = WebSocketMessage.builder()
                            .type(MessageType.NOTIFICATION_RECEIVE)
                            .userId("server")
                            .data(objectMapper.valueToTree(notifMap))
                            .build();
                    channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(syncNotif)));
                }
            }
        } catch (Exception e) {
            log.error("Failed to query offline notifications for user: {}", userId, e);
        }

        WebSocketMessage response = WebSocketMessage.builder()
                .type(MessageType.MSG_SYNC_RESPONSE)
                .userId("server")
                .roomId(message.getRoomId())
                .build();
        channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(response)));
    }

    private void handleMeetingControl(Channel channel, WebSocketMessage message) {
        String userIdStr = channel.attr(WebSocketAuthHandler.USER_ID_KEY).get();
        if (userIdStr == null) return;
        Long operatorId = Long.valueOf(userIdStr);
        String meetingId = message.getRoomId();
        String controlType = message.getType().getCode(); // mute_all, mute_user, kick_user, grant_host

        Long targetUserId = null;
        if (message.getData() != null && message.getData().get("targetUserId") != null) {
            try {
                targetUserId = Long.valueOf(message.getData().get("targetUserId").toString());
            } catch (Exception e) {
                log.warn("Failed to parse targetUserId for meeting control: {}", message.getData().get("targetUserId"));
            }
        }

        log.info("Verifying meeting control: type={}, operator={}, meeting={}, target={}",
                controlType, operatorId, meetingId, targetUserId);

        try {
            var res = meetingClient.handleMeetingControl(internalToken, meetingId, operatorId, controlType, targetUserId);
            if (res != null && res.getCode() == 200 && Boolean.TRUE.equals(res.getData())) {
                log.info("Meeting control approved by meeting-service, broadcasting to room={}", meetingId);
                MessageBroadcaster.broadcastToRoomExceptConnection(meetingId, message, channel.id().asShortText());
            } else {
                log.warn("Meeting control denied or failed: operator={} is not host or error occurred", operatorId);
                WebSocketMessage err = WebSocketMessage.builder()
                        .type(MessageType.ERROR)
                        .userId("server")
                        .data(objectMapper.valueToTree(Map.of("message", "无主持人权限，或会议控制操作执行失败")))
                        .build();
                channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(err)));
            }
        } catch (Exception e) {
            log.error("Failed to call meeting-service for control verification: {}", e.getMessage());
            WebSocketMessage err = WebSocketMessage.builder()
                    .type(MessageType.ERROR)
                    .userId("server")
                    .data(objectMapper.valueToTree(Map.of("message", "服务端通信异常，无法执行控制操作")))
                    .build();
            channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(err)));
        }
    }

    private void handleDefault(Channel channel, WebSocketMessage message) {
        // 默认广播到房间
        if (message.getRoomId() != null) {
            MessageBroadcaster.broadcastToRoomExceptConnection(message.getRoomId(), message, channel.id().asShortText());
        }
    }
}
