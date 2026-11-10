package com.collaboffice.doc.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.entity.TemplateTemplate;
import com.collaboffice.doc.mapper.TemplateTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {
    @Mock TemplateTemplateMapper templates;
    @Mock DocumentService documents;
    @Mock DocumentContentService contents;
    @Mock WorkspaceAccess access;
    @Mock JdbcTemplate jdbc;
    @Spy ObjectMapper json=new ObjectMapper();
    @InjectMocks TemplateService service;

    @Test void useCreatesIndependentDocumentWithStoredContent() {
        var tpl=new TemplateTemplate();tpl.setId(7L);tpl.setType(1);tpl.setSpaceId(13L);tpl.setIsOfficial(0);
        tpl.setName("计划模板");tpl.setContentJson("{\"documentContent\":\"<h1>项目计划</h1>\"}");
        when(templates.selectOne(any())).thenReturn(tpl);when(access.currentSpace()).thenReturn(13L);
        when(access.requireSpace(eq(13L),anyBoolean())).thenReturn(13L);when(access.userId()).thenReturn(1L);
        when(jdbc.queryForObject(anyString(),eq(String.class),eq(1L))).thenReturn("演示用户");
        when(documents.create(eq(1L),eq("演示用户"),any())).thenReturn("900");
        assertEquals("900",service.use("template"));
        verify(contents).saveContent("900","<h1>项目计划</h1>",1L);
        verify(jdbc).update("UPDATE template_template SET usage_count=usage_count+1 WHERE id=?",7L);
    }
    @Test void nonOwnerCannotAlterTemplate() {
        var tpl=new TemplateTemplate();tpl.setSpaceId(13L);tpl.setCreatorId(2L);tpl.setIsOfficial(0);
        when(templates.selectOne(any())).thenReturn(tpl);when(access.userId()).thenReturn(1L);
        assertThrows(BusinessException.class,()->service.require("template",true));
        verifyNoInteractions(documents,contents);
    }
    @Test void htmlExportKeepsHeadingsAndDecodesEntities() {
        var blocks=com.alibaba.fastjson2.JSON.parseObject(DocumentExportContent.normalize("<h1>计划 &amp; 进度</h1><p>第一行<br>第二行</p>")).getJSONArray("blocks");
        assertEquals("h1",blocks.getJSONObject(0).getString("type"));
        assertEquals("计划 & 进度",blocks.getJSONObject(0).getString("text"));
        assertEquals(3,blocks.size());
    }
    @Test void browserJsonQuotedHtmlAndEmptyDocumentAreExportable() {
        String html="<h1>浏览器正文</h1><p>引号 \" 保留</p>";
        assertEquals(html,DocumentExportContent.unwrapHtml(com.alibaba.fastjson2.JSON.toJSONString(html)));
        assertTrue(DocumentExportContent.normalize(com.alibaba.fastjson2.JSON.toJSONString(html)).contains("浏览器正文"));
        assertNotNull(com.alibaba.fastjson2.JSON.parseObject(DocumentExportContent.normalize("{}")).getJSONArray("blocks"));
    }
}
