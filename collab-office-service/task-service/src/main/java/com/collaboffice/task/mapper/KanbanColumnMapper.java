package com.collaboffice.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.task.entity.KanbanColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface KanbanColumnMapper extends BaseMapper<KanbanColumn> {
    @Select("SELECT id FROM task_kanban_column WHERE id=#{columnId} AND deleted=0 FOR UPDATE")
    Long lockColumn(Long columnId);

    @Select("SELECT COUNT(*) FROM task_task WHERE column_id=#{columnId} AND deleted=0")
    int countTasks(Long columnId);

    @Select("SELECT * FROM task_kanban_column WHERE project_id = #{projectId} AND deleted = 0 ORDER BY sort_order ASC")
    List<KanbanColumn> selectByProjectId(String projectId);

    @Select("SELECT COUNT(*) FROM task_task WHERE column_id = #{columnId} AND status < 2 AND deleted = 0")
    int countActiveTasksInColumn(Long columnId);
}
