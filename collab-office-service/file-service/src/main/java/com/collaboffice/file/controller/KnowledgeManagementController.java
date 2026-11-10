package com.collaboffice.file.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeManagementController {
    private final JdbcTemplate jdbc;
    private final WorkspaceAccess access;

    @GetMapping("/categories")
    public CommonResult<List<Map<String,Object>>> categories() {
        return CommonResult.success(jdbc.queryForList("SELECT id,name FROM knowledge_category WHERE space_id=? ORDER BY name",access.currentSpace()));
    }
    @PostMapping("/categories")
    public CommonResult<Void> category(@RequestBody Map<String,String> body) {
        Long space=access.requireSpace(access.currentSpace(),true);
        String name=Objects.toString(body.get("name"),"").trim();
        if(name.isBlank()||name.length()>100) throw new BusinessException(400,"分类名称为1至100字");
        jdbc.update("INSERT IGNORE INTO knowledge_category(space_id,name) VALUES(?,?)",space,name);
        return CommonResult.success(null);
    }
    @DeleteMapping("/categories/{id}")
    @Transactional
    public CommonResult<Void> deleteCategory(@PathVariable Long id) {
        Long space=access.requireSpace(access.currentSpace(),true);
        if(!access.isAdmin(space)) throw new BusinessException(403,"需要管理员权限");
        jdbc.update("UPDATE knowledge_item SET category_id=NULL WHERE category_id=? AND space_id=?",id,space);
        jdbc.update("DELETE FROM knowledge_category WHERE id=? AND space_id=?",id,space);
        return CommonResult.success(null);
    }
    @PutMapping("/categories/{id}")
    public CommonResult<Void> renameCategory(@PathVariable Long id,@RequestBody Map<String,String> body) {
        Long space=access.requireSpace(access.currentSpace(),true);
        if(!access.isAdmin(space)) throw new BusinessException(403,"需要管理员权限");
        String name=Objects.toString(body.get("name"),"").trim();
        if(name.isBlank()||name.length()>100) throw new BusinessException(400,"分类名称为1至100字");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_category WHERE space_id=? AND name=? AND id<>?",Integer.class,space,name,id)>0) throw new BusinessException(409,"分类名称已存在");
        if(jdbc.update("UPDATE knowledge_category SET name=? WHERE id=? AND space_id=?",name,id,space)==0) throw new BusinessException(404,"分类不存在");
        return CommonResult.success(null);
    }
    @GetMapping("/contributions")
    public CommonResult<List<Map<String,Object>>> contributions() {
        Long space=access.requireSpace(access.currentSpace(),false);
        return CommonResult.success(jdbc.queryForList("SELECT k.contributor_id AS userId,COALESCE(u.real_name,u.username,'团队成员') AS name,COUNT(*) AS articles,COALESCE(SUM(k.view_count),0) AS views,COALESCE(SUM(k.star_count),0) AS favorites FROM knowledge_item k LEFT JOIN sys_user u ON u.id=k.contributor_id WHERE k.space_id=? AND k.deleted=0 GROUP BY k.contributor_id,u.real_name,u.username ORDER BY articles DESC,k.contributor_id",space));
    }
    @GetMapping("/favorites")
    public CommonResult<List<String>> favorites() {
        return CommonResult.success(jdbc.queryForList("SELECT f.knowledge_id FROM knowledge_favorite f JOIN knowledge_item k ON BINARY k.knowledge_id=BINARY f.knowledge_id WHERE f.user_id=? AND k.space_id=? AND k.deleted=0",String.class,access.userId(),access.currentSpace()));
    }
    @PutMapping("/{id}/favorite")
    @Transactional
    public CommonResult<Void> favorite(@PathVariable String id,@RequestParam boolean value) {
        var item=access.row("SELECT space_id FROM knowledge_item WHERE knowledge_id=? AND deleted=0",id);
        access.requireSpace(((Number)item.get("space_id")).longValue(),false);
        if(value) jdbc.update("INSERT IGNORE INTO knowledge_favorite(knowledge_id,user_id) VALUES(?,?)",id,access.userId());
        else jdbc.update("DELETE FROM knowledge_favorite WHERE knowledge_id=? AND user_id=?",id,access.userId());
        jdbc.update("UPDATE knowledge_item SET star_count=(SELECT COUNT(*) FROM knowledge_favorite WHERE knowledge_id=?) WHERE knowledge_id=?",id,id);
        return CommonResult.success(null);
    }
}
