package com.collaboffice.meet.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.meet.dto.MeetingCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MeetingScheduleTest {
 @Test void cancelledMeetingsCannotBeRescheduled(){
  var access=mock(WorkspaceAccess.class);var jdbc=mock(JdbcTemplate.class);
  when(access.row(anyString(),eq("cancelled"))).thenReturn(Map.of("status",4));
  var service=new MeetingScheduleController(access,jdbc,mock(RabbitTemplate.class));
  assertThrows(BusinessException.class,()->service.reschedule("cancelled",new MeetingCreateRequest()));
  verifyNoInteractions(jdbc);
 }
 @Test void backwardsTimeIsRejectedBeforeWrites(){
  var access=mock(WorkspaceAccess.class);var jdbc=mock(JdbcTemplate.class);
  when(access.row(anyString(),eq("scheduled"))).thenReturn(Map.of("status",1));
  var input=new MeetingCreateRequest();input.setStartTime(LocalDateTime.now());input.setEndTime(input.getStartTime().minusMinutes(1));
  var service=new MeetingScheduleController(access,jdbc,mock(RabbitTemplate.class));
  assertThrows(BusinessException.class,()->service.reschedule("scheduled",input));verifyNoInteractions(jdbc);
 }
}
