package com.collaboffice.file.service;

import com.collaboffice.file.entity.DocReference;
import com.collaboffice.file.entity.FileFile;
import com.collaboffice.file.mapper.DocReferenceMapper;
import com.collaboffice.file.mapper.FileFileMapper;
import com.collaboffice.file.service.impl.DocReferenceServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocReferenceService 单元测试")
class DocReferenceServiceTest {

    @Mock
    private DocReferenceMapper docReferenceMapper;

    @Mock
    private FileFileMapper fileFileMapper;

    @InjectMocks
    private DocReferenceServiceImpl docReferenceService;

    private DocReference ref1;
    private DocReference ref2;
    private FileFile docInfo1;
    private FileFile docInfo2;

    @BeforeEach
    void setUp() {
        ref1 = new DocReference();
        ref1.setId(1L);
        ref1.setSourceDocId("docA");
        ref1.setTargetDocId("docB");
        ref1.setCreatorId(1001L);
        ref1.setCreateTime(LocalDateTime.now());

        ref2 = new DocReference();
        ref2.setId(2L);
        ref2.setSourceDocId("docC");
        ref2.setTargetDocId("docA");
        ref2.setCreatorId(1002L);
        ref2.setCreateTime(LocalDateTime.now());

        docInfo1 = new FileFile();
        docInfo1.setFileId("docB");
        docInfo1.setFileName("目标文档B");

        docInfo2 = new FileFile();
        docInfo2.setFileId("docC");
        docInfo2.setFileName("源文档C");
    }

    @Test
    @DisplayName("测试创建文档引用关系")
    void testCreateReference() {
        Mockito.when(docReferenceMapper.insert(any(DocReference.class))).thenReturn(1);
        docReferenceService.createReference("docA", "docB", 1001L);
        Mockito.verify(docReferenceMapper, Mockito.times(1)).insert(any(DocReference.class));
    }

    @Test
    @DisplayName("测试获取出向引用")
    void testGetOutboundReferences() {
        Mockito.when(docReferenceMapper.selectOutbound("docA")).thenReturn(List.of(ref1));
        Mockito.when(fileFileMapper.selectOne(any())).thenReturn(docInfo1);

        List<Map<String, Object>> result = docReferenceService.getOutboundReferences("docA");

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("docB", result.get(0).get("docId"));
        Assertions.assertEquals("目标文档B", result.get(0).get("title"));
    }

    @Test
    @DisplayName("测试获取入向引用")
    void testGetInboundReferences() {
        Mockito.when(docReferenceMapper.selectInbound("docA")).thenReturn(List.of(ref2));
        Mockito.when(fileFileMapper.selectOne(any())).thenReturn(docInfo2);

        List<Map<String, Object>> result = docReferenceService.getInboundReferences("docA");

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("docC", result.get(0).get("docId"));
        Assertions.assertEquals("源文档C", result.get(0).get("title"));
    }

    @Test
    @DisplayName("测试生成文档引用图谱")
    void testGetReferenceGraph() {
        Mockito.when(docReferenceMapper.selectOutbound("docA")).thenReturn(List.of(ref1));
        Mockito.when(docReferenceMapper.selectInbound("docA")).thenReturn(List.of(ref2));
        
        // Mock getDocTitle file mapping for docA, docB, docC
        FileFile docInfoA = new FileFile();
        docInfoA.setFileId("docA");
        docInfoA.setFileName("核心文档A");

        Mockito.when(fileFileMapper.selectOne(any()))
                .thenReturn(docInfoA)  // For docA
                .thenReturn(docInfo1)  // For docB
                .thenReturn(docInfo2); // For docC

        Map<String, Object> graph = docReferenceService.getReferenceGraph("docA");

        Assertions.assertNotNull(graph);
        List<?> nodes = (List<?>) graph.get("nodes");
        List<?> edges = (List<?>) graph.get("edges");

        Assertions.assertEquals(3, nodes.size());
        Assertions.assertEquals(2, edges.size());
    }
}
