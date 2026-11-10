package com.collaboffice.meet.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.meet.entity.MeetingRoom;
import com.collaboffice.meet.entity.MeetingParticipant;
import com.collaboffice.meet.entity.MeetingRecord;
import com.collaboffice.meet.entity.MeetingEventLog;
import com.collaboffice.meet.mapper.MeetingRoomMapper;
import com.collaboffice.meet.mapper.MeetingParticipantMapper;
import com.collaboffice.meet.mapper.MeetingRecordMapper;
import com.collaboffice.meet.mapper.MeetingEventLogMapper;
import com.collaboffice.meet.service.MeetingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingRoomMapper meetingRoomMapper;
    private final MeetingParticipantMapper participantMapper;
    private final MeetingRecordMapper recordMapper;
    private final MeetingEventLogMapper eventLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final String MEETING_EXCHANGE = "meeting-event-exchange";
    private static final String MEETING_KEY = "meeting.event";

    @Override
    @Transactional
    public String createMeeting(Long userId, String title, Date startTime) {
        MeetingRoom room = new MeetingRoom();
        room.setRoomId(SnowflakeIdGenerator.nextIdStr());
        room.setTitle(title);
        room.setHostId(userId);
        if (startTime != null) {
            room.setStartTime(new java.sql.Timestamp(startTime.getTime()).toLocalDateTime());
        }
        room.setStatus(MeetingRoom.SCHEDULED);
        meetingRoomMapper.insert(room);

        MeetingParticipant p = new MeetingParticipant();
        p.setMeetingId(room.getRoomId());
        p.setUserId(userId);
        p.setRoleType(1);
        p.setJoinTime(LocalDateTime.now());
        p.setUserName(participantMapper.getRealNameByUserId(userId));
        participantMapper.insert(p);

        return room.getRoomId();
    }

    @Override
    public List<Map<String, Object>> getMeetingList(Long userId) {
        List<String> ids = participantMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingParticipant>().eq(MeetingParticipant::getUserId,userId)).stream().map(MeetingParticipant::getMeetingId).toList();
        if (ids.isEmpty()) return List.of();
        List<MeetingRoom> rooms = meetingRoomMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRoom>().in(MeetingRoom::getRoomId,ids).orderByDesc(MeetingRoom::getStartTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MeetingRoom r : rooms) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("meetingId", r.getRoomId());
            map.put("title", r.getTitle());
            map.put("status", r.getStatus());
            map.put("hostId", r.getHostId());
            map.put("startTime", r.getStartTime());
            map.put("endTime", r.getEndTime());
            map.put("agenda", r.getAgenda());
            result.add(map);
        }
        return result;
    }

    @Override
    public Map<String, Object> getMeetingDetail(String meetingId) {
        MeetingRoom room = meetingRoomMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getRoomId, meetingId));
        if (room == null) return Collections.emptyMap();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("meetingId", room.getRoomId());
        detail.put("title", room.getTitle());
        detail.put("hostId", room.getHostId());
        detail.put("status", room.getStatus());
        detail.put("startTime", room.getStartTime());
        detail.put("endTime", room.getEndTime());
        detail.put("agenda", room.getAgenda());
        detail.put("participants", participantMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingParticipant>().eq(MeetingParticipant::getMeetingId,meetingId)));
        return detail;
    }

    @Override
    @Transactional
    public void joinMeeting(String meetingId, Long userId) {
        MeetingParticipant p = participantMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingParticipant>()
                        .eq(MeetingParticipant::getMeetingId, meetingId)
                        .eq(MeetingParticipant::getUserId, userId)
                        .isNull(MeetingParticipant::getLeaveTime)
                        .last("limit 1"));
        if (p == null) {
            p = new MeetingParticipant();
            p.setMeetingId(meetingId);
            p.setUserId(userId);
            p.setRoleType(3);
            p.setJoinTime(LocalDateTime.now());
            p.setUserName(participantMapper.getRealNameByUserId(userId));
            participantMapper.insert(p);
        }
        broadcastEvent(meetingId, "user_joined", userId);
    }

    @Override
    @Transactional
    public void leaveMeeting(String meetingId, Long userId) {
        MeetingParticipant p = participantMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingParticipant>()
                        .eq(MeetingParticipant::getMeetingId, meetingId)
                        .eq(MeetingParticipant::getUserId, userId)
                        .isNull(MeetingParticipant::getLeaveTime)
                        .last("limit 1"));
        if (p != null) {
            p.setLeaveTime(LocalDateTime.now());
            long seconds = java.time.Duration.between(p.getJoinTime(), p.getLeaveTime()).getSeconds();
            p.setDuration((int) seconds);
            participantMapper.updateById(p);
        }
        broadcastEvent(meetingId, "user_left", userId);
    }

    private void broadcastEvent(String meetingId, String event, Long userId) {
        try {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("meetingId", meetingId);
            msg.put("event", event);
            msg.put("userId", userId);
            msg.put("timestamp", System.currentTimeMillis());
            rabbitTemplate.convertAndSend(MEETING_EXCHANGE, MEETING_KEY, msg);
        } catch (Exception e) {
            log.warn("Failed to publish meeting event: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void cancelMeeting(Long userId, String meetingId) {
        MeetingRoom room = meetingRoomMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getRoomId, meetingId));
        if (room != null) {
            if (!Objects.equals(room.getHostId(), userId)) {
                throw new com.collaboffice.common.exception.BusinessException(403, "只有主持人可以取消会议");
            }
            room.setStatus(MeetingRoom.CANCELLED);
            meetingRoomMapper.updateById(room);
        }
    }

    @Override
    public Map<String, Object> getMeetingMinutes(String meetingId) {
        MeetingRecord record = recordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRecord>()
                        .eq(MeetingRecord::getMeetingId, meetingId));
        Map<String, Object> result = new LinkedHashMap<>();
        if (record == null) {
            result.put("meetingId", meetingId);
            result.put("minuteContent", "");
            result.put("todoListJson", Collections.emptyList());
            return result;
        }
        result.put("meetingId", record.getMeetingId());
        result.put("minuteContent", record.getMinuteContent() != null ? record.getMinuteContent() : "");
        try {
            if (record.getTodoListJson() != null && !record.getTodoListJson().isEmpty()) {
                result.put("todoListJson", objectMapper.readValue(record.getTodoListJson(), List.class));
            } else {
                result.put("todoListJson", Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Failed to parse todoListJson: {}", e.getMessage());
            result.put("todoListJson", Collections.emptyList());
        }
        return result;
    }

    @Override
    @Transactional
    public void saveMeetingMinutes(String meetingId, String minuteContent, String todoListJson) {
        MeetingRecord record = recordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRecord>()
                        .eq(MeetingRecord::getMeetingId, meetingId));
        if (record == null) {
            record = new MeetingRecord();
            record.setMeetingId(meetingId);
            record.setMinuteContent(minuteContent);
            record.setTodoListJson(todoListJson);
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        } else {
            record.setMinuteContent(minuteContent);
            record.setTodoListJson(todoListJson);
            recordMapper.updateById(record);
        }
    }

    @Override
    public boolean handleMeetingControl(String meetingId, Long operatorId, String controlType, Long targetUserId) {
        MeetingRoom room = meetingRoomMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getRoomId, meetingId));
        if (room == null || !room.getHostId().equals(operatorId)) {
            log.warn("Meeting control denied: operator={} is not host of meeting={}", operatorId, meetingId);
            return false;
        }

        // Log the event
        MeetingEventLog eventLog = new MeetingEventLog();
        eventLog.setMeetingId(meetingId);
        eventLog.setOperatorId(operatorId);
        eventLog.setEventType(controlType);
        eventLog.setTargetUserId(targetUserId);
        eventLog.setEventTime(LocalDateTime.now());
        eventLogMapper.insert(eventLog);

        // Broadcast control to room
        try {
            Map<String, Object> control = new LinkedHashMap<>();
            control.put("meetingId", meetingId);
            control.put("event", controlType);
            control.put("operatorId", operatorId);
            control.put("targetUserId", targetUserId);
            control.put("timestamp", System.currentTimeMillis());
            rabbitTemplate.convertAndSend(MEETING_EXCHANGE, MEETING_KEY, control);
        } catch (Exception e) {
            log.warn("Failed to broadcast meeting control: {}", e.getMessage());
        }

        log.info("Meeting control executed: {} by host={} on user={} in meeting={}",
                controlType, operatorId, targetUserId, meetingId);
        return true;
    }
}
