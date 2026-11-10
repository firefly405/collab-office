package com.collaboffice.msg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.msg.entity.ChatSessionMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatSessionMemberMapper extends BaseMapper<ChatSessionMember> {
    @Select("SELECT * FROM chat_session_member WHERE session_id = #{sessionId}")
    List<ChatSessionMember> selectBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM chat_session_member WHERE user_id = #{userId}")
    List<ChatSessionMember> selectByUserId(@Param("userId") Long userId);
}
