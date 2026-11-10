package com.collaboffice.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.file.entity.FileFolder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileFolderMapper extends BaseMapper<FileFolder> {
    @Select({
            "<script>",
            "SELECT * FROM file_folder",
            "WHERE deleted = 0",
            "<choose>",
            "  <when test='parentId == null'>AND parent_id IS NULL</when>",
            "  <otherwise>AND parent_id = #{parentId}</otherwise>",
            "</choose>",
            "ORDER BY sort_order ASC, create_time DESC",
            "</script>"
    })
    List<FileFolder> selectByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM file_folder WHERE deleted = 0 AND space_id = #{spaceId} ORDER BY sort_order ASC, create_time DESC")
    List<FileFolder> selectBySpaceId(@Param("spaceId") Long spaceId);
}
