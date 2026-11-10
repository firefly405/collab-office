package com.collaboffice.meet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.meet.entity.MeetingRecording;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MeetingRecordingMapper extends BaseMapper<MeetingRecording> {
    @Select("SELECT * FROM meeting_recording WHERE meeting_id = #{meetingId} ORDER BY create_time DESC")
    List<MeetingRecording> selectByMeetingId(@Param("meetingId") String meetingId);
}
