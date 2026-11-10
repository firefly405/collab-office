package com.collaboffice.task.service;

import java.util.*;

public interface TaskService {
    String createTask(Long userId, String title, String projectId);
    Map<String, Object> getTaskDetail(String taskId);
    List<Map<String, Object>> getTaskList(String projectId, Integer status);
    void updateTaskStatus(String taskId, Integer status);
    void updateTask(String taskId, Map<String, Object> updates);
    void deleteTask(String taskId);
    String createProject(Long userId, String name, Long spaceId);
    List<Map<String, Object>> getProjectList(Long spaceId);
    Map<String, List<Map<String, Object>>> getKanbanData(String projectId);
    List<Map<String, Object>> getCalendarTasks(String projectId, String startDate, String endDate);
    void addTaskComment(Long userId, String taskId, String content);
}
