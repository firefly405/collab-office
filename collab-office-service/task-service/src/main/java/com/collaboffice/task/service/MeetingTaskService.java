package com.collaboffice.task.service;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;

@Service @RequiredArgsConstructor
public class MeetingTaskService {
 private final JdbcTemplate jdbc;private final WorkspaceAccess access;private final TaskService tasks;
 @Transactional public Map<String,String> convert(String meetingId,List<String> itemIds,String projectId){
  if(meetingId==null||!meetingId.matches("[0-9]{1,30}")||itemIds==null||itemIds.isEmpty()||itemIds.size()>100||new HashSet<>(itemIds).size()!=itemIds.size())throw new BusinessException(400,"请选择有效纪要事项");
  access.requireMeeting(meetingId,true);Long space=access.requireSpace(access.currentSpace(),true);
  jdbc.queryForList("SELECT id FROM meeting_room WHERE room_id=? AND deleted=0 FOR UPDATE",meetingId);
  var rows=jdbc.queryForList("SELECT id,todo_list_json FROM meeting_record WHERE meeting_id=?",meetingId);if(rows.isEmpty())throw new BusinessException(400,"请先保存纪要");
  var record=rows.get(0);var todos=JSON.parseArray(Objects.toString(record.get("todo_list_json"),"[]"));
  if(!todos.stream().map(o->((Map<?,?>)o).get("id").toString()).toList().containsAll(itemIds))throw new BusinessException(400,"事项不存在");
  if(projectId==null||projectId.isBlank()){var projects=tasks.getProjectList(space);projectId=projects.stream().filter(p->!Objects.equals(p.get("status"),2)).map(p->p.get("projectId").toString()).findFirst().orElse(null);if(projectId==null)projectId=tasks.createProject(access.userId(),"会议待办",space);}
  else if(jdbc.queryForObject("SELECT COUNT(*) FROM task_project WHERE project_id=? AND space_id=? AND deleted=0 AND status<>2",Integer.class,projectId,space)==0)throw new BusinessException(403,"项目不属于当前团队或已归档");
  Map<String,String> result=new LinkedHashMap<>();
  for(int i=0;i<todos.size();i++){
   var todo=todos.getJSONObject(i);String id=todo.getString("id");if(!itemIds.contains(id))continue;
   if(!Objects.equals(todo.getLong("spaceId"),space))throw new BusinessException(403,"事项属于其他团队，请切换团队");
   String taskId=todo.getString("taskId");
   if(taskId==null||taskId.isBlank()){
    String assignee=Objects.toString(todo.getString("assigneeId"),"");if(!assignee.isBlank()&&jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=? AND (expires_at IS NULL OR expires_at>NOW())",Integer.class,space,assignee)==0)throw new BusinessException(400,"负责人已不属于有效团队成员");
    taskId=tasks.createTask(access.userId(),todo.getString("title"),projectId);Map<String,Object> data=new HashMap<>();data.put("description","来自会议纪要："+meetingId);data.put("dueDate",todo.getString("dueDate"));if(!assignee.isBlank())data.put("assigneeId",assignee);tasks.updateTask(taskId,data);todo.put("taskId",taskId);
   }
   result.put(id,taskId);
  }
  jdbc.update("UPDATE meeting_record SET todo_list_json=? WHERE id=?",todos.toJSONString(),record.get("id"));return result;
 }
}
