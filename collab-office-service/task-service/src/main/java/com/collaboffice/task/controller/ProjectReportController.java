package com.collaboffice.task.controller;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/task/reports")
public class ProjectReportController {
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;

    @GetMapping
    public CommonResult<List<Map<String,Object>>> list(@RequestParam(required=false) String projectId) {
        Long space=access.currentSpace();
        if(projectId!=null) access.requireProject(projectId,false);
        var rows=jdbc.queryForList("SELECT r.id,r.project_id AS projectId,p.name AS projectName,r.author_id AS authorId,COALESCE(u.real_name,u.username) AS authorName,r.title,r.content,r.status,r.created_at AS createdAt FROM project_report r JOIN task_project p ON p.project_id=r.project_id LEFT JOIN sys_user u ON u.id=r.author_id WHERE p.space_id=? AND p.deleted=0"+(projectId==null?"":" AND r.project_id=?")+" ORDER BY r.id DESC LIMIT 200",projectId==null?new Object[]{space}:new Object[]{space,projectId});
        var visible=rows.stream().filter(r->access.canReadProject(r.get("projectId").toString())).toList();
        for(var row:visible) {
            row.put("canReview",access.canManageProject(row.get("projectId").toString()));
            row.put("replies",jdbc.queryForList("SELECT r.id,r.author_id AS authorId,COALESCE(u.real_name,u.username) AS authorName,r.content,r.created_at AS createdAt FROM project_report_reply r LEFT JOIN sys_user u ON u.id=r.author_id WHERE report_id=? ORDER BY r.id",row.get("id")));
        }
        return CommonResult.success(visible);
    }

    private String text(Map<String,String> body,String field,int max) {
        String value=Objects.toString(body.get(field),"").trim();
        if(value.isEmpty() || value.length()>max) throw new BusinessException(400,"标题或内容为空或过长");
        return value;
    }

    @PostMapping
    public CommonResult<Void> create(@RequestBody Map<String,String> body) {
        String project=text(body,"projectId",32);access.requireProject(project,false);
        access.requireSpace(access.currentSpace(),true);
        var p=access.row("SELECT space_id,status FROM task_project WHERE project_id=? AND deleted=0",project);
        if(!Objects.equals(((Number)p.get("space_id")).longValue(),access.currentSpace()) || ((Number)p.get("status")).intValue()!=1) throw new BusinessException(409,"请选择当前团队内的进行中项目");
        jdbc.update("INSERT INTO project_report(project_id,author_id,title,content) VALUES(?,?,?,?)",project,access.userId(),text(body,"title",150),text(body,"content",5000));
        return CommonResult.success(null);
    }

    @PostMapping("/{id}/replies")
    @Transactional
    public CommonResult<Void> reply(@PathVariable Long id,@RequestBody Map<String,String> body) {
        var report=access.row("SELECT project_id FROM project_report WHERE id=?",id);
        access.requireProject(report.get("project_id").toString(),false);
        jdbc.update("INSERT INTO project_report_reply(report_id,author_id,content) VALUES(?,?,?)",id,access.userId(),text(body,"content",2000));
        return CommonResult.success(null);
    }

    @PutMapping("/{id}/review")
    public CommonResult<Void> review(@PathVariable Long id) {
        var report=access.row("SELECT project_id FROM project_report WHERE id=?",id);
        access.requireProject(report.get("project_id").toString(),true);
        jdbc.update("UPDATE project_report SET status='reviewed' WHERE id=?",id);
        return CommonResult.success(null);
    }
}
