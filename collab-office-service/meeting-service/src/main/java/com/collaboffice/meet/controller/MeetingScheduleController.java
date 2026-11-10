package com.collaboffice.meet.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.meet.dto.MeetingCreateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/meeting")
@RequiredArgsConstructor
public class MeetingScheduleController {
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;

    @PutMapping("/{id}/schedule")
    @Transactional
    public CommonResult<Void> reschedule(@PathVariable String id,@Valid @RequestBody MeetingCreateRequest body) {
        access.requireMeeting(id,true);
        var room=access.row("SELECT status FROM meeting_room WHERE room_id=? AND deleted=0 FOR UPDATE",id);
        if(!Set.of(1,2).contains(((Number)room.get("status")).intValue()))throw new BusinessException(409,"已结束或取消的会议不能改期");
        if(!body.getEndTime().isAfter(body.getStartTime()))throw new BusinessException(400,"结束时间必须晚于开始时间");
        Long space=access.requireSpace(access.currentSpace(),true);
        Set<Long> people=new LinkedHashSet<>(body.getParticipantIds()==null?List.of():body.getParticipantIds());
        people.add(access.userId());
        for(Long person:people)if(jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=? AND (expires_at IS NULL OR expires_at>NOW())",Integer.class,space,person)==0)throw new BusinessException(400,"参与人必须为有效团队成员");
        jdbc.update("UPDATE meeting_room SET title=?,agenda=?,start_time=?,end_time=?,status=1,update_time=NOW() WHERE room_id=?",body.getTitle().trim(),body.getAgenda(),body.getStartTime(),body.getEndTime(),id);
        for(Long person:people){
            if(jdbc.queryForObject("SELECT COUNT(*) FROM meeting_participant WHERE meeting_id=? AND user_id=?",Integer.class,id,person)==0)
                jdbc.update("INSERT INTO meeting_participant(meeting_id,user_id,user_name,role_type) SELECT ?,id,COALESCE(real_name,username),2 FROM sys_user WHERE id=? AND deleted=0",id,person);
            if(!person.equals(access.userId()))rabbit.convertAndSend("notification-exchange","notification.meeting",new HashMap<>(Map.of("userId",person,"type",4,"title","会议安排更新","content",body.getTitle()+" · "+body.getStartTime(),"relatedId",id)));
        }
        // Preserve attendance history. Existing invitees remain visible and may be re-invited.
        return CommonResult.success(null);
    }
}
