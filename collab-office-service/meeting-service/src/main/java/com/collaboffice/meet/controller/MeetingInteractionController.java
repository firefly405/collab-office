package com.collaboffice.meet.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.meet.service.MeetingInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/meeting")
@RequiredArgsConstructor
public class MeetingInteractionController {

    private final MeetingInteractionService interactionService;

    @Operation(summary = "举手/取消举手")
    @PostMapping("/{roomId}/raise-hand")
    public CommonResult<Void> raiseHand(@PathVariable String roomId,
                                         @RequestHeader("X-User-Id") Long userId,
                                         @RequestParam(defaultValue = "true") boolean isRaised) {
        interactionService.raiseHand(roomId, userId, isRaised);
        return CommonResult.success(null);
    }

    @Operation(summary = "创建投票")
    @PostMapping("/{roomId}/vote/create")
    public CommonResult<String> createVote(@PathVariable String roomId,
                                            @RequestHeader("X-User-Id") Long userId,
                                            @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        @SuppressWarnings("unchecked")
        List<String> options = (List<String>) body.get("options");
        return CommonResult.success(interactionService.createVote(roomId, userId, title, options));
    }

    @Operation(summary = "提交投票")
    @PostMapping("/{roomId}/vote/{voteId}/submit")
    public CommonResult<Void> submitVote(@PathVariable String roomId,
                                          @PathVariable String voteId,
                                          @RequestHeader("X-User-Id") Long userId,
                                          @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> selectedOptions = (List<Integer>) body.get("selectedOptions");
        interactionService.submitVote(voteId, userId, selectedOptions != null ? selectedOptions : List.of());
        return CommonResult.success(null);
    }

    @Operation(summary = "获取投票结果")
    @GetMapping("/{roomId}/vote/{voteId}/result")
    public CommonResult<Map<String, Object>> getVoteResult(@PathVariable String roomId,
                                                            @PathVariable String voteId) {
        return CommonResult.success(interactionService.getVoteResult(voteId));
    }

    @Operation(summary = "开始录制")
    @PostMapping("/{roomId}/recording/start")
    public CommonResult<String> startRecording(@PathVariable String roomId,
                                                @RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(interactionService.startRecording(roomId, userId));
    }

    @Operation(summary = "完成录制")
    @PutMapping("/{roomId}/recording/{recordingId}/finish")
    public CommonResult<Void> finishRecording(@PathVariable String roomId,
                                               @PathVariable String recordingId,
                                               @RequestBody Map<String, Object> body) {
        String minioObjectKey = (String) body.get("minioObjectKey");
        long fileSize = ((Number) body.get("fileSize")).longValue();
        int duration = ((Number) body.get("duration")).intValue();
        interactionService.finishRecording(recordingId, minioObjectKey, fileSize, duration);
        return CommonResult.success(null);
    }

    @Operation(summary = "获取录制上传预签名URL")
    @GetMapping("/{roomId}/recording/{recordingId}/upload-url")
    public CommonResult<Map<String, String>> getRecordingUploadUrl(@PathVariable String roomId,
                                                                    @PathVariable String recordingId) {
        String url = interactionService.getRecordingUploadUrl(roomId, recordingId);
        return CommonResult.success(Map.of("uploadUrl", url));
    }

    @Operation(summary = "获取录制列表")
    @GetMapping("/{roomId}/recordings")
    public CommonResult<List<Map<String, Object>>> getRecordings(@PathVariable String roomId) {
        return CommonResult.success(interactionService.getRecordings(roomId));
    }

    @Operation(summary = "获取录制回放URL")
    @GetMapping("/{roomId}/recordings/{recordingId}/playback")
    public CommonResult<Map<String, String>> getPlaybackUrl(@PathVariable String roomId,
                                                             @PathVariable String recordingId) {
        return CommonResult.success(Map.of("playbackUrl",
                interactionService.getPlaybackUrl(roomId, recordingId)));
    }

    @Operation(summary = "删除录制")
    @DeleteMapping("/{roomId}/recordings/{recordingId}")
    public CommonResult<Void> deleteRecording(@PathVariable String roomId,
                                               @PathVariable String recordingId) {
        interactionService.deleteRecording(recordingId);
        return CommonResult.success(null);
    }

    @Operation(summary = "获取录制HLS流播放URL")
    @GetMapping("/{roomId}/recordings/{recordingId}/stream")
    public CommonResult<Map<String, String>> getStreamUrl(@PathVariable String roomId,
                                                           @PathVariable String recordingId) {
        return CommonResult.success(Map.of("streamUrl",
                interactionService.getStreamUrl(roomId, recordingId)));
    }
}
