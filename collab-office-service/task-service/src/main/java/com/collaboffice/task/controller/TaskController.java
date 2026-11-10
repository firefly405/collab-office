package com.collaboffice.task.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final com.collaboffice.task.service.KanbanService kanban;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final com.collaboffice.task.service.MeetingTaskService meetingTasks;

    @PostMapping("/from-meeting")
    public CommonResult<Map<String,String>> fromMeeting(@RequestBody MeetingTaskRequest request){return CommonResult.success(meetingTasks.convert(request.meetingId(),request.itemIds(),request.projectId()));}
    public record MeetingTaskRequest(String meetingId,List<String> itemIds,String projectId){}

    @PostMapping("/create")
    @org.springframework.transaction.annotation.Transactional
    public CommonResult<String> create(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam(required = false) String title,
                                       @RequestParam(required = false) String projectId,
                                       @RequestBody(required = false) java.util.Map<String,Object> body) {
        java.util.Map<String,Object> data = body == null ? new java.util.HashMap<>() : body;
        if (body != null) { title = (String)body.get("title"); projectId = (String)body.get("projectId"); }
        if (title == null || title.isBlank() || title.length() > 200) throw new com.collaboffice.common.exception.BusinessException(400, "任务标题长度为1到200字");
        Long spaceId = access.requireSpace(access.currentSpace(), true);
        if (projectId == null || projectId.isBlank()) {
            var projects = taskService.getProjectList(spaceId);
            projectId = projects.isEmpty() ? taskService.createProject(userId, "团队任务", spaceId) : (String)projects.get(0).get("projectId");
        }
        String id = taskService.createTask(userId, title, projectId);
        taskService.updateTask(id, data);
        if(data.get("columnId")!=null) kanban.moveTaskToColumn(id,Long.valueOf(data.get("columnId").toString()));
        return CommonResult.success(id);
    }

    @GetMapping("/list")
    public CommonResult<List<Map<String, Object>>> list(@RequestParam(required = false) String projectId,
                                                         @RequestParam(required = false) Integer status) {
        return CommonResult.success(taskService.getTaskList(projectId, status));
    }

    @GetMapping("/{taskId}")
    public CommonResult<Map<String, Object>> detail(@PathVariable String taskId) {
        return CommonResult.success(taskService.getTaskDetail(taskId));
    }

    @PutMapping("/{taskId}")
    @org.springframework.transaction.annotation.Transactional
    public CommonResult<Void> update(@PathVariable String taskId, @RequestBody Map<String, Object> updates) {
        taskService.updateTask(taskId, updates);
        if(updates.get("columnId")!=null) kanban.moveTaskToColumn(taskId,Long.valueOf(updates.get("columnId").toString()));
        return CommonResult.success(null);
    }

    @PutMapping("/{taskId}/status")
    public CommonResult<Void> updateStatus(@PathVariable String taskId, @RequestParam Integer status) {
        taskService.updateTaskStatus(taskId, status);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{taskId}")
    public CommonResult<Void> delete(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return CommonResult.success(null);
    }

    @PostMapping("/project/create")
    public CommonResult<String> createProject(@RequestHeader("X-User-Id") Long userId,
                                              @RequestParam String name,
                                              @RequestParam Long spaceId) {
        access.requireSpace(spaceId, true);
        return CommonResult.success(taskService.createProject(userId, name, spaceId));
    }

    @GetMapping("/project/list")
    public CommonResult<List<Map<String, Object>>> listProjects(@RequestParam Long spaceId) {
        access.requireSpace(spaceId, false);
        return CommonResult.success(taskService.getProjectList(spaceId));
    }

    @GetMapping("/kanban")
    public CommonResult<Map<String, List<Map<String, Object>>>> kanban(@RequestParam(required = false) String projectId) {
        return CommonResult.success(taskService.getKanbanData(projectId));
    }

    @GetMapping("/calendar")
    public CommonResult<List<Map<String, Object>>> calendar(@RequestParam(required = false) String projectId,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate) {
        return CommonResult.success(taskService.getCalendarTasks(projectId, startDate, endDate));
    }

    @PostMapping("/{taskId}/comment")
    public CommonResult<Void> addComment(@RequestHeader("X-User-Id") Long userId,
                                         @PathVariable String taskId,
                                         @RequestBody Map<String, String> body) {
        String content = body.get("content");
        taskService.addTaskComment(userId, taskId, content);
        return CommonResult.success(null);
    }
}
