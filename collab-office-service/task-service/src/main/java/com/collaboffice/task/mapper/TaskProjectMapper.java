package com.collaboffice.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.task.entity.TaskProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface TaskProjectMapper extends BaseMapper<TaskProject> {
    @Select("SELECT * FROM task_project WHERE space_id = #{spaceId} AND deleted = 0 ORDER BY create_time DESC")
    List<TaskProject> selectBySpaceId(@Param("spaceId") Long spaceId);
}
