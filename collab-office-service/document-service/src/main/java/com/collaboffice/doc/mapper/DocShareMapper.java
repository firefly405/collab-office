package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.DocShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocShareMapper extends BaseMapper<DocShare> {

    @Select("""
            SELECT *
            FROM doc_share
            WHERE share_token = #{shareToken}
              AND status = 1
            LIMIT 1
            """)
    DocShare selectByShareToken(@Param("shareToken") String shareToken);

    @Select("""
            SELECT *
            FROM doc_share
            WHERE doc_id = #{docId}
              AND status = 1
              AND (expire_time IS NULL OR expire_time > NOW())
            ORDER BY create_time DESC
            """)
    List<DocShare> selectValidShares(@Param("docId") String docId);
}
