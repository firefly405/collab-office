package com.collaboffice.doc.service;
import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WhiteboardOperationTest {
 @Test void retriesAreIdempotentButCannotOverwriteOrImpersonate(){
  var jdbc=org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);
  var access=org.mockito.Mockito.mock(com.collaboffice.common.security.WorkspaceAccess.class);
  var service=new WhiteboardWorkspaceService(jdbc,access);
  var op=JSON.parseObject("{\"id\":\"retry\",\"tool\":\"rect\",\"color\":\"#008800\",\"width\":3,\"x\":20,\"y\":30,\"w\":100,\"h\":120}");
  var rows=java.util.List.<java.util.Map<String,Object>>of(java.util.Map.of("type","operation","content",op.toJSONString(),"metadata","{\"actorId\":1,\"active\":true}"));
  org.mockito.Mockito.when(access.userId()).thenReturn(1L);
  org.mockito.Mockito.when(jdbc.queryForList(org.mockito.ArgumentMatchers.startsWith("SELECT id,element_type"),org.mockito.ArgumentMatchers.eq("board"))).thenReturn(rows);
  assertEquals(rows,service.append("board",op));
  op.put("x",25);assertThrows(BusinessException.class,()->service.append("board",op));op.put("x",20);
  org.mockito.Mockito.when(access.userId()).thenReturn(2L);assertThrows(BusinessException.class,()->service.append("board",op));
  org.mockito.Mockito.verify(jdbc,org.mockito.Mockito.never()).update(org.mockito.ArgumentMatchers.startsWith("INSERT INTO"),org.mockito.ArgumentMatchers.<Object[]>any());
 }
 @Test void validatesShapeAndRejectsUnboundedInput(){assertEquals("triangle",WhiteboardWorkspaceService.validate(JSON.parseObject("{\"id\":\"a\",\"tool\":\"triangle\",\"color\":\"#008800\",\"width\":3,\"x\":20,\"y\":30,\"w\":100,\"h\":120}")).getString("tool"));assertThrows(BusinessException.class,()->WhiteboardWorkspaceService.validate(JSON.parseObject("{\"id\":\"a\",\"tool\":\"script\",\"color\":\"red\",\"width\":3}")));}
 @Test void penMustHaveValidPoints(){assertThrows(BusinessException.class,()->WhiteboardWorkspaceService.validate(JSON.parseObject("{\"id\":\"a\",\"tool\":\"pen\",\"color\":\"#000000\",\"width\":3,\"points\":[[99999,1]]}")));}
}
