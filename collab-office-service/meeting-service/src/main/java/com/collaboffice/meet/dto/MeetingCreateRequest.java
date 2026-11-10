package com.collaboffice.meet.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MeetingCreateRequest {
    @NotBlank @Size(max=200) private String title;
    @Size(max=5000) private String agenda;
    @NotNull @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime startTime;
    @NotNull @com.fasterxml.jackson.annotation.JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss") private LocalDateTime endTime;
    @Size(max=100) private List<Long> participantIds;
}
