package com.collaboffice.meet.service;

import java.util.*;

public interface MeetingService {
    String createMeeting(Long userId, String title, Date startTime);
    List<Map<String, Object>> getMeetingList(Long userId);
    Map<String, Object> getMeetingDetail(String meetingId);
    void joinMeeting(String meetingId, Long userId);
    void leaveMeeting(String meetingId, Long userId);
    void cancelMeeting(Long userId, String meetingId);
    Map<String, Object> getMeetingMinutes(String meetingId);
    void saveMeetingMinutes(String meetingId, String minuteContent, String todoListJson);
    boolean handleMeetingControl(String meetingId, Long operatorId, String controlType, Long targetUserId);
}

