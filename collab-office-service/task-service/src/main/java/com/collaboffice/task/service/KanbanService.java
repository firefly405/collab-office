package com.collaboffice.task.service;

import java.util.List;
import java.util.Map;

/**
 * 看板高级功能服务接口
 */
public interface KanbanService {

    /** 获取项目的所有自定义列（含WIP配置） */
    List<Map<String, Object>> getColumns(String projectId);

    /** 创建自定义列 */
    Map<String, Object> createColumn(String projectId, String name, Integer wipLimit,
                                      Integer sortOrder, String color, Integer isDoneColumn);

    /** 更新自定义列（名称/WIP限制/排序） */
    void updateColumn(Long columnId, String name, Integer wipLimit, Integer sortOrder, String color);

    /** 删除自定义列 */
    void deleteColumn(Long columnId);

    /**
     * 将任务移动到指定列（检查 WIP 上限）
     * @throws com.collaboffice.task.exception.WipLimitExceededException 超出 WIP 上限
     */
    void moveTaskToColumn(String taskId, Long targetColumnId);

    /** 获取子任务列表 */
    List<Map<String, Object>> getSubtasks(String parentTaskId);

    /** 获取任务依赖关系 */
    List<Map<String, Object>> getDependencies(String taskId);

    /** 添加任务依赖 */
    void addDependency(String taskId, String dependsOnTaskId, Long creatorId);

    /** 移除任务依赖 */
    void removeDependency(String taskId, String dependsOnTaskId);
}
