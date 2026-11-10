package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.WhiteboardTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface WhiteboardTemplateMapper extends BaseMapper<WhiteboardTemplate> {
    @Select("SELECT * FROM whiteboard_template WHERE category = #{category} OR #{category} IS NULL")
    List<WhiteboardTemplate> selectByCategory(@Param("category") String category);
}
