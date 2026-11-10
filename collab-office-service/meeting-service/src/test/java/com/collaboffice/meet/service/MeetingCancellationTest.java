package com.collaboffice.meet.service;

import com.collaboffice.meet.entity.MeetingRoom;
import com.collaboffice.meet.mapper.*;
import com.collaboffice.meet.service.impl.MeetingServiceImpl;
import com.collaboffice.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MeetingCancellationTest {
    private final MeetingRoomMapper rooms = mock(MeetingRoomMapper.class);
    private final MeetingServiceImpl service = new MeetingServiceImpl(rooms,
            mock(MeetingParticipantMapper.class), mock(MeetingRecordMapper.class),
            mock(MeetingEventLogMapper.class), mock(RabbitTemplate.class), new ObjectMapper());

    @Test
    void cancellationUsesExistingDatabaseStateAndIsIdempotent() {
        MeetingRoom room = new MeetingRoom();
        room.setHostId(1L);
        room.setStatus(MeetingRoom.SCHEDULED);
        when(rooms.selectOne(any())).thenReturn(room);
        service.cancelMeeting(1L, "fixture");
        assertEquals(4, room.getStatus());
        service.cancelMeeting(1L, "fixture");
        assertEquals(MeetingRoom.CANCELLED, room.getStatus());
        verify(rooms, times(2)).updateById(room);
    }

    @Test
    void participantCannotCancelHostsMeeting() {
        MeetingRoom room = new MeetingRoom();
        room.setHostId(1L);
        room.setStatus(MeetingRoom.SCHEDULED);
        when(rooms.selectOne(any())).thenReturn(room);
        assertThrows(BusinessException.class, () -> service.cancelMeeting(2L, "fixture"));
        assertEquals(MeetingRoom.SCHEDULED, room.getStatus());
        verify(rooms, never()).updateById(any(MeetingRoom.class));
    }
}
