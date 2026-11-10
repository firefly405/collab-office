package com.collaboffice.ws.manager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {

    // roomId -> Set<userId>
    private static final ConcurrentHashMap<String, Set<String>> ROOM_MEMBERS = new ConcurrentHashMap<>();
    // userId -> Set<roomId>
    private static final ConcurrentHashMap<String, Set<String>> MEMBER_ROOMS = new ConcurrentHashMap<>();

    public static void joinRoom(String roomId, String userId) {
        ROOM_MEMBERS.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        MEMBER_ROOMS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(roomId);
    }

    public static void leaveRoom(String roomId, String userId) {
        Set<String> members = ROOM_MEMBERS.get(roomId);
        if (members != null) {
            members.remove(userId);
            if (members.isEmpty()) {
                ROOM_MEMBERS.remove(roomId);
            }
        }
        Set<String> rooms = MEMBER_ROOMS.get(userId);
        if (rooms != null) {
            rooms.remove(roomId);
            if (rooms.isEmpty()) {
                MEMBER_ROOMS.remove(userId);
            }
        }
    }

    public static Set<String> getRoomMembers(String roomId) {
        return ROOM_MEMBERS.getOrDefault(roomId, ConcurrentHashMap.newKeySet());
    }

    public static Set<String> getUserRooms(String userId) {
        return MEMBER_ROOMS.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }

    public static void kickUser(String roomId, String userId) {
        leaveRoom(roomId, userId);
    }

    public static int getRoomMemberCount(String roomId) {
        Set<String> members = ROOM_MEMBERS.get(roomId);
        return members != null ? members.size() : 0;
    }
}
