package com.collaboffice.ws.util;

import com.collaboffice.common.message.WebSocketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WebSocketJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WebSocketJson() {
    }

    public static WebSocketMessage parse(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, WebSocketMessage.class);
    }

    public static String stringify(WebSocketMessage message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("WebSocket消息序列化失败", e);
        }
    }
}
