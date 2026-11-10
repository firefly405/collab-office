package com.collaboffice.ws.heartbeat;

import com.collaboffice.common.message.MessageType;
import com.collaboffice.common.message.WebSocketMessage;
import com.collaboffice.ws.manager.ConnectionManager;
import com.collaboffice.ws.util.WebSocketJson;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HeartbeatDetector {

    private static final long HEARTBEAT_INTERVAL = 30_000L;
    private static final long IDLE_TIMEOUT = 90_000L;

    private static final ConcurrentHashMap<String, Long> LAST_ACTIVE_TIME = new ConcurrentHashMap<>();

    public static void updateLastActiveTime(String connectionId) {
        LAST_ACTIVE_TIME.put(connectionId, System.currentTimeMillis());
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void checkHeartbeat() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = LAST_ACTIVE_TIME.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            String connectionId = entry.getKey();
            long lastActive = entry.getValue();

            if (now - lastActive > IDLE_TIMEOUT) {
                Channel channel = ConnectionManager.getChannel(connectionId);
                if (channel != null && channel.isActive()) {
                    // 发送PING帧
                    WebSocketMessage ping = WebSocketMessage.builder()
                            .type(MessageType.HEARTBEAT)
                            .userId("server")
                            .build();
                    channel.writeAndFlush(new TextWebSocketFrame(WebSocketJson.stringify(ping)));
                } else {
                    // 连接已断，清理
                    ConnectionManager.removeConnection(connectionId);
                    it.remove();
                    log.info("清理断线连接: {}", connectionId);
                }
            }
        }
    }
}
