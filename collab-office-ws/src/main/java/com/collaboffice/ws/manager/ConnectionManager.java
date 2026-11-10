package com.collaboffice.ws.manager;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConnectionManager {

    private static final int MAX_DEVICES_PER_USER = 5;

    // connectionId -> Channel
    private static final ConcurrentHashMap<String, Channel> CONNECTIONS = new ConcurrentHashMap<>();
    // userId -> Set<connectionId>
    private static final ConcurrentHashMap<String, Set<String>> USER_CONNECTIONS = new ConcurrentHashMap<>();

    public static void addConnection(Channel channel) {
        String connectionId = channel.id().asShortText();
        CONNECTIONS.put(connectionId, channel);
    }

    public static void bindUser(String connectionId, String userId) {
        Channel channel = CONNECTIONS.get(connectionId);
        if (channel == null) return;

        USER_CONNECTIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(connectionId);

        Set<String> userConns = USER_CONNECTIONS.get(userId);
        if (userConns != null && userConns.size() > MAX_DEVICES_PER_USER) {
            String oldestConnId = userConns.iterator().next();
            Channel oldestChannel = CONNECTIONS.get(oldestConnId);
            if (oldestChannel != null) {
                oldestChannel.close();
            }
            removeConnection(oldestConnId);
        }

        log.info("用户绑定: userId={}, connectionId={}, 设备数={}", userId, connectionId,
                USER_CONNECTIONS.getOrDefault(userId, ConcurrentHashMap.newKeySet()).size());
    }

    public static void removeConnection(Channel channel) {
        String connectionId = channel.id().asShortText();
        removeConnection(connectionId);
    }

    public static void removeConnection(String connectionId) {
        CONNECTIONS.remove(connectionId);
        USER_CONNECTIONS.forEach((userId, connSet) -> {
            connSet.remove(connectionId);
            if (connSet.isEmpty()) {
                USER_CONNECTIONS.remove(userId);
            }
        });
    }

    public static Set<String> getUserConnections(String userId) {
        return USER_CONNECTIONS.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }

    public static Channel getChannel(String connectionId) {
        return CONNECTIONS.get(connectionId);
    }

    public static int getOnlineUserCount() {
        return USER_CONNECTIONS.size();
    }

    public static int getConnectionCount() {
        return CONNECTIONS.size();
    }

    public static boolean isUserOnline(String userId) {
        Set<String> conns = USER_CONNECTIONS.get(userId);
        return conns != null && !conns.isEmpty();
    }
}
