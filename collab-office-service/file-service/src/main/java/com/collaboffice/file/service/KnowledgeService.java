package com.collaboffice.file.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.file.entity.KnowledgeItem;

public interface KnowledgeService {
    Page<KnowledgeItem> getKnowledgeList(int pageNum, int pageSize);
    KnowledgeItem getKnowledgeDetail(String itemId);
    String createKnowledge(KnowledgeItem item, Long userId);
    void updateKnowledge(String itemId, KnowledgeItem item);
    void deleteKnowledge(String itemId);
}
