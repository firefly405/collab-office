package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    @Select("SELECT * FROM sys_dept WHERE space_id = #{spaceId} AND deleted = 0 ORDER BY dept_sort, id")
    List<SysDept> selectBySpaceId(@Param("spaceId") Long spaceId);

    @Select("SELECT * FROM sys_dept WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY dept_sort, id")
    List<SysDept> selectChildrenByParentId(@Param("parentId") Long parentId);
}
