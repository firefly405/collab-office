package com.collaboffice.doc.service;

import com.alibaba.fastjson2.JSONObject;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.impl.MindmapServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MindmapWorkspaceTest {
    MindmapNode node(long id,Long parent,String map){var n=new MindmapNode();n.setId(id);n.setParentId(parent);n.setMapId(map);n.setContent("节点"+id);return n;}
    @Test void treeRejectsCyclesOrphansMultipleRootsAndExcessiveDepth(){
        MindmapServiceImpl.validateTree(List.of(node(1,null,"a"),node(2,1L,"a")));
        assertThrows(BusinessException.class,()->MindmapServiceImpl.validateTree(List.of(node(1,null,"a"),node(2,3L,"a"),node(3,2L,"a"))));
        assertThrows(BusinessException.class,()->MindmapServiceImpl.validateTree(List.of(node(1,null,"a"),node(2,9L,"a"))));
        assertThrows(BusinessException.class,()->MindmapServiceImpl.validateTree(List.of(node(1,null,"a"),node(2,null,"a"))));
        var deep=new ArrayList<MindmapNode>();for(long i=1;i<=51;i++)deep.add(node(i,i==1?null:i-1,"a"));assertThrows(BusinessException.class,()->MindmapServiceImpl.validateTree(deep));
    }
    @Test void moveRejectsCrossMapCyclesAndRootDeletion(){
        var nodes=mock(MindmapNodeMapper.class);var snapshots=mock(MindmapSnapshotMapper.class);
        var service=new MindmapServiceImpl(mock(MindmapMapMapper.class),nodes,mock(JdbcTemplate.class),mock(WorkspaceAccess.class),snapshots);
        var root=node(1,null,"a");var child=node(2,1L,"a");var grandchild=node(3,2L,"a");
        when(nodes.selectById(1L)).thenReturn(root);when(nodes.selectById(2L)).thenReturn(child);when(nodes.selectById(3L)).thenReturn(grandchild);when(nodes.selectById(4L)).thenReturn(node(4,null,"b"));when(nodes.selectByMapId("a")).thenReturn(List.of(root,child,grandchild));
        assertThrows(BusinessException.class,()->service.moveNode(2L,4L,null));assertThrows(BusinessException.class,()->service.moveNode(2L,3L,null));assertThrows(BusinessException.class,()->service.deleteNode(1L));verify(nodes,never()).updateById(any(MindmapNode.class));verifyNoInteractions(snapshots);
    }
    @Test void staleNoteIsRejectedBeforeAnyMutation(){
        var nodes=mock(MindmapNodeMapper.class);var core=mock(MindmapService.class);var n=node(2,1L,"a");n.setNote("其他人的备注");when(nodes.selectById(2L)).thenReturn(n);
        var service=new MindmapWorkspaceService(mock(WorkspaceAccess.class),nodes,mock(MindmapMapMapper.class),core,mock(JdbcTemplate.class),mock(StringRedisTemplate.class));
        assertThrows(BusinessException.class,()->service.properties("a",2L,JSONObject.of("expectedNote","旧备注")));verifyNoInteractions(core);verify(nodes,never()).updateById(any(MindmapNode.class));
    }
    @Test void pdfReopensAndInvalidInputRejected() throws Exception {
        var service=new MindmapWorkspaceService(null,null,null,null,null,null);var image=new java.awt.image.BufferedImage(300,150,java.awt.image.BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();graphics.setColor(java.awt.Color.WHITE);graphics.fillRect(0,0,300,150);graphics.setColor(java.awt.Color.BLACK);graphics.drawString("Mindmap",30,60);graphics.dispose();var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);
        String base=Base64.getEncoder().encodeToString(out.toByteArray());byte[] pdf=Base64.getDecoder().decode(service.pdf(base));var reader=new com.lowagie.text.pdf.PdfReader(pdf);assertEquals(1,reader.getNumberOfPages());reader.close();assertThrows(BusinessException.class,()->service.pdf("not-base64"));
        String directory=System.getProperty("prd.export.directory");if(directory!=null)java.nio.file.Files.write(java.nio.file.Path.of(directory,"prd-batch6-input.png"),out.toByteArray());
        var downloaded=java.nio.file.Path.of(System.getProperty("user.home"),"Downloads","PRD 验收 · 协作产品规划.pdf");if(java.nio.file.Files.exists(downloaded)){var actual=new com.lowagie.text.pdf.PdfReader(java.nio.file.Files.readAllBytes(downloaded));assertEquals(1,actual.getNumberOfPages());assertTrue(actual.getPageN(1).getAsDict(com.lowagie.text.pdf.PdfName.RESOURCES).size()>0);actual.close();}
    }
    @Test void otherMemberCannotControlActivePresentation(){
        var access=mock(WorkspaceAccess.class);when(access.userId()).thenReturn(2L);var redis=mock(StringRedisTemplate.class);var values=mock(org.springframework.data.redis.core.ValueOperations.class);when(redis.opsForValue()).thenReturn(values);when(values.setIfAbsent(anyString(),anyString(),any(java.time.Duration.class))).thenReturn(true);when(values.get("mindmap:presentation:a")).thenReturn("{\"active\":true,\"presenterId\":1}");
        var service=new MindmapWorkspaceService(access,null,null,null,null,redis);assertThrows(BusinessException.class,()->service.control("a",JSONObject.of("action","next")));verify(values,never()).set(anyString(),anyString(),any(java.time.Duration.class));
    }
}
