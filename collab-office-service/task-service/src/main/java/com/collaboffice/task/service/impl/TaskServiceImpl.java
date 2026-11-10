package com.collaboffice.task.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.task.entity.*;
import com.collaboffice.task.mapper.*;
import com.collaboffice.task.service.TaskService;
import com.collaboffice.task.service.TaskSchedule;
import com.collaboffice.common.exception.BusinessException;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskTaskMapper taskMapper;
    private final TaskProjectMapper projectMapper;
    private final TaskCommentMapper commentMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public String createTask(Long userId, String title, String projectId) {
        if (projectId != null) {
            TaskProject project = projectMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskProject>().eq(TaskProject::getProjectId, projectId));
            if (project == null) throw new com.collaboffice.common.exception.BusinessException(404, "项目不存在");
            access.requireSpace(project.getSpaceId(), true);
            access.requireProject(projectId,false);
            if(Integer.valueOf(2).equals(project.getStatus())) throw new BusinessException(409,"项目已归档，请先恢复项目");
        }
        TaskTask task = new TaskTask();
        task.setTaskId(SnowflakeIdGenerator.nextIdStr());
        task.setTitle(title);
        task.setProjectId(projectId);
        task.setCreatorId(userId);
        task.setAssigneeId(userId);
        task.setStatus(0);
        task.setPriority(2);
        task.setProgress(0);
        taskMapper.insert(task);

        publishTaskIndexEvent(task, "UPDATE");

        return task.getTaskId();
    }

    @Override
    public Map<String, Object> getTaskDetail(String taskId) {
        TaskTask task = findTask(taskId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getTaskId());
        boolean manager=access.canManageProject(task.getProjectId());
        result.put("canManage",manager);
        result.put("canEdit",manager || Objects.equals(task.getCreatorId(),access.userId()) || Objects.equals(task.getAssigneeId(),access.userId()));
        result.put("canDelete",manager || Objects.equals(task.getCreatorId(),access.userId()));
        result.put("title", task.getTitle());
        result.put("description", task.getDescription());
        result.put("projectId", task.getProjectId());
        result.put("status", task.getStatus());
        result.put("priority", task.getPriority());
        result.put("progress", task.getProgress());
        result.put("assigneeId", task.getAssigneeId());
        result.put("dueDate", task.getDueDate());
        result.put("createTime", task.getCreateTime());
        result.put("parentId",task.getParentId());
        result.put("columnId",task.getColumnId());
        result.put("tags",task.getTags());
        result.put("attachments",task.getAttachments()==null?List.of():JSON.parseArray(task.getAttachments()));
        result.put("repeatRule",task.getRepeatRule());
        result.put("repeatNextId",task.getRepeatNextId());
        result.put("reminderEnabled",task.getReminderEnabled());
        result.put("reminderDays",task.getReminderDays());
        result.put("reminderFrequency",task.getReminderFrequency());
        result.put("comments", commentMapper.selectByTaskId(taskId).stream().map(c -> {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("userId", c.getUserId());
            cm.put("content", c.getContent());
            cm.put("createTime", c.getCreateTime());
            return cm;
        }).collect(Collectors.toList()));
        return result;
    }

    @Override
    public List<Map<String, Object>> getTaskList(String projectId, Integer status) {
        Long spaceId = access.currentSpace();
        Set<String> allowedProjects = projectMapper.selectBySpaceId(spaceId).stream().map(TaskProject::getProjectId).filter(access::canReadProject).collect(Collectors.toSet());
        List<TaskTask> tasks = projectId != null ? taskMapper.selectByProjectId(projectId) : taskMapper.selectList(null);
        return tasks.stream().filter(t -> allowedProjects.contains(t.getProjectId())).filter(t -> status == null || status.equals(t.getStatus())).map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getTaskId());
            m.put("title", t.getTitle());
            m.put("projectId", t.getProjectId());
            m.put("status", t.getStatus());
            m.put("priority", t.getPriority());
            m.put("progress", t.getProgress());
            m.put("assigneeId", t.getAssigneeId());
            m.put("dueDate", t.getDueDate());
            m.put("parentId",t.getParentId());
            m.put("columnId",t.getColumnId());
            m.put("tags",t.getTags());
            m.put("repeatRule",t.getRepeatRule());
            m.put("completedDate",t.getCompletedDate());
            m.put("createTime",t.getCreateTime());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateTaskStatus(String taskId, Integer status) {
        validateRange(status, 0, 3, "任务状态");
        updateTask(taskId, Map.of("status",status));
    }

    @Override
    @Transactional
    public void updateTask(String taskId, Map<String, Object> updates) {
        access.requireTask(taskId,true);
        jdbc.queryForList("SELECT id FROM task_task WHERE task_id=? AND deleted=0 FOR UPDATE",taskId);
        TaskTask task = findTask(taskId);
        var project=projectMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskProject>().eq(TaskProject::getProjectId,task.getProjectId()));
        if(project==null || Integer.valueOf(2).equals(project.getStatus())) throw new BusinessException(409,"项目已归档或不存在");
        Integer previousStatus=task.getStatus();
        if(!access.canManageProject(task.getProjectId()) && updates.containsKey("assigneeId") && !Objects.equals(toLong(updates.get("assigneeId")),task.getAssigneeId()) && !Objects.equals(toLong(updates.get("assigneeId")),access.userId())) throw new BusinessException(403,"仅项目管理员可以分派其他成员");
        if (updates.containsKey("priority")) validateRange((Integer)updates.get("priority"),1,4,"优先级");
        if (updates.containsKey("status")) validateRange((Integer)updates.get("status"),0,3,"任务状态");
        if (updates.containsKey("progress")) validateRange((Integer)updates.get("progress"),0,100,"进度");
        if (updates.containsKey("description") && Objects.toString(updates.get("description"),"").length()>5000) throw new BusinessException(400,"说明最多5000字");
        if (updates.containsKey("title") && (updates.get("title")==null || updates.get("title").toString().isBlank() || updates.get("title").toString().length()>200)) throw new com.collaboffice.common.exception.BusinessException(400,"请填写1到200字的标题");
        Long assignee = toLong(updates.get("assigneeId"));
        if (assignee != null && jdbc.queryForObject("SELECT COUNT(*) FROM task_project_member pm JOIN task_project p ON p.project_id=pm.project_id WHERE p.project_id=? AND pm.user_id=? AND p.deleted=0",Integer.class,task.getProjectId(),assignee)==0) throw new com.collaboffice.common.exception.BusinessException(400,"负责人必须属于项目成员");
        if (updates.containsKey("title")) task.setTitle((String) updates.get("title"));
        if (updates.containsKey("status")) task.setStatus((Integer) updates.get("status"));
        if (updates.containsKey("priority")) task.setPriority((Integer) updates.get("priority"));
        if (updates.containsKey("assigneeId")) task.setAssigneeId(toLong(updates.get("assigneeId")));
        if (updates.containsKey("description")) task.setDescription((String)updates.get("description"));
        if (updates.containsKey("dueDate") && updates.get("dueDate") != null && !updates.get("dueDate").toString().isBlank()) task.setDueDate(LocalDate.parse(updates.get("dueDate").toString()));
        if (updates.containsKey("progress")) task.setProgress((Integer) updates.get("progress"));
        if(updates.containsKey("parentId")) {
            String parent=Objects.toString(updates.get("parentId"),"");
            if(!parent.isBlank()) {
                TaskTask parentTask=findTask(parent);
                if(parent.equals(taskId) || parentTask.getParentId()!=null || !Objects.equals(parentTask.getProjectId(),task.getProjectId()))
                    throw new BusinessException(400,"子任务必须归属于同一项目的顶层任务");
                task.setParentId(parent);
            }
        }
        if(updates.containsKey("tags")) {
            String tags=Objects.toString(updates.get("tags"),"");
            if(tags.length()>500) throw new BusinessException(400,"标签最多500字");
            task.setTags(tags);
        }
        if(updates.containsKey("attachments")) {
            if(!(updates.get("attachments") instanceof List<?> files) || files.size()>20) throw new BusinessException(400,"最多20个附件");
            List<Map<String,Object>> safe=new ArrayList<>();
            for(Object entry:files) {
                if(!(entry instanceof Map<?,?> file) || file.get("fileId")==null) throw new BusinessException(400,"附件无效");
                String id=file.get("fileId").toString();access.requireFile(id,false);
                var stored=access.row("SELECT file_name,space_id FROM file_file WHERE file_id=? AND deleted=0",id);
                if(!Objects.equals(((Number)stored.get("space_id")).longValue(),project.getSpaceId())) throw new BusinessException(400,"附件必须属于当前团队");
                safe.add(Map.of("fileId",id,"fileName",stored.get("file_name")));
            }
            task.setAttachments(JSON.toJSONString(safe));
        }
        if(updates.containsKey("repeatRule")) {String rule=Objects.toString(updates.get("repeatRule"),"none");TaskSchedule.validateRule(rule);task.setRepeatRule(rule);}
        if(updates.containsKey("reminderEnabled")) {validateRange((Integer)updates.get("reminderEnabled"),0,1,"提醒开关");task.setReminderEnabled((Integer)updates.get("reminderEnabled"));}
        if(updates.containsKey("reminderDays")) {validateRange((Integer)updates.get("reminderDays"),0,30,"提前天数");task.setReminderDays((Integer)updates.get("reminderDays"));}
        if(updates.containsKey("reminderFrequency")) {
            String frequency=Objects.toString(updates.get("reminderFrequency"),"");
            if(!Set.of("once","daily","hourly").contains(frequency)) throw new BusinessException(400,"提醒频率无效");
            task.setReminderFrequency(frequency);
        }
        if(updates.containsKey("status")) task.setColumnId(null);
        if (task.getStatus()==2) {
            if(jdbc.queryForObject("SELECT COUNT(*) FROM task_dependency d JOIN task_task t ON t.task_id=d.depends_on_task_id WHERE d.task_id=? AND t.status<>2 AND t.deleted=0",Integer.class,taskId)>0)
                throw new BusinessException(409,"前置任务尚未完成");
            task.setProgress(100);
            if(!Integer.valueOf(2).equals(previousStatus)) task.setCompletedDate(LocalDateTime.now());
            createNextOccurrence(task);
        } else if(Integer.valueOf(2).equals(previousStatus) && Integer.valueOf(100).equals(task.getProgress())) task.setProgress(0);
        taskMapper.updateById(task);
        var clear = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TaskTask>().eq(TaskTask::getTaskId,taskId);
        boolean needsClear=false;
        if(updates.containsKey("assigneeId") && assignee==null) {clear.set(TaskTask::getAssigneeId,null);needsClear=true;}
        if(updates.containsKey("dueDate") && (updates.get("dueDate")==null || updates.get("dueDate").toString().isBlank())) {clear.set(TaskTask::getDueDate,null);needsClear=true;}
        if(task.getStatus()!=2) {clear.set(TaskTask::getCompletedDate,null);needsClear=true;}
        if(needsClear) taskMapper.update(null,clear);
        publishTaskIndexEvent(task, "UPDATE");
    }

    @Override
    @Transactional
    public void deleteTask(String taskId) {
        requireEditable(taskId);
        TaskTask task = findTask(taskId);
        if(!access.canManageProject(task.getProjectId()) && !Objects.equals(task.getCreatorId(),access.userId())) throw new BusinessException(403,"仅创建者或项目管理员可删除任务");
        taskMapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TaskTask>()
                .eq(TaskTask::getParentId,taskId).set(TaskTask::getParentId,null));
        taskMapper.deleteById(task.getId());
        publishTaskIndexEvent(task, "DELETE");
    }

    @Override
    @Transactional
    public String createProject(Long userId, String name, Long spaceId) {
        access.requireProjectCreator();access.requireSpace(spaceId,true);
        if(name==null || name.isBlank() || name.length()>100) throw new BusinessException(400,"项目名称为1到100字");
        TaskProject project = new TaskProject();
        project.setProjectId(SnowflakeIdGenerator.nextIdStr());
        project.setName(name);
        project.setSpaceId(spaceId);
        project.setCreateBy(userId);
        project.setStatus(1);
        project.setProgress(0);
        projectMapper.insert(project);
        jdbc.update("INSERT INTO task_project_member(project_id,user_id) VALUES (?,?)",project.getProjectId(),userId);
        return project.getProjectId();
    }

    @Override
    public List<Map<String, Object>> getProjectList(Long spaceId) {
        return projectMapper.selectBySpaceId(spaceId).stream().filter(p->access.canReadProject(p.getProjectId())).map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("projectId", p.getProjectId());
            m.put("canManage",access.canManageProject(p.getProjectId()));
            m.put("memberIds",jdbc.queryForList("SELECT user_id FROM task_project_member WHERE project_id=?",Long.class,p.getProjectId()));
            m.put("name", p.getName());
            m.put("spaceId", p.getSpaceId());
            m.put("status", p.getStatus());
            m.put("progress", p.getProgress());
            m.put("description",p.getDescription());
            m.put("milestones",p.getMilestonesJson()==null?List.of():JSON.parseArray(p.getMilestonesJson()));
            return m;
        }).collect(Collectors.toList());
    }

    private TaskTask findTask(String taskId) {
        TaskTask task = taskMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskTask>()
                        .eq(TaskTask::getTaskId, taskId));
        if (task == null) throw new com.collaboffice.common.exception.BusinessException(404,"任务不存在");
        access.requireTask(taskId,false);
        return task;
    }

    private void createNextOccurrence(TaskTask task) {
        String rule=task.getRepeatRule()==null?"none":task.getRepeatRule();
        if("none".equals(rule) || task.getRepeatNextId()!=null) return;
        TaskTask next=new TaskTask();
        next.setTaskId(SnowflakeIdGenerator.nextIdStr());next.setProjectId(task.getProjectId());
        next.setTitle(task.getTitle());next.setDescription(task.getDescription());next.setParentId(task.getParentId());
        next.setCreatorId(task.getCreatorId());next.setAssigneeId(task.getAssigneeId());next.setPriority(task.getPriority());
        next.setTags(task.getTags());next.setAttachments(task.getAttachments());next.setStatus(0);next.setProgress(0);
        next.setRepeatRule(rule);next.setDueDate(TaskSchedule.nextDate(task.getDueDate(),rule,LocalDate.now()));
        next.setReminderEnabled(task.getReminderEnabled());next.setReminderDays(task.getReminderDays());next.setReminderFrequency(task.getReminderFrequency());
        taskMapper.insert(next);task.setRepeatNextId(next.getTaskId());publishTaskIndexEvent(next,"UPDATE");
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.valueOf(v.toString());
    }

    private void validateRange(Integer value, int min, int max, String field) {
        if (value==null || value<min || value>max) throw new com.collaboffice.common.exception.BusinessException(400,field+"无效");
    }

    @Override
    public Map<String, List<Map<String, Object>>> getKanbanData(String projectId) {
        List<TaskTask> tasks = scopedTasks(projectId);
        Map<String, List<Map<String, Object>>> kanban = new LinkedHashMap<>();
        kanban.put("todo", new ArrayList<>());
        kanban.put("in_progress", new ArrayList<>());
        kanban.put("done", new ArrayList<>());

        for (TaskTask t : tasks) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getTaskId());
            m.put("title", t.getTitle());
            m.put("priority", t.getPriority());
            m.put("assigneeId", t.getAssigneeId());
            m.put("assigneeName", t.getAssigneeName());
            m.put("dueDate", t.getDueDate());
            m.put("progress", t.getProgress());
            List<String> tags = new ArrayList<>();
            if (t.getTags() != null) tags.add(t.getTags());
            m.put("tags", tags);

            if (t.getStatus() == null || t.getStatus() == 0) kanban.get("todo").add(m);
            else if (t.getStatus() == 1) kanban.get("in_progress").add(m);
            else if (t.getStatus() == 2) kanban.get("done").add(m);
        }
        return kanban;
    }

    @Override
    public List<Map<String, Object>> getCalendarTasks(String projectId, String startDate, String endDate) {
        List<TaskTask> tasks = scopedTasks(projectId);
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().withDayOfMonth(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : start.plusMonths(1);

        return tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> !t.getDueDate().isBefore(start) && !t.getDueDate().isAfter(end))
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("taskId", t.getTaskId());
                    m.put("title", t.getTitle());
                    m.put("status", t.getStatus());
                    m.put("priority", t.getPriority());
                    m.put("dueDate", t.getDueDate().toString());
                    return m;
                }).collect(Collectors.toList());
    }

    public List<TaskTask> getDueSoonTasks() {
        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(2);
        return taskMapper.selectList(null).stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> !t.getDueDate().isBefore(today) && !t.getDueDate().isAfter(soon))
                .filter(t -> t.getStatus() != null && t.getStatus() < 2)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTaskComment(Long userId, String taskId, String content) {
        access.requireTask(taskId,false);
        if(content==null || content.isBlank() || content.length()>5000) throw new com.collaboffice.common.exception.BusinessException(400,"评论长度为1到5000字");
        TaskComment c = new TaskComment();
        c.setTaskId(taskId);
        c.setUserId(userId);
        c.setContent(content);
        c.setCreateTime(LocalDateTime.now());
        c.setUserName(commentMapper.getRealNameByUserId(userId));
        commentMapper.insert(c);
    }

    private void publishTaskIndexEvent(TaskTask task, String action) {
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("id", task.getTaskId());
            event.put("action", action);
            if (!"DELETE".equalsIgnoreCase(action)) {
                event.put("title", task.getTitle());
                event.put("description", task.getDescription());
                event.put("status", task.getStatus());
                event.put("assigneeId", task.getAssigneeId());

                Long spaceId = null;
                if (task.getProjectId() != null) {
                    TaskProject project = projectMapper.selectOne(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TaskProject>()
                                    .eq(TaskProject::getProjectId, task.getProjectId()));
                    if (project != null) {
                        spaceId = project.getSpaceId();
                    }
                }
                event.put("spaceId", spaceId);
            }
            rabbitTemplate.convertAndSend("search-index-exchange", "search.task." + action.toLowerCase(), event);
        } catch (Exception e) {
            log.error("Failed to publish task index event for taskId={}", task.getTaskId(), e);
        }
    }

    private void requireEditable(String taskId) {
        access.requireTask(taskId,true);
        var p=access.row("SELECT p.status FROM task_project p JOIN task_task t ON t.project_id=p.project_id WHERE t.task_id=? AND t.deleted=0 AND p.deleted=0",taskId);
        if(((Number)p.get("status")).intValue()==2) throw new BusinessException(409,"项目已归档");
    }

    private List<TaskTask> scopedTasks(String projectId) {
        Set<String> ids = projectMapper.selectBySpaceId(access.currentSpace()).stream().map(TaskProject::getProjectId).filter(access::canReadProject).collect(Collectors.toSet());
        return taskMapper.selectList(null).stream().filter(t -> ids.contains(t.getProjectId()) && (projectId==null || projectId.equals(t.getProjectId()))).toList();
    }
}
