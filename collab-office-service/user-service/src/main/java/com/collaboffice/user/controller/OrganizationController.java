package com.collaboffice.user.controller;
import com.collaboffice.user.service.OrganizationService;
import com.collaboffice.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/space/{spaceId}/organization")
public class OrganizationController {
 private final OrganizationService org;
 @GetMapping("/documents/{id}") public CommonResult<Map<String,Object>> document(@PathVariable Long spaceId,@PathVariable String id){return CommonResult.success(org.document(spaceId,id));}
 @PutMapping("/documents/{id}") public CommonResult<Void> document(@PathVariable Long spaceId,@PathVariable String id,@RequestBody List<Map<String,Object>> rows){org.document(spaceId,id,rows);return CommonResult.success(null);}
 @GetMapping public CommonResult<Map<String,Object>> state(@PathVariable Long spaceId){return CommonResult.success(org.state(spaceId));}
 @PostMapping("/departments") public CommonResult<Void> department(@PathVariable Long spaceId,@RequestBody Map<String,Object> b){org.department(spaceId,b);return CommonResult.success(null);}
 @DeleteMapping("/departments/{id}") public CommonResult<Void> department(@PathVariable Long spaceId,@PathVariable Long id){org.deleteDepartment(spaceId,id);return CommonResult.success(null);}
 @PostMapping("/roles") public CommonResult<Void> role(@PathVariable Long spaceId,@RequestBody Map<String,Object> b){org.role(spaceId,b);return CommonResult.success(null);}
 @DeleteMapping("/roles/{id}") public CommonResult<Void> role(@PathVariable Long spaceId,@PathVariable Long id){org.deleteRole(spaceId,id);return CommonResult.success(null);}
 @PutMapping("/members/{id}") public CommonResult<Void> member(@PathVariable Long spaceId,@PathVariable Long id,@RequestBody Map<String,Object> b){org.memberSettings(spaceId,id,b);return CommonResult.success(null);}
 @PostMapping("/transfer") public CommonResult<Void> transfer(@PathVariable Long spaceId,@RequestBody Map<String,Long> b){org.transfer(spaceId,b.get("userId"));return CommonResult.success(null);}
 @PostMapping("/dissolve") public CommonResult<Void> dissolve(@PathVariable Long spaceId,@RequestBody Map<String,String> b){org.dissolve(spaceId,b.get("confirm"));return CommonResult.success(null);}
 @PutMapping("/settings") public CommonResult<Void> settings(@PathVariable Long spaceId,@RequestBody Map<String,Object> b){org.settings(spaceId,b);return CommonResult.success(null);}
 @GetMapping("/folders/{id}") public CommonResult<List<Map<String,Object>>> folder(@PathVariable Long spaceId,@PathVariable String id){return CommonResult.success(org.folder(spaceId,id));}
 @PutMapping("/folders/{id}") public CommonResult<Void> folder(@PathVariable Long spaceId,@PathVariable String id,@RequestBody List<Map<String,Object>> b){org.folder(spaceId,id,b);return CommonResult.success(null);}
}
