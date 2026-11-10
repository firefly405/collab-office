package com.collaboffice.meet.service;

import java.util.List;
import java.util.Map;

public interface MeetingInteractionService {
    void raiseHand(String meetingId, Long userId, boolean isRaised);
    String createVote(String meetingId, Long creatorId, String title, List<String> options);
    void submitVote(String voteId, Long userId, List<Integer> selectedOptionIndexes);
    Map<String, Object> getVoteResult(String voteId);
    String startRecording(String meetingId, Long uploaderId);
    void finishRecording(String recordingId, String minioObjectKey, long fileSize, int duration);
    String getRecordingUploadUrl(String meetingId, String recordingId);
    List<Map<String, Object>> getRecordings(String meetingId);
    String getPlaybackUrl(String meetingId, String recordingId);
    void deleteRecording(String recordingId);
    String getStreamUrl(String meetingId, String recordingId);
}
