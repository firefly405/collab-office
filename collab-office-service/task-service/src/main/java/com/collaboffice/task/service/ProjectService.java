package com.collaboffice.task.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;
    private final TaskService tasks;

    public Map<String,Object> require(String id,boolean write) {
        var project=access.row("SELECT project_id,name,description,space_id,status,creator_id,milestones_json FROM task_project WHERE project_id=? AND deleted=0",id);
        access.requireSpace(((Number)project.get("space_id")).longValue(),write);
        access.requireProject(id,false);
        if(write && ((Number)project.get("status")).intValue()==2) throw new BusinessException(409,"项目已归档");
        return project;
    }

    public void requireColumn(Long column,boolean write) {
        String project=access.row("SELECT project_id FROM task_kanban_column WHERE id=? AND deleted=0",column).get("project_id").toString();
        require(project,write);
        if(write) access.requireProject(project,true);
    }

    public void requireTask(String id,boolean write) {
        access.requireTask(id,write);
        require(access.row("SELECT project_id FROM task_task WHERE task_id=? AND deleted=0",id).get("project_id").toString(),write);
    }

    public void sameProject(String task,String other,boolean column) {
        var a=access.row("SELECT project_id FROM task_task WHERE task_id=? AND deleted=0",task);
        var b=access.row(column?"SELECT project_id FROM task_kanban_column WHERE id=? AND deleted=0":"SELECT project_id FROM task_task WHERE task_id=? AND deleted=0",other);
        if(!Objects.equals(a.get("project_id"),b.get("project_id"))) throw new BusinessException(400,"资源必须属于同一项目");
    }

    public Map<String,Object> detail(String id) {
        var p=require(id,false);Map<String,Object> result=new LinkedHashMap<>(p);
        Object milestones=result.remove("milestones_json");
        result.put("milestones",milestones==null?List.of():JSON.parseArray(milestones.toString()));
        result.put("memberIds",jdbc.queryForList("SELECT user_id FROM task_project_member WHERE project_id=?",Long.class,id));
        result.put("canManage",access.canManageProject(id));
        result.put("managerId",p.get("creator_id"));
        return result;
    }

    @Transactional
    public void update(String id,Map<String,Object> body) {
        var p=require(id,false);Long space=((Number)p.get("space_id")).longValue();access.requireSpace(space,true);
        access.requireProject(id,true);
        Long manager=p.get("creator_id")==null?null:((Number)p.get("creator_id")).longValue();
        if(body.containsKey("managerId")) {
            access.requireSystemAdmin();
            if(body.get("managerId")==null) throw new BusinessException(400,"请选择项目负责人");
            manager=Long.valueOf(body.get("managerId").toString());
            if(!Set.of("SYSTEM_ADMIN","PROJECT_ADMIN").contains(access.platformRole(manager)) || jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=? AND (expires_at IS NULL OR expires_at>NOW())",Integer.class,space,manager)==0) throw new BusinessException(400,"负责人须为当前团队内的系统管理员或项目管理员");
        }
        String name=Objects.toString(body.getOrDefault("name",p.get("name")),"").trim();
        String description=Objects.toString(body.getOrDefault("description",p.get("description")),"");
        if(name.isEmpty() || name.length()>100 || description.length()>5000) throw new BusinessException(400,"项目名称或说明长度无效");
        int status=body.get("status")==null?((Number)p.get("status")).intValue():((Number)body.get("status")).intValue();
        if(status!=1 && status!=2) throw new BusinessException(400,"项目状态无效");
        Object milestones=body.getOrDefault("milestones",p.get("milestones_json")==null?List.of():JSON.parseArray(p.get("milestones_json").toString()));
        if(!(milestones instanceof List<?> list) || list.size()>50) throw new BusinessException(400,"最多50个里程碑");
        List<Map<String,Object>> clean=new ArrayList<>();
        for(Object entry:list) {
            if(!(entry instanceof Map<?,?> m)) throw new BusinessException(400,"里程碑无效");
            String title=Objects.toString(m.get("title"),"").trim(),date=Objects.toString(m.get("dueDate"),"");
            if(title.isEmpty() || title.length()>100) throw new BusinessException(400,"里程碑标题为1到100字");
            if(!date.isEmpty()) LocalDate.parse(date);
            clean.add(Map.of("title",title,"dueDate",date,"done",Boolean.TRUE.equals(m.get("done"))));
        }
        if(body.containsKey("memberIds")) {
            if(!(body.get("memberIds") instanceof List<?> ids) || ids.size()>500) throw new BusinessException(400,"项目成员无效");
            Set<Long> validated=new LinkedHashSet<>();
            if(manager!=null) validated.add(manager);
            for(Object value:ids) {
                long member=Long.parseLong(value.toString());
                if(jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=?",Integer.class,space,member)==0) throw new BusinessException(400,"成员必须属于项目团队");
                validated.add(member);
            }
            jdbc.update("DELETE FROM task_project_member WHERE project_id=?",id);
            for(Long member:validated) jdbc.update("INSERT INTO task_project_member(project_id,user_id) VALUES (?,?)",id,member);
        }
        if(manager!=null) jdbc.update("INSERT IGNORE INTO task_project_member(project_id,user_id) VALUES (?,?)",id,manager);
        jdbc.update("UPDATE task_project SET name=?,description=?,status=?,milestones_json=?,creator_id=? WHERE project_id=? AND deleted=0",name,description,status,JSON.toJSONString(clean),manager,id);
        jdbc.update("UPDATE task_task SET is_archived=? WHERE project_id=? AND deleted=0",status==2?1:0,id);
    }

    public Map<String,Object> statistics(String project,Long assignee) {
        if(project!=null && !project.isBlank()) require(project,false);
        var rows=tasks.getTaskList(project==null || project.isBlank()?null:project,null).stream()
                .filter(t->assignee==null || Objects.equals(Objects.toString(t.get("assigneeId"),""),assignee.toString())).toList();
        long done=rows.stream().filter(t->Integer.valueOf(2).equals(t.get("status"))).count();
        long overdue=rows.stream().filter(t->((Number)t.get("status")).intValue()<2 && t.get("dueDate")!=null && LocalDate.parse(t.get("dueDate").toString()).isBefore(LocalDate.now())).count();
        List<Map<String,Object>> trend=new ArrayList<>();
        for(int i=13;i>=0;i--) {
            String date=LocalDate.now().minusDays(i).toString();
            trend.add(Map.of("date",date,"created",rows.stream().filter(t->t.get("createTime")!=null && t.get("createTime").toString().startsWith(date)).count(),
                    "completed",rows.stream().filter(t->t.get("completedDate")!=null && t.get("completedDate").toString().startsWith(date)).count()));
        }
        return Map.of("total",rows.size(),"completed",done,"overdue",overdue,"completionRate",rows.isEmpty()?0:Math.round(done*100.0/rows.size()),
                "overdueRate",rows.isEmpty()?0:Math.round(overdue*100.0/rows.size()),"trend",trend,"tasks",rows);
    }
}
