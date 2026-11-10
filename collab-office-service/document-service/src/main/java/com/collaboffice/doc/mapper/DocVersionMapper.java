package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.DocVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocVersionMapper extends BaseMapper<DocVersion> {

    @Select("""
            SELECT *
            FROM doc_version
            WHERE doc_id = #{docId}
            ORDER BY version DESC
            LIMIT #{limit}
            """)
    List<DocVersion> selectLatestVersions(@Param("docId") String docId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM doc_version
            WHERE doc_id = #{docId}
              AND is_snapshot = 1
            ORDER BY create_time DESC
            """)
    List<DocVersion> selectSnapshotsOnly(@Param("docId") String docId);
}
