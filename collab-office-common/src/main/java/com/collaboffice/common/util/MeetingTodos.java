package com.collaboffice.common.util;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import java.util.*;
import java.time.LocalDate;

public final class MeetingTodos {
 private MeetingTodos(){}
 public static List<Map<String,Object>> normalize(List<?> input,List<?> previous,Long space){
  if(input==null||input.size()>100)throw new BusinessException(400,"最多100个纪要事项");
  Map<String,Map<?,?>> old=new HashMap<>();for(Object item:previous)if(item instanceof Map<?,?> m)old.put(Objects.toString(m.get("id"),""),m);
  Set<String> seen=new HashSet<>();List<Map<String,Object>> result=new ArrayList<>();
  for(Object item:input){
   if(!(item instanceof Map<?,?> m))throw new BusinessException(400,"事项格式无效");
   String id=Objects.toString(m.get("id"),""),title=Objects.toString(m.get("title"),"").trim();
   if(!id.matches("[A-Za-z0-9_-]{1,64}")||!seen.add(id)||title.isEmpty()||title.length()>200)throw new BusinessException(400,"事项标识重复或标题无效");
   Map<?,?> before=old.getOrDefault(id,Map.of());String task=Objects.toString(before.get("taskId"),"");
   if(!Objects.toString(m.get("taskId"),"").equals(task))throw new BusinessException(409,"任务关联已变化，请重新载入纪要");
   var safe=new LinkedHashMap<String,Object>();safe.put("id",id);safe.put("title",title);safe.put("spaceId",before.get("spaceId")==null?space:before.get("spaceId"));
   String assignee=Objects.toString(m.get("assigneeId"),"");if(!assignee.isEmpty()&&!assignee.matches("[0-9]{1,19}"))throw new BusinessException(400,"负责人无效");
   String due=Objects.toString(m.get("dueDate"),"");try{if(!due.isEmpty())LocalDate.parse(due);}catch(Exception e){throw new BusinessException(400,"截止日期无效");}
   safe.put("assigneeId",assignee);safe.put("dueDate",due);if(!task.isEmpty())safe.put("taskId",task);result.add(safe);
  }
  for(var entry:old.entrySet())if(entry.getValue().get("taskId")!=null&&!seen.contains(entry.getKey()))throw new BusinessException(409,"已转任务的事项需保留关联");
  return result;
 }
 public static String revision(Object content,Object todos){return cn.hutool.crypto.digest.DigestUtil.sha256Hex(Objects.toString(content,"")+"\n"+JSON.toJSONString(todos));}
}
