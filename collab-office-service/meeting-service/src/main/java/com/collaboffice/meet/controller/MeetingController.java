package com.collaboffice.meet.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.meet.entity.MeetingRoom;
import com.collaboffice.meet.mapper.MeetingRoomMapper;
import com.collaboffice.meet.mapper.MeetingParticipantMapper;
import com.collaboffice.meet.entity.MeetingParticipant;
import com.collaboffice.meet.service.MeetingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/meeting")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;
    private final ObjectMapper objectMapper;
    private final MeetingRoomMapper meetingRoomMapper;
    private final MeetingParticipantMapper participantMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @Value("${app.internal-token:default-secret-token}")
    private String internalToken;

    @PostMapping("/create")
    @org.springframework.transaction.annotation.Transactional
    public CommonResult<String> create(@RequestHeader("X-User-Id") Long userId,
                                       @jakarta.validation.Valid @RequestBody com.collaboffice.meet.dto.MeetingCreateRequest request) {
        if (!request.getEndTime().isAfter(request.getStartTime())) throw new com.collaboffice.common.exception.BusinessException(400, "结束时间必须晚于开始时间");
        Long space = access.requireSpace(access.currentSpace(), true);
        Set<Long> people = new LinkedHashSet<>(request.getParticipantIds() == null ? List.of() : request.getParticipantIds());
        people.add(userId);
        for (Long person : people) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=?", Integer.class, space, person) == 0) throw new com.collaboffice.common.exception.BusinessException(400, "参与人必须属于当前团队");
        }
        String id = meetingService.createMeeting(userId, request.getTitle(), java.sql.Timestamp.valueOf(request.getStartTime()));
        MeetingRoom room = meetingRoomMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRoom>().eq(MeetingRoom::getRoomId,id));
        room.setAgenda(request.getAgenda()); room.setEndTime(request.getEndTime()); room.setStatus(MeetingRoom.SCHEDULED);
        meetingRoomMapper.updateById(room);
        for (Long person : people) {
            if (person.equals(userId)) continue;
            MeetingParticipant participant = new MeetingParticipant();
            participant.setMeetingId(id); participant.setUserId(person); participant.setRoleType(2);
            participant.setUserName(participantMapper.getRealNameByUserId(person));
            participantMapper.insert(participant);
            rabbit.convertAndSend("notification-exchange","notification.meeting",new java.util.HashMap<>(Map.of("userId",person,"type",4,"title","会议邀请","content","您受邀参加："+request.getTitle(),"relatedId",id)));
        }
        return CommonResult.success(id);
    }

    @GetMapping("/list")
    public CommonResult<List<Map<String, Object>>> list(@RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(meetingService.getMeetingList(userId));
    }

    @GetMapping("/{meetingId}")
    public CommonResult<Map<String, Object>> detail(@PathVariable String meetingId) {
        return CommonResult.success(meetingService.getMeetingDetail(meetingId));
    }

    @PostMapping("/{meetingId}/join")
    public CommonResult<Void> join(@RequestHeader("X-User-Id") Long userId, @PathVariable String meetingId) {
        meetingService.joinMeeting(meetingId, userId);
        return CommonResult.success(null);
    }

    @PostMapping("/{meetingId}/leave")
    public CommonResult<Void> leave(@RequestHeader("X-User-Id") Long userId, @PathVariable String meetingId) {
        meetingService.leaveMeeting(meetingId, userId);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{meetingId}")
    public CommonResult<Void> cancel(@RequestHeader("X-User-Id") Long userId, @PathVariable String meetingId) {
        meetingService.cancelMeeting(userId, meetingId);
        return CommonResult.success(null);
    }

    @GetMapping("/{meetingId}/minutes")
    public CommonResult<Map<String, Object>> getMinutes(@PathVariable String meetingId) {
        var data=meetingService.getMeetingMinutes(meetingId);
        data.put("revision",com.collaboffice.common.util.MeetingTodos.revision(data.get("minuteContent"),data.get("todoListJson")));
        return CommonResult.success(data);
    }

    @PutMapping("/{meetingId}/minutes")
    @org.springframework.transaction.annotation.Transactional
    public CommonResult<Void> updateMinutes(@PathVariable String meetingId,
                                            @RequestBody MeetingMinutesRequest request) {
        access.requireMeeting(meetingId,true);
        jdbc.queryForList("SELECT id FROM meeting_room WHERE room_id=? AND deleted=0 FOR UPDATE",meetingId);
        var old=meetingService.getMeetingMinutes(meetingId);
        if(!Objects.equals(request.getRevision(),com.collaboffice.common.util.MeetingTodos.revision(old.get("minuteContent"),old.get("todoListJson"))))throw new com.collaboffice.common.exception.BusinessException(409,"纪要已更新，请重新打开后编辑");
        if(request.getMinuteContent()==null||request.getMinuteContent().length()>20000)throw new com.collaboffice.common.exception.BusinessException(400,"纪要最多20000字");
        var todos=com.collaboffice.common.util.MeetingTodos.normalize(request.getTodoListJson(),(List<?>)old.get("todoListJson"),access.currentSpace());
        for(var todo:todos)if(!todo.get("assigneeId").toString().isBlank()&&jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=? AND (expires_at IS NULL OR expires_at>NOW())",Integer.class,todo.get("spaceId"),todo.get("assigneeId"))==0)throw new com.collaboffice.common.exception.BusinessException(400,"负责人须为有效团队成员");
        meetingService.saveMeetingMinutes(meetingId, request.getMinuteContent(),com.alibaba.fastjson2.JSON.toJSONString(todos));
        return CommonResult.success(null);
    }

    @Data
    public static class MeetingMinutesRequest {
        private String minuteContent;
        private List<Object> todoListJson;
        private String revision;
    }

    @PostMapping("/internal/control")
    public CommonResult<Boolean> handleMeetingControl(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestParam String meetingId,
            @RequestParam Long operatorId,
            @RequestParam String controlType,
            @RequestParam(required = false) Long targetUserId) {
        if (!internalToken.equals(token)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "未授权的内部访问");
        }
        boolean success = meetingService.handleMeetingControl(meetingId, operatorId, controlType, targetUserId);
        return CommonResult.success(success);
    }

    /**
     * 内部接口: 校验用户是否有权加入会议房间(供 ws-service 在 MEETING_JOIN 时调用)
     * 通过条件: 主持人 / 已加入会议的参与者
     */
    @GetMapping("/internal/permission/check")
    public CommonResult<Boolean> checkMeetingPermission(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String meetingId,
            @RequestParam Long userId) {
        if (!internalToken.equals(token)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "未授权的内部访问");
        }
        // 1. 会议是否存在
        MeetingRoom room = meetingRoomMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getRoomId, meetingId)
                        .last("LIMIT 1"));
        if (room == null) {
            return CommonResult.success(false);
        }
        // 2. 主持人直接放行
        if (room.getHostId() != null && room.getHostId().equals(userId)) {
            return CommonResult.success(true);
        }
        // 3. 已是参与者(未离场)放行
        MeetingParticipant p = participantMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MeetingParticipant>()
                        .eq(MeetingParticipant::getMeetingId, meetingId)
                        .eq(MeetingParticipant::getUserId, userId)
                        .isNull(MeetingParticipant::getLeaveTime)
                        .last("LIMIT 1"));
        return CommonResult.success(p != null);
    }
}

