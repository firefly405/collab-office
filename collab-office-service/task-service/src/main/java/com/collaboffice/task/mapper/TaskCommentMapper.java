package com.collaboffice.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.task.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {
    @Select("select * from task_comment where task_id = #{taskId} order by create_time asc")
    List<TaskComment> selectByTaskId(@Param("taskId") String taskId);

    @Select("select coalesce(real_name, username) from sys_user where id = #{userId} and deleted = 0")
    String getRealNameByUserId(@Param("userId") Long userId);
}

