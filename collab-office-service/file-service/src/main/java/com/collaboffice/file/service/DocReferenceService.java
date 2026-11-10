package com.collaboffice.file.service;

import java.util.List;
import java.util.Map;

public interface DocReferenceService {
    void createReference(String sourceDocId, String targetDocId, Long creatorId);
    List<Map<String, Object>> getOutboundReferences(String docId);
    List<Map<String, Object>> getInboundReferences(String docId);
    Map<String, Object> getReferenceGraph(String docId);
}
