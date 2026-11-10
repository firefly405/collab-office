package com.collaboffice.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collaboffice.task.entity.KanbanColumn;
import com.collaboffice.task.entity.TaskDependency;
import com.collaboffice.task.entity.TaskTask;
import com.collaboffice.task.exception.WipLimitExceededException;
import com.collaboffice.task.mapper.KanbanColumnMapper;
import com.collaboffice.task.mapper.TaskDependencyMapper;
import com.collaboffice.task.mapper.TaskTaskMapper;
import com.collaboffice.task.service.KanbanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 看板高级功能实现：WIP 在制品限制、自定义列 CRUD、子任务、任务依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanServiceImpl implements KanbanService {

    private final KanbanColumnMapper columnMapper;
    private final TaskTaskMapper taskMapper;
    private final TaskDependencyMapper dependencyMapper;
    private final com.collaboffice.task.service.TaskService taskService;

    @Override
    public List<Map<String, Object>> getColumns(String projectId) {
        return columnMapper.selectByProjectId(projectId).stream().map(col -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", col.getId());
            m.put("projectId", col.getProjectId());
            m.put("name", col.getName());
            m.put("sortOrder", col.getSortOrder());
            m.put("wipLimit", col.getWipLimit());
            m.put("color", col.getColor());
            m.put("isDoneColumn", col.getIsDoneColumn());
            // 当前列实际任务数
            m.put("currentTaskCount", columnMapper.countActiveTasksInColumn(col.getId()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> createColumn(String projectId, String name, Integer wipLimit,
                                             Integer sortOrder, String color, Integer isDoneColumn) {
        validateColumn(name,wipLimit,color);
        KanbanColumn col = new KanbanColumn();
        col.setProjectId(projectId);
        col.setName(name);
        col.setWipLimit(wipLimit != null ? wipLimit : 0);
        col.setSortOrder(sortOrder != null ? sortOrder : 999);
        col.setColor(color);
        col.setIsDoneColumn(isDoneColumn != null ? isDoneColumn : 0);
        col.setCreateTime(LocalDateTime.now());
        col.setUpdateTime(LocalDateTime.now());
        columnMapper.insert(col);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", col.getId());
        result.put("name", col.getName());
        result.put("wipLimit", col.getWipLimit());
        return result;
    }

    @Override
    @Transactional
    public void updateColumn(Long columnId, String name, Integer wipLimit, Integer sortOrder, String color) {
        validateColumn(name,wipLimit,color);
        KanbanColumn col = columnMapper.selectById(columnId);
        if (col == null) throw new com.collaboffice.common.exception.BusinessException(404,"看板列不存在: " + columnId);
        if (name != null) col.setName(name);
        if (wipLimit != null) col.setWipLimit(wipLimit);
        if (sortOrder != null) col.setSortOrder(sortOrder);
        if (color != null) col.setColor(color);
        col.setUpdateTime(LocalDateTime.now());
        columnMapper.updateById(col);
    }

    @Override
    @Transactional
    public void deleteColumn(Long columnId) {
        KanbanColumn col = columnMapper.selectById(columnId);
        if (col == null) throw new com.collaboffice.common.exception.BusinessException(404,"看板列不存在: " + columnId);
        if(columnMapper.countTasks(columnId)>0) throw new com.collaboffice.common.exception.BusinessException(409,"请先移出列内任务");
        columnMapper.deleteById(columnId);
        log.info("Deleted kanban column: {}", columnId);
    }

    @Override
    @Transactional
    public void moveTaskToColumn(String taskId, Long targetColumnId) {
        columnMapper.lockColumn(targetColumnId);
        // 1. 查找目标列
        KanbanColumn targetCol = columnMapper.selectById(targetColumnId);
        if (targetCol == null) throw new com.collaboffice.common.exception.BusinessException(404,"目标看板列不存在: " + targetColumnId);
        TaskTask task = taskMapper.selectOne(new LambdaQueryWrapper<TaskTask>().eq(TaskTask::getTaskId,taskId));
        if(task==null) throw new com.collaboffice.common.exception.BusinessException(404,"任务不存在");
        if(!Objects.equals(task.getProjectId(),targetCol.getProjectId())) throw new com.collaboffice.common.exception.BusinessException(400,"任务与看板列必须属于同一项目");
        if(Objects.equals(task.getColumnId(),targetColumnId)) return;

        // 2. WIP 限制检查
        int wipLimit = targetCol.getWipLimit() != null ? targetCol.getWipLimit() : 0;
        if (wipLimit > 0 && !Integer.valueOf(1).equals(targetCol.getIsDoneColumn())) {
            int currentCount = columnMapper.countActiveTasksInColumn(targetColumnId);
            if (currentCount >= wipLimit) {
                throw new WipLimitExceededException(targetColumnId, currentCount, wipLimit);
            }
        }

        // 3. 如果目标列是完成列，检查前置依赖是否全部完成
        if (Integer.valueOf(1).equals(targetCol.getIsDoneColumn())) {
            int unfinishedDeps = dependencyMapper.countUnfinishedDependencies(taskId);
            if (unfinishedDeps > 0) {
                throw new com.collaboffice.common.exception.BusinessException(409,String.format(
                        "DEPENDENCY_NOT_FULFILLED: 任务 [%s] 有 %d 个前置依赖尚未完成", taskId, unfinishedDeps));
            }
        }

        // 4. 更新任务列（使用 columnId 字段）

        taskService.updateTaskStatus(taskId,Integer.valueOf(1).equals(targetCol.getIsDoneColumn())?2:1);
        taskMapper.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TaskTask>()
                .eq(TaskTask::getTaskId,taskId).set(TaskTask::getColumnId,targetColumnId));
        log.info("Task {} moved to column {}", taskId, targetColumnId);
    }

    @Override
    public List<Map<String, Object>> getSubtasks(String parentTaskId) {
        List<TaskTask> subtasks = taskMapper.selectList(
                new LambdaQueryWrapper<TaskTask>()
                        .eq(TaskTask::getParentId, parentTaskId)
                        .orderByAsc(TaskTask::getSortOrder));
        return subtasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getTaskId());
            m.put("title", t.getTitle());
            m.put("status", t.getStatus());
            m.put("priority", t.getPriority());
            m.put("assigneeId", t.getAssigneeId());
            m.put("dueDate", t.getDueDate());
            m.put("parentId", t.getParentId());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDependencies(String taskId) {
        List<TaskDependency> deps = dependencyMapper.selectByTaskId(taskId);
        return deps.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", d.getTaskId());
            m.put("dependsOnTaskId", d.getDependsOnTaskId());
            m.put("createTime", d.getCreateTime());

            // 查询被依赖任务的状态
            TaskTask depTask = taskMapper.selectOne(
                    new LambdaQueryWrapper<TaskTask>().eq(TaskTask::getTaskId, d.getDependsOnTaskId()));
            if (depTask != null) {
                m.put("dependsOnTitle", depTask.getTitle());
                m.put("dependsOnStatus", depTask.getStatus());
                m.put("isCompleted", depTask.getStatus() != null && depTask.getStatus() == 2);
            }
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addDependency(String taskId, String dependsOnTaskId, Long creatorId) {
        if(taskId.equals(dependsOnTaskId)) throw new com.collaboffice.common.exception.BusinessException(400,"CIRCULAR_DEPENDENCY: 不能依赖自身");
        // 检查循环依赖（简单检查：dependsOnTaskId 是否依赖于 taskId）
        int circular = dependencyMapper.selectList(
                new LambdaQueryWrapper<TaskDependency>()
                        .eq(TaskDependency::getTaskId, dependsOnTaskId)
                        .eq(TaskDependency::getDependsOnTaskId, taskId)).size();
        if (circular > 0) {
            throw new com.collaboffice.common.exception.BusinessException(400,"CIRCULAR_DEPENDENCY: 检测到循环依赖，不允许添加");
        }
        Set<String> visited=new HashSet<>();
        Deque<String> pending=new ArrayDeque<>();pending.add(dependsOnTaskId);
        while(!pending.isEmpty()) {
            String node=pending.removeFirst();
            if(!visited.add(node)) continue;
            if(node.equals(taskId)) throw new com.collaboffice.common.exception.BusinessException(400,"CIRCULAR_DEPENDENCY: 检测到循环依赖");
            for(TaskDependency dep:dependencyMapper.selectByTaskId(node)) pending.add(dep.getDependsOnTaskId());
        }

        // 检查是否已存在
        long existing = dependencyMapper.selectCount(
                new LambdaQueryWrapper<TaskDependency>()
                        .eq(TaskDependency::getTaskId, taskId)
                        .eq(TaskDependency::getDependsOnTaskId, dependsOnTaskId));
        if (existing > 0) {
            throw new com.collaboffice.common.exception.BusinessException(409,"依赖关系已存在");
        }

        TaskDependency dep = new TaskDependency();
        dep.setTaskId(taskId);
        dep.setDependsOnTaskId(dependsOnTaskId);
        dep.setCreatorId(creatorId);
        dep.setCreateTime(LocalDateTime.now());
        dependencyMapper.insert(dep);
    }

    @Override
    @Transactional
    public void removeDependency(String taskId, String dependsOnTaskId) {
        dependencyMapper.delete(
                new LambdaQueryWrapper<TaskDependency>()
                        .eq(TaskDependency::getTaskId, taskId)
                        .eq(TaskDependency::getDependsOnTaskId, dependsOnTaskId));
    }

    private void validateColumn(String name,Integer wip,String color) {
        if(name!=null && (name.isBlank() || name.length()>50) || wip!=null && (wip<0 || wip>10000)
                || color!=null && !color.matches("#[0-9a-fA-F]{6}"))
            throw new com.collaboffice.common.exception.BusinessException(400,"列名称、WIP或颜色无效");
    }
}
