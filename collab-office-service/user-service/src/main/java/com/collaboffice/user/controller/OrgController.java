package com.collaboffice.user.controller;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.user.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.poi.ss.usermodel.*;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/org")
public class OrgController {
 private final OrganizationService org;private final WorkspaceAccess access;private final JdbcTemplate jdbc;
 @GetMapping("/dept/tree") public CommonResult<Object> tree(@RequestParam Long spaceId){return CommonResult.success(org.state(spaceId).get("departments"));}
 @PostMapping("/dept") public CommonResult<Void> create(@RequestBody Map<String,Object> b){org.department(access.currentSpace(),b);return CommonResult.success(null);}
 @PutMapping("/dept/{id}") public CommonResult<Void> update(@PathVariable Long id,@RequestBody Map<String,Object> b){b.put("id",id);org.department(access.currentSpace(),b);return CommonResult.success(null);}
 @DeleteMapping("/dept/{id}") public CommonResult<Void> delete(@PathVariable Long id){org.deleteDepartment(access.currentSpace(),id);return CommonResult.success(null);}
 @GetMapping("/permission/matrix") public CommonResult<Object> matrix(){return CommonResult.success(org.state(access.currentSpace()).get("roles"));}
 @PutMapping("/permission/matrix") public CommonResult<Void> matrix(@RequestBody Map<String,Object> b){org.role(access.currentSpace(),b);return CommonResult.success(null);}
 @PostMapping("/members/batch-import") @org.springframework.transaction.annotation.Transactional
 public CommonResult<Map<String,Object>> importMembers(@RequestParam Long spaceId,@RequestParam("file")MultipartFile file)throws Exception{
  org.admin(spaceId);if(file.getSize()>2*1024*1024)throw new BusinessException(400,"Excel最多2MB");var rows=new ArrayList<Map<String,Object>>();int added=0;var format=new DataFormatter();
  try(var book=WorkbookFactory.create(file.getInputStream())){var sheet=book.getSheetAt(0);if(sheet.getLastRowNum()>200)throw new BusinessException(400,"最多200名成员");for(int i=1;i<=sheet.getLastRowNum();i++){var row=sheet.getRow(i);if(row==null)continue;String account=format.formatCellValue(row.getCell(0)).trim();if(account.isBlank())continue;var users=jdbc.queryForList("SELECT id FROM sys_user WHERE deleted=0 AND status=1 AND (username=? OR email=? OR phone=?)",Long.class,account,account,account);if(users.size()!=1){rows.add(Map.of("row",i+1,"account",account,"result","账号不存在或不唯一"));continue;}int count=jdbc.update("INSERT IGNORE INTO sys_space_member(space_id,user_id,role_type) VALUES(?,?,'2')",spaceId,users.get(0));added+=count;rows.add(Map.of("row",i+1,"account",account,"result",count==1?"已添加":"已在团队中"));}}
  jdbc.update("INSERT INTO workspace_audit(space_id,actor_id,action,target) VALUES(?,?,'members.import',?)",spaceId,access.userId(),String.valueOf(added));return CommonResult.success(Map.of("added",added,"rows",rows));
 }
}
