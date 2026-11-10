package com.collaboffice.common.message;

import lombok.Getter;

@Getter
public enum MessageType {

    CONNECT("connect", "连接请求"),
    CONNECT_ACK("connect_ack", "连接确认"),
    DISCONNECT("disconnect", "断开连接"),
    HEARTBEAT("heartbeat", "心跳"),
    HEARTBEAT_ACK("heartbeat_ack", "心跳确认"),

    DOC_JOIN("doc_join", "加入文档"),
    DOC_LEAVE("doc_leave", "离开文档"),
    DOC_SYNC("doc_sync", "文档同步"),
    DOC_OPERATION("doc_operation", "文档操作"),
    DOC_OPERATION_BROADCAST("doc_operation_broadcast", "文档操作广播"),
    DOC_CURSOR("doc_cursor", "光标位置"),
    DOC_CURSOR_BROADCAST("doc_cursor_broadcast", "光标广播"),
    DOC_AWARENESS("doc_awareness", "在线状态"),

    CHAT_SEND("chat_send", "发送消息"),
    CHAT_RECEIVE("chat_receive", "接收消息"),
    NOTIFICATION_RECEIVE("notification_receive", "接收系统通知"),
    CHAT_READ("chat_read", "消息已读"),
    CHAT_TYPING("chat_typing", "正在输入"),
    CHAT_TYPING_BROADCAST("chat_typing_broadcast", "输入状态广播"),
    MSG_SYNC("msg_sync", "离线消息同步请求"),
    MSG_SYNC_RESPONSE("msg_sync_response", "离线消息同步响应"),

    MEETING_JOIN("meeting_join", "加入会议"),
    MEETING_LEAVE("meeting_leave", "离开会议"),
    MEETING_SIGNAL("meeting_signal", "会议信令"),
    MEETING_MEDIA("meeting_media", "媒体控制"),
    MUTE_ALL("mute_all", "全体静音"),
    MUTE_USER("mute_user", "静音指定用户"),
    KICK_USER("kick_user", "移出会议"),
    GRANT_HOST("grant_host", "转让主持人"),

    TASK_UPDATE("task_update", "任务更新"),
    TASK_BROADCAST("task_broadcast", "任务广播"),

    SHEET_CELL_UPDATE("sheet_cell_update", "单元格更新"),
    SHEET_CELL_BROADCAST("sheet_cell_broadcast", "单元格广播"),

    MINDMAP_NODE_UPDATE("mindmap_node_update", "思维导图节点更新"),
    MINDMAP_NODE_BROADCAST("mindmap_node_broadcast", "思维导图广播"),

    WHITEBOARD_DRAW("whiteboard_draw", "白板绘制"),
    WHITEBOARD_DRAW_BROADCAST("whiteboard_draw_broadcast", "白板广播"),

    ERROR("error", "错误");

    private final String code;
    private final String description;

    MessageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static MessageType fromCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
