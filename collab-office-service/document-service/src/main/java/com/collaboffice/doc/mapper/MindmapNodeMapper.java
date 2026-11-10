package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.MindmapNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MindmapNodeMapper extends BaseMapper<MindmapNode> {

    @Select("SELECT * FROM mindmap_node WHERE map_id = #{mapId} ORDER BY sort_order ASC, id ASC")
    List<MindmapNode> selectByMapId(@Param("mapId") String mapId);

    @Select("SELECT * FROM mindmap_node WHERE parent_id = #{parentId} ORDER BY sort_order ASC, id ASC")
    List<MindmapNode> selectChildrenByParentId(@Param("parentId") Long parentId);
}
