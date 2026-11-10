package com.collaboffice.collab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档 Yjs update 持久化记录。
 * 每条记录对应一个客户端产生的 Yjs update 字节（Base64 编码）。
 */
@Data
@TableName("doc_yjs_update")
public class DocYjsUpdate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String docId;

    private String clientId;

    private Long userId;

    /** Yjs update 字节的 Base64 编码 */
    private String updateB64;

    /** 文档内单调递增的序列号 */
    private Long seq;

    /** update 种类: yjs_update / yjs_state */
    private String kind;

    private LocalDateTime createdAt;
}
