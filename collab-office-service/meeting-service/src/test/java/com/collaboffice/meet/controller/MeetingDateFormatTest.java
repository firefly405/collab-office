package com.collaboffice.meet.controller;

import com.collaboffice.common.config.JacksonConfig;
import com.collaboffice.meet.dto.MeetingCreateRequest;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeetingDateFormatTest {
    @Test void acceptsDatePickerIsoDateWithSharedMapper() throws Exception {
        var request = new JacksonConfig().objectMapper().readValue(
                "{\"title\":\"Planning\",\"startTime\":\"2026-10-16T09:00:00\",\"endTime\":\"2026-10-16T10:00:00\"}",
                MeetingCreateRequest.class);
        assertEquals(LocalDateTime.of(2026,10,16,9,0), request.getStartTime());
    }
}
