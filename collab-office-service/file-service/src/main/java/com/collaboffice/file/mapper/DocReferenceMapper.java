package com.collaboffice.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.file.entity.DocReference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocReferenceMapper extends BaseMapper<DocReference> {
    @Select("SELECT * FROM doc_reference WHERE source_doc_id = #{docId}")
    List<DocReference> selectOutbound(@Param("docId") String docId);

    @Select("SELECT * FROM doc_reference WHERE target_doc_id = #{docId}")
    List<DocReference> selectInbound(@Param("docId") String docId);
}
