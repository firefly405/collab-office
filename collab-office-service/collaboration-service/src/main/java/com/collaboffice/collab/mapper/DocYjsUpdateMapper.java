package com.collaboffice.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.collab.entity.DocYjsUpdate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocYjsUpdateMapper extends BaseMapper<DocYjsUpdate> {

    /**
     * 拉取某个文档在指定 seq 之后的全部 updates（按 seq 升序）。
     * 用于新客户端 join 时做增量同步。
     */
    @Select("SELECT * FROM doc_yjs_update WHERE doc_id = #{docId} AND seq > #{afterSeq} ORDER BY seq ASC LIMIT #{limit}")
    List<DocYjsUpdate> selectAfterSeq(@Param("docId") String docId,
                                       @Param("afterSeq") long afterSeq,
                                       @Param("limit") int limit);

    /**
     * 拉取文档的全部 updates（按 seq 升序），用于首次 join 时的全量同步。
     */
    @Select("SELECT * FROM doc_yjs_update WHERE doc_id = #{docId} ORDER BY seq ASC LIMIT #{limit}")
    List<DocYjsUpdate> selectAll(@Param("docId") String docId, @Param("limit") int limit);

    /**
     * 查询文档当前最大 seq（用于初始化 seq 计数器）。
     */
    @Select("SELECT COALESCE(MAX(seq), 0) FROM doc_yjs_update WHERE doc_id = #{docId}")
    Long selectMaxSeq(@Param("docId") String docId);
}
