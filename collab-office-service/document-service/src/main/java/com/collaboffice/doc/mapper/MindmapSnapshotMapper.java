package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.MindmapSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MindmapSnapshotMapper extends BaseMapper<MindmapSnapshot> {
    @Select("SELECT * FROM mindmap_snapshot WHERE mindmap_id = #{mindmapId} ORDER BY created_at DESC, id DESC")
    List<MindmapSnapshot> selectByMindmapId(@Param("mindmapId") String mindmapId);
}
