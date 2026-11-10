package com.collaboffice.ws.broadcaster;

import com.collaboffice.common.message.WebSocketMessage;
import com.collaboffice.ws.manager.ConnectionManager;
import com.collaboffice.ws.manager.RoomManager;
import com.collaboffice.ws.util.WebSocketJson;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class MessageBroadcaster {

    public static void broadcastToRoom(String roomId, WebSocketMessage message, String excludeUserId) {
        Set<String> members = RoomManager.getRoomMembers(roomId);
        String json = WebSocketJson.stringify(message);
        TextWebSocketFrame frame = new TextWebSocketFrame(json);

        for (String userId : members) {
            if (userId.equals(excludeUserId)) continue;
            sendToUser(userId, frame);
        }
    }

    public static void broadcastToRoomExceptConnection(String roomId, WebSocketMessage message, String excludeConnectionId) {
        Set<String> members = RoomManager.getRoomMembers(roomId);
        if (members.isEmpty()) return;

        String json = WebSocketJson.stringify(message);
        for (String userId : members) {
            Set<String> connectionIds = ConnectionManager.getUserConnections(userId);
            for (String connId : connectionIds) {
                if (connId.equals(excludeConnectionId)) continue;
                Channel channel = ConnectionManager.getChannel(connId);
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(new TextWebSocketFrame(json));
                }
            }
        }
    }

    public static void sendToUser(String userId, WebSocketMessage message) {
        String json = WebSocketJson.stringify(message);
        TextWebSocketFrame frame = new TextWebSocketFrame(json);
        sendToUser(userId, frame);
    }

    private static void sendToUser(String userId, TextWebSocketFrame frame) {
        Set<String> connectionIds = ConnectionManager.getUserConnections(userId);
        if (connectionIds.isEmpty()) return;

        for (String connId : connectionIds) {
            Channel channel = ConnectionManager.getChannel(connId);
            if (channel != null && channel.isActive()) {
                channel.writeAndFlush(frame.retain());
            }
        }
        frame.release();
    }

    public static void broadcastToAll(WebSocketMessage message, String excludeUserId) {
        // 全局广播
    }

    public static void broadcastToUserGroup(Set<String> userIds, WebSocketMessage message) {
        for (String userId : userIds) {
            sendToUser(userId, message);
        }
    }
}
