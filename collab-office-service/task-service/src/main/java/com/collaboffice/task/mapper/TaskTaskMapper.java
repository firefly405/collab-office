package com.collaboffice.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.task.entity.TaskTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TaskTaskMapper extends BaseMapper<TaskTask> {
    @Select("SELECT * FROM task_task WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order, create_time DESC")
    List<TaskTask> selectByProjectId(@Param("projectId") String projectId);

    @Select("SELECT * FROM task_task WHERE assignee_id = #{assigneeId} AND deleted = 0 ORDER BY due_date, create_time DESC")
    List<TaskTask> selectByAssigneeId(@Param("assigneeId") Long assigneeId);
}
