package com.collaboffice.msg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.msg.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
    @Select("SELECT s.* FROM chat_session s " +
            "INNER JOIN chat_session_member m ON s.session_id = m.session_id " +
            "WHERE m.user_id = #{userId} AND s.status = 1 " +
            "ORDER BY s.last_msg_time DESC")
    List<ChatSession> selectByUserId(@Param("userId") Long userId);
}
