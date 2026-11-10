package com.collaboffice.msg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.msg.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    @Select("SELECT * FROM chat_message WHERE session_id=#{sessionId} AND status=1 ORDER BY send_time DESC,msg_id DESC LIMIT #{size} OFFSET #{offset}")
    List<ChatMessage> selectPageHistory(@Param("sessionId") String sessionId,@Param("size") int size,@Param("offset") int offset);
    @Select("<script>" +
            "SELECT * FROM chat_message " +
            "WHERE session_id = #{sessionId} AND status = 1 " +
            "<if test=\"beforeMsgId != null and beforeMsgId != ''\">" +
            "  AND send_time &lt; (SELECT send_time FROM chat_message WHERE msg_id = #{beforeMsgId}) " +
            "</if>" +
            "ORDER BY send_time DESC " +
            "LIMIT #{pageSize}" +
            "</script>")
    List<ChatMessage> selectHistory(@Param("sessionId") String sessionId,
                                    @Param("beforeMsgId") String beforeMsgId,
                                    @Param("pageSize") int pageSize);

    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND status = 1 ORDER BY send_time DESC LIMIT 1")
    ChatMessage selectLastMsg(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM chat_message WHERE content LIKE CONCAT('%',#{keyword},'%') AND status = 1 ORDER BY send_time DESC LIMIT #{limit}")
    List<ChatMessage> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);
}
