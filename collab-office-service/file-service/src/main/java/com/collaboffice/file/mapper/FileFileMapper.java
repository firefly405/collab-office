package com.collaboffice.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.file.entity.FileFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FileFileMapper extends BaseMapper<FileFile> {
    @Select({
            "<script>",
            "SELECT * FROM file_file",
            "WHERE deleted = 0 AND status = 1",
            "<choose>",
            "  <when test='folderId == null'>AND folder_id IS NULL</when>",
            "  <otherwise>AND folder_id = #{folderId}</otherwise>",
            "</choose>",
            "ORDER BY create_time DESC",
            "</script>"
    })
    List<FileFile> selectByFolderId(@Param("folderId") Long folderId);

    @Select("SELECT * FROM file_file WHERE deleted = 0 AND status = 1 AND space_id = #{spaceId} ORDER BY create_time DESC")
    List<FileFile> selectBySpaceId(@Param("spaceId") Long spaceId);

    @Select("SELECT * FROM file_file WHERE deleted = 0 AND status = 1 AND file_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY create_time DESC")
    List<FileFile> searchByKeyword(@Param("keyword") String keyword);
}
