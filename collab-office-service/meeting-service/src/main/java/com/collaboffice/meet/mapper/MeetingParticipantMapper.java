package com.collaboffice.meet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.meet.entity.MeetingParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MeetingParticipantMapper extends BaseMapper<MeetingParticipant> {
    @Select("select coalesce(real_name, username) from sys_user where id = #{userId} and deleted = 0")
    String getRealNameByUserId(@Param("userId") Long userId);
}

