package com.collaboffice.file.service.impl;

import com.collaboffice.file.entity.DocReference;
import com.collaboffice.file.entity.FileFile;
import com.collaboffice.file.mapper.DocReferenceMapper;
import com.collaboffice.file.mapper.FileFileMapper;
import com.collaboffice.file.service.DocReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocReferenceServiceImpl implements DocReferenceService {

    private final DocReferenceMapper docReferenceMapper;
    private final FileFileMapper fileFileMapper;

    @Override
    @Transactional
    public void createReference(String sourceDocId, String targetDocId, Long creatorId) {
        DocReference ref = new DocReference();
        ref.setSourceDocId(sourceDocId);
        ref.setTargetDocId(targetDocId);
        ref.setCreatorId(creatorId);
        ref.setCreateTime(LocalDateTime.now());
        docReferenceMapper.insert(ref);
    }

    @Override
    public List<Map<String, Object>> getOutboundReferences(String docId) {
        List<DocReference> refs = docReferenceMapper.selectOutbound(docId);
        return mapReferences(refs, true);
    }

    @Override
    public List<Map<String, Object>> getInboundReferences(String docId) {
        List<DocReference> refs = docReferenceMapper.selectInbound(docId);
        return mapReferences(refs, false);
    }

    @Override
    public Map<String, Object> getReferenceGraph(String docId) {
        List<DocReference> outbound = docReferenceMapper.selectOutbound(docId);
        List<DocReference> inbound = docReferenceMapper.selectInbound(docId);
        Set<String> seenIds = new HashSet<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        // Add center node
        nodes.add(nodeMap(docId));
        seenIds.add(docId);

        for (DocReference ref : outbound) {
            if (seenIds.add(ref.getTargetDocId())) {
                nodes.add(nodeMap(ref.getTargetDocId()));
            }
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", ref.getSourceDocId());
            edge.put("target", ref.getTargetDocId());
            edge.put("type", "reference");
            edges.add(edge);
        }
        for (DocReference ref : inbound) {
            if (seenIds.add(ref.getSourceDocId())) {
                nodes.add(nodeMap(ref.getSourceDocId()));
            }
            Map<String, Object> edge = new LinkedHashMap<>();
            edge.put("source", ref.getSourceDocId());
            edge.put("target", ref.getTargetDocId());
            edge.put("type", "reference");
            edges.add(edge);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    private List<Map<String, Object>> mapReferences(List<DocReference> refs, boolean outbound) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocReference ref : refs) {
            Map<String, Object> m = new LinkedHashMap<>();
            String linkedDocId = outbound ? ref.getTargetDocId() : ref.getSourceDocId();
            m.put("docId", linkedDocId);
            m.put("title", getDocTitle(linkedDocId));
            m.put("createTime", ref.getCreateTime());
            result.add(m);
        }
        return result;
    }

    private Map<String, Object> nodeMap(String docId) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", docId);
        node.put("label", getDocTitle(docId));
        return node;
    }

    private String getDocTitle(String docId) {
        try {
            FileFile file = fileFileMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                            .eq(FileFile::getFileId, docId));
            return file != null && file.getFileName() != null ? file.getFileName() : docId;
        } catch (Exception e) {
            return docId;
        }
    }
}
