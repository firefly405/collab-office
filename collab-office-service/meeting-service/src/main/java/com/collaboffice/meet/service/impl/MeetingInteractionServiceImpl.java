package com.collaboffice.meet.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.meet.entity.*;
import com.collaboffice.meet.mapper.*;
import com.collaboffice.meet.service.MeetingInteractionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingInteractionServiceImpl implements MeetingInteractionService {

    private final MeetingVoteMapper voteMapper;
    private final MeetingVoteRecordMapper voteRecordMapper;
    private final MeetingRecordingMapper recordingMapper;
    private final RabbitTemplate rabbitTemplate;
    private final MinioClient minioClient;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String INTERACTION_EXCHANGE = "meeting-interaction-exchange";

    @Override
    public void raiseHand(String meetingId, Long userId, boolean isRaised) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("meetingId", meetingId);
        payload.put("userId", userId);
        payload.put("type", isRaised ? "hand_raised" : "hand_lowered");
        payload.put("timestamp", System.currentTimeMillis());
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, "meeting.interaction.raise_hand", payload);
    }

    @Override
    @Transactional
    public String createVote(String meetingId, Long creatorId, String title, List<String> options) {
        String voteId = SnowflakeIdGenerator.nextIdStr();
        MeetingVote vote = new MeetingVote();
        vote.setVoteId(voteId);
        vote.setMeetingId(meetingId);
        vote.setTitle(title);
        vote.setCreatorId(creatorId);
        vote.setOptions(toJson(options));
        vote.setStatus(0);
        vote.setCreateTime(LocalDateTime.now());
        voteMapper.insert(vote);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("meetingId", meetingId);
        payload.put("voteId", voteId);
        payload.put("type", "vote_created");
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, "meeting.interaction.vote", payload);
        return voteId;
    }

    @Override
    @Transactional
    public void submitVote(String voteId, Long userId, List<Integer> selectedOptionIndexes) {
        MeetingVote vote = voteMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingVote>()
                        .eq(MeetingVote::getVoteId, voteId));
        if (vote == null || vote.getStatus() != 0) {
            throw new RuntimeException("投票不存在或已结束");
        }
        MeetingVoteRecord existing = voteRecordMapper.selectByVoteAndUser(voteId, userId);
        if (existing != null) {
            throw new RuntimeException("您已投过票");
        }
        MeetingVoteRecord record = new MeetingVoteRecord();
        record.setVoteId(voteId);
        record.setMeetingId(vote.getMeetingId());
        record.setUserId(userId);
        record.setSelectedOptions(toJson(selectedOptionIndexes));
        record.setCreateTime(LocalDateTime.now());
        voteRecordMapper.insert(record);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("voteId", voteId);
        payload.put("type", "vote_submitted");
        rabbitTemplate.convertAndSend(INTERACTION_EXCHANGE, "meeting.interaction.vote", payload);
    }

    @Override
    public Map<String, Object> getVoteResult(String voteId) {
        MeetingVote vote = voteMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingVote>()
                        .eq(MeetingVote::getVoteId, voteId));
        if (vote == null) throw new RuntimeException("投票不存在");

        List<String> options = fromJson(vote.getOptions(), new TypeReference<List<String>>() {});
        List<MeetingVoteRecord> records = voteRecordMapper.selectByVoteId(voteId);
        int[] counts = new int[options.size()];
        for (MeetingVoteRecord r : records) {
            List<Integer> selected = fromJson(r.getSelectedOptions(), new TypeReference<List<Integer>>() {});
            if (selected != null) {
                for (int idx : selected) {
                    if (idx >= 0 && idx < counts.length) counts[idx]++;
                }
            }
        }
        List<Map<String, Object>> optionResults = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", i);
            m.put("text", options.get(i));
            m.put("count", counts[i]);
            optionResults.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("voteId", voteId);
        result.put("title", vote.getTitle());
        result.put("options", optionResults);
        result.put("totalVoters", records.size());
        return result;
    }

    @Override
    @Transactional
    public String startRecording(String meetingId, Long uploaderId) {
        String recordingId = SnowflakeIdGenerator.nextIdStr();
        MeetingRecording recording = new MeetingRecording();
        recording.setRecordingId(recordingId);
        recording.setMeetingId(meetingId);
        recording.setUploaderId(uploaderId);
        recording.setStatus(0);
        recording.setStartTime(LocalDateTime.now());
        recording.setCreateTime(LocalDateTime.now());
        recordingMapper.insert(recording);
        return recordingId;
    }

    @Override
    @Transactional
    public void finishRecording(String recordingId, String minioObjectKey, long fileSize, int duration) {
        MeetingRecording recording = recordingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRecording>()
                        .eq(MeetingRecording::getRecordingId, recordingId));
        if (recording != null) {
            recording.setMinioObjectKey(minioObjectKey);
            recording.setFileSize(fileSize);
            recording.setDuration(duration);
            recording.setStatus(1);
            recording.setEndTime(LocalDateTime.now());
            recordingMapper.updateById(recording);
        }
    }

    @Override
    public String getRecordingUploadUrl(String meetingId, String recordingId) {
        String objectKey = "recordings/" + meetingId + "/" + recordingId + ".webm";
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket("doc-files")
                    .object(objectKey)
                    .method(Method.PUT)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate recording upload URL: {}", e.getMessage());
            throw new RuntimeException("生成录制上传URL失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getRecordings(String meetingId) {
        List<MeetingRecording> recordings = recordingMapper.selectByMeetingId(meetingId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MeetingRecording r : recordings) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("recordingId", r.getRecordingId());
            m.put("status", r.getStatus());
            m.put("fileSize", r.getFileSize());
            m.put("duration", r.getDuration());
            m.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : null);
            result.add(m);
        }
        return result;
    }

    @Override
    public String getPlaybackUrl(String meetingId, String recordingId) {
        MeetingRecording recording = recordingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRecording>()
                        .eq(MeetingRecording::getRecordingId, recordingId));
        if (recording == null || recording.getMinioObjectKey() == null) {
            throw new RuntimeException("录制文件不存在");
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket("doc-files")
                    .object(recording.getMinioObjectKey())
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("生成播放URL失败", e);
        }
    }

    @Override
    @Transactional
    public void deleteRecording(String recordingId) {
        MeetingRecording recording = recordingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRecording>()
                        .eq(MeetingRecording::getRecordingId, recordingId));
        if (recording != null) {
            if (recording.getMinioObjectKey() != null) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket("doc-files")
                            .object(recording.getMinioObjectKey())
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to delete MinIO recording object: {}", e.getMessage());
                }
            }
            recordingMapper.deleteById(recording.getId());
        }
    }

    @Override
    public String getStreamUrl(String meetingId, String recordingId) {
        MeetingRecording recording = recordingMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRecording>()
                        .eq(MeetingRecording::getRecordingId, recordingId));
        if (recording == null || recording.getMinioObjectKey() == null) {
            throw new RuntimeException("录制文件不存在");
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket("doc-files")
                    .object(recording.getMinioObjectKey())
                    .method(Method.GET)
                    .expiry(2, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("生成流播放URL失败", e);
        }
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); } catch (Exception e) { return "[]"; }
    }

    private <T> T fromJson(String json, TypeReference<T> ref) {
        try { return objectMapper.readValue(json, ref); } catch (Exception e) { return null; }
    }
}
