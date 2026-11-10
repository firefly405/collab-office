package com.collaboffice.doc.service;

import com.collaboffice.doc.vo.DocumentContentVO;

public interface DocumentContentService {
    DocumentContentVO getContent(String docId);
    void saveContent(String docId, String content, Long userId);
    DocumentContentVO getSyncState(String docId);
}
