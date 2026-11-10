package com.collaboffice.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.file.entity.KnowledgeItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeItemMapper extends BaseMapper<KnowledgeItem> {
    @Select("SELECT * FROM knowledge_item WHERE deleted = 0 AND space_id = #{spaceId} ORDER BY create_time DESC")
    List<KnowledgeItem> selectBySpaceId(@Param("spaceId") Long spaceId);
}
