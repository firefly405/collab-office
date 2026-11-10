package com.collaboffice.meet.controller;

import com.collaboffice.meet.service.MeetingInteractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeetingInteractionController - standalone 接口测试")
class MeetingInteractionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MeetingInteractionService interactionService;

    @InjectMocks
    private MeetingInteractionController interactionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interactionController).build();
    }

    @Test
    @DisplayName("POST /api/v1/meeting/{roomId}/raise-hand - 成功举手/取消举手")
    void testRaiseHand() throws Exception {
        Mockito.doNothing().when(interactionService).raiseHand(anyString(), anyLong(), anyBoolean());

        mockMvc.perform(post("/api/v1/meeting/room123/raise-hand")
                        .header("X-User-Id", 1001L)
                        .param("isRaised", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Mockito.verify(interactionService).raiseHand("room123", 1001L, true);
    }

    @Test
    @DisplayName("POST /api/v1/meeting/{roomId}/vote/create - 成功创建投票")
    void testCreateVote() throws Exception {
        Mockito.when(interactionService.createVote(anyString(), anyLong(), anyString(), anyList()))
                .thenReturn("vote_456");

        Map<String, Object> body = Map.of(
                "title", "你想选择哪个方案？",
                "options", List.of("方案A", "方案B")
        );

        mockMvc.perform(post("/api/v1/meeting/room123/vote/create")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("vote_456"));
    }

    @Test
    @DisplayName("POST /api/v1/meeting/{roomId}/vote/{voteId}/submit - 提交投票")
    void testSubmitVote() throws Exception {
        Mockito.doNothing().when(interactionService).submitVote(anyString(), anyLong(), anyList());

        Map<String, Object> body = Map.of(
                "selectedOptions", List.of(0, 1)
        );

        mockMvc.perform(post("/api/v1/meeting/room123/vote/vote_456/submit")
                        .header("X-User-Id", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/meeting/{roomId}/vote/{voteId}/result - 获取投票结果")
    void testGetVoteResult() throws Exception {
        Map<String, Object> mockResult = Map.of(
                "voteId", "vote_456",
                "title", "方案选择",
                "totalVoters", 5
        );
        Mockito.when(interactionService.getVoteResult(anyString())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/meeting/room123/vote/vote_456/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.voteId").value("vote_456"));
    }

    @Test
    @DisplayName("POST /api/v1/meeting/{roomId}/recording/start - 开始录制")
    void testStartRecording() throws Exception {
        Mockito.when(interactionService.startRecording(anyString(), anyLong())).thenReturn("rec_789");

        mockMvc.perform(post("/api/v1/meeting/room123/recording/start")
                        .header("X-User-Id", 1001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("rec_789"));
    }

    @Test
    @DisplayName("PUT /api/v1/meeting/{roomId}/recording/{recordingId}/finish - 完成录制")
    void testFinishRecording() throws Exception {
        Mockito.doNothing().when(interactionService).finishRecording(anyString(), anyString(), anyLong(), anyInt());

        Map<String, Object> body = Map.of(
                "minioObjectKey", "recordings/room123/rec_789.webm",
                "fileSize", 102400L,
                "duration", 120
        );

        mockMvc.perform(put("/api/v1/meeting/room123/recording/rec_789/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
