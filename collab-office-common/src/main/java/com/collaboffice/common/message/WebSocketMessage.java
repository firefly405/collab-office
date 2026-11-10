package com.collaboffice.common.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private MessageType type;
    private String roomId;
    private String userId;
    private Long clientTimestamp;
    private Long serverTimestamp;
    private Long seq;
    private JsonNode data;
}
