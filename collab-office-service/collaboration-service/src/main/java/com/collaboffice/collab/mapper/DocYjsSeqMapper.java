package com.collaboffice.collab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.collab.entity.DocYjsSeq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DocYjsSeqMapper extends BaseMapper<DocYjsSeq> {

    /**
     * 原子地分配下一个 seq：若不存在则插入 1，否则自增并返回新值。
     * 利用 MySQL 的 INSERT ... ON DUPLICATE KEY UPDATE 实现原子 upsert。
     */
    @Update("INSERT INTO doc_yjs_seq (doc_id, seq) VALUES (#{docId}, 1) " +
            "ON DUPLICATE KEY UPDATE seq = seq + 1")
    int upsertAndIncrement(@Param("docId") String docId);
}
