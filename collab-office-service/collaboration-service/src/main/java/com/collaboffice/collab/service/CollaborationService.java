package com.collaboffice.collab.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CollaborationService {

    void applyOperation(String docId, String userId, String operationJson);

    Map<String, Object> getDocumentState(String docId);

    void joinDocument(String docId, String userId);

    void leaveDocument(String docId, String userId);

    Set<Object> getOnlineUsers(String docId);

    void persistDocumentState(String docId);

    /**
     * 持久化一条 Yjs update 到 MySQL，分配单调递增的 seq。
     *
     * @param docId      文档ID
     * @param clientId   Yjs clientID
     * @param userId     产生该 update 的用户ID（可空）
     * @param updateB64  Yjs update 字节的 Base64 编码
     * @param kind       update 种类：yjs_update / yjs_state
     * @return 分配到的 seq；失败返回 null
     */
    Long persistYjsUpdate(String docId, String clientId, Long userId, String updateB64, String kind);

    /**
     * 拉取文档历史 Yjs updates，用于新客户端 join 时的初始化同步。
     *
     * @param docId     文档ID
     * @param afterSeq  从哪个 seq 之后开始拉取（0 表示全量）
     * @param limit     最多拉取多少条
     * @return update 列表（按 seq 升序）
     */
    List<Map<String, Object>> getYjsUpdates(String docId, long afterSeq, int limit);

    /**
     * 查询文档当前最大 seq。
     */
    long getYjsMaxSeq(String docId);
}
