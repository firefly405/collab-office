package com.collaboffice.meet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.meet.entity.MeetingVote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MeetingVoteMapper extends BaseMapper<MeetingVote> {
    @Select("SELECT * FROM meeting_vote WHERE meeting_id = #{meetingId} ORDER BY create_time DESC")
    List<MeetingVote> selectByMeetingId(@Param("meetingId") String meetingId);
}
