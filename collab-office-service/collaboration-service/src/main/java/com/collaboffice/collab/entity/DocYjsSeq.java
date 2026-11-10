package com.collaboffice.collab.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档 Yjs seq 计数器，用于为每个 update 分配单调递增的 seq。
 */
@Data
@TableName("doc_yjs_seq")
public class DocYjsSeq {

    @TableId
    private String docId;

    private Long seq;

    private LocalDateTime updatedAt;
}
