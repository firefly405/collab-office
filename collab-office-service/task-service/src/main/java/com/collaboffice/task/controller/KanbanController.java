package com.collaboffice.task.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.task.service.KanbanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 看板高级功能控制器：自定义列 CRUD、子任务、任务依赖、任务移动（WIP检查）
 */
@Tag(name = "看板高级功能", description = "WIP限制、自定义列、子任务、任务依赖")
@RestController
@RequestMapping("/api/v1/kanban")
@RequiredArgsConstructor
public class KanbanController {

    private final KanbanService kanbanService;
    private final com.collaboffice.task.service.ProjectService projects;
    private final com.collaboffice.common.security.WorkspaceAccess access;

    // =========== 自定义列 CRUD ===========

    @Operation(summary = "获取项目看板列（含WIP配置和当前任务数）")
    @GetMapping("/{projectId}/columns")
    public CommonResult<List<Map<String, Object>>> getColumns(@PathVariable String projectId) {
        projects.require(projectId,false);
        return CommonResult.success(kanbanService.getColumns(projectId));
    }

    @Operation(summary = "创建自定义看板列")
    @PostMapping("/{projectId}/columns")
    public CommonResult<Map<String, Object>> createColumn(@PathVariable String projectId,
                                                           @RequestBody CreateColumnRequest request) {
        projects.require(projectId,true);
        access.requireProject(projectId,true);
        if(request.getName()==null) throw new com.collaboffice.common.exception.BusinessException(400,"请输入列名称");
        return CommonResult.success(kanbanService.createColumn(
                projectId, request.getName(), request.getWipLimit(),
                request.getSortOrder(), request.getColor(), request.getIsDoneColumn()));
    }

    @Operation(summary = "更新看板列（名称/WIP限制/排序/颜色）")
    @PutMapping("/columns/{columnId}")
    public CommonResult<Void> updateColumn(@PathVariable Long columnId,
                                            @RequestBody UpdateColumnRequest request) {
        projects.requireColumn(columnId,true);
        kanbanService.updateColumn(columnId, request.getName(), request.getWipLimit(),
                request.getSortOrder(), request.getColor());
        return CommonResult.success(null);
    }

    @Operation(summary = "删除看板列")
    @DeleteMapping("/columns/{columnId}")
    public CommonResult<Void> deleteColumn(@PathVariable Long columnId) {
        projects.requireColumn(columnId,true);
        kanbanService.deleteColumn(columnId);
        return CommonResult.success(null);
    }

    @Operation(summary = "将任务移动到指定看板列（自动检查WIP上限和前置依赖）")
    @PostMapping("/columns/{columnId}/move-task")
    public CommonResult<Void> moveTaskToColumn(@PathVariable Long columnId,
                                                @RequestParam String taskId) {
        projects.requireTask(taskId,true);projects.requireColumn(columnId,false);
        projects.sameProject(taskId,columnId.toString(),true);
        kanbanService.moveTaskToColumn(taskId, columnId);
        return CommonResult.success(null);
    }

    // =========== 子任务 ===========

    @Operation(summary = "获取任务的子任务列表")
    @GetMapping("/task/{taskId}/subtasks")
    public CommonResult<List<Map<String, Object>>> getSubtasks(@PathVariable String taskId) {
        projects.requireTask(taskId,false);
        return CommonResult.success(kanbanService.getSubtasks(taskId));
    }

    // =========== 任务依赖 ===========

    @Operation(summary = "获取任务的依赖关系（前置任务列表）")
    @GetMapping("/task/{taskId}/dependencies")
    public CommonResult<List<Map<String, Object>>> getDependencies(@PathVariable String taskId) {
        projects.requireTask(taskId,false);
        return CommonResult.success(kanbanService.getDependencies(taskId));
    }

    @Operation(summary = "添加任务依赖（taskId 依赖 dependsOnTaskId 先完成）")
    @PostMapping("/task/{taskId}/dependencies")
    public CommonResult<Void> addDependency(@PathVariable String taskId,
                                             @RequestHeader("X-User-Id") Long userId,
                                             @RequestParam String dependsOnTaskId) {
        projects.requireTask(taskId,true);projects.requireTask(dependsOnTaskId,false);
        access.requireProject(access.row("SELECT project_id FROM task_task WHERE task_id=? AND deleted=0",taskId).get("project_id").toString(),true);
        projects.sameProject(taskId,dependsOnTaskId,false);
        kanbanService.addDependency(taskId, dependsOnTaskId, userId);
        return CommonResult.success(null);
    }

    @Operation(summary = "删除任务依赖关系")
    @DeleteMapping("/task/{taskId}/dependencies")
    public CommonResult<Void> removeDependency(@PathVariable String taskId,
                                                @RequestParam String dependsOnTaskId) {
        projects.requireTask(taskId,true);
        access.requireProject(access.row("SELECT project_id FROM task_task WHERE task_id=? AND deleted=0",taskId).get("project_id").toString(),true);
        kanbanService.removeDependency(taskId, dependsOnTaskId);
        return CommonResult.success(null);
    }

    // =========== 请求 DTO ===========

    @Data
    public static class CreateColumnRequest {
        private String name;
        private Integer wipLimit;
        private Integer sortOrder;
        private String color;
        private Integer isDoneColumn;
    }

    @Data
    public static class UpdateColumnRequest {
        private String name;
        private Integer wipLimit;
        private Integer sortOrder;
        private String color;
    }
}
