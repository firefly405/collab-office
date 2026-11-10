package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.DocContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocContentMapper extends BaseMapper<DocContent> {

    @Select("""
            SELECT *
            FROM doc_content
            WHERE doc_id = #{docId}
            ORDER BY version DESC
            LIMIT 1
            """)
    DocContent selectLatestByDocId(@Param("docId") String docId);

    @Select("""
            SELECT *
            FROM doc_content
            WHERE doc_id = #{docId}
              AND version = #{version}
            LIMIT 1
            """)
    DocContent selectByDocIdAndVersion(@Param("docId") String docId, @Param("version") Long version);
}
