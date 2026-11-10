package com.collaboffice.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    @Select("<script>" +
            "SELECT * FROM notification " +
            "WHERE user_id = #{userId} " +
            "<if test=\"type != null\">" +
            "  AND type = #{type} " +
            "</if>" +
            "<if test=\"isRead != null\">" +
            "  AND is_read = #{isRead} " +
            "</if>" +
            "ORDER BY send_time DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<Notification> selectByUserId(@Param("userId") Long userId,
                                      @Param("type") Integer type,
                                      @Param("isRead") Integer isRead,
                                      @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    Integer countUnread(@Param("userId") Long userId);
}
