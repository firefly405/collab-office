package com.collaboffice.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.task.entity.TaskDependency;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TaskDependencyMapper extends BaseMapper<TaskDependency> {

    @Select("SELECT * FROM task_dependency WHERE task_id = #{taskId}")
    List<TaskDependency> selectByTaskId(String taskId);

    @Select("SELECT * FROM task_dependency WHERE depends_on_task_id = #{taskId}")
    List<TaskDependency> selectByDependsOnTaskId(String taskId);

    @Select("SELECT COUNT(*) FROM task_dependency d " +
            "JOIN task_task t ON d.depends_on_task_id = t.task_id " +
            "WHERE d.task_id = #{taskId} AND t.status <> 2 AND t.deleted = 0")
    int countUnfinishedDependencies(String taskId);
}
