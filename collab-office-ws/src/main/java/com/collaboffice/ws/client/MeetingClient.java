package com.collaboffice.ws.client;

import com.collaboffice.common.result.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "meeting-service", contextId = "meetingClient")
public interface MeetingClient {
    @PostMapping("/api/v1/meeting/internal/control")
    CommonResult<Boolean> handleMeetingControl(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("meetingId") String meetingId,
            @RequestParam("operatorId") Long operatorId,
            @RequestParam("controlType") String controlType,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId
    );

    /**
     * 校验用户是否有权加入会议房间(供 MEETING_JOIN 时调用)
     */
    @GetMapping("/api/v1/meeting/internal/permission/check")
    CommonResult<Boolean> checkMeetingPermission(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam("meetingId") String meetingId,
            @RequestParam("userId") Long userId
    );
}
