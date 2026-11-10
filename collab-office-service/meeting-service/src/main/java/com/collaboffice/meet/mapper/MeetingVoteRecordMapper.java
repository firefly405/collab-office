package com.collaboffice.meet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.meet.entity.MeetingVoteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MeetingVoteRecordMapper extends BaseMapper<MeetingVoteRecord> {
    @Select("SELECT * FROM meeting_vote_record WHERE vote_id = #{voteId}")
    List<MeetingVoteRecord> selectByVoteId(@Param("voteId") String voteId);

    @Select("SELECT * FROM meeting_vote_record WHERE vote_id = #{voteId} AND user_id = #{userId}")
    MeetingVoteRecord selectByVoteAndUser(@Param("voteId") String voteId, @Param("userId") Long userId);
}
