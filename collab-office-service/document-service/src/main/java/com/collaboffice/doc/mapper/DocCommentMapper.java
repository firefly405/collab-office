package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.DocComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocCommentMapper extends BaseMapper<DocComment> {

    @Select("""
            SELECT *, selection_range_json AS selection_range
            FROM doc_comment
            WHERE doc_id = #{docId}
              AND deleted = 0
            ORDER BY create_time ASC, id ASC
            """)
    List<DocComment> selectByDocId(@Param("docId") String docId);

    @Select("""
            SELECT *, selection_range_json AS selection_range
            FROM doc_comment
            WHERE doc_id = #{docId}
              AND is_resolved = 0
              AND deleted = 0
            ORDER BY create_time ASC, id ASC
            """)
    List<DocComment> selectUnresolved(@Param("docId") String docId);
}
