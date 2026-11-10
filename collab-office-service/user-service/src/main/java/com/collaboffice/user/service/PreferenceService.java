package com.collaboffice.user.service;
import com.alibaba.fastjson2.*;
import com.collaboffice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class PreferenceService {
    private final JdbcTemplate jdbc;
    public Map<String,Object> get(Long userId){var values=jdbc.queryForList("SELECT preference_json FROM user_setting WHERE user_id=?",String.class,userId);return values.isEmpty()||values.get(0)==null?defaults():merged(JSON.parseObject(values.get(0)));}
    public void save(Long userId,Map<String,Object> input){var values=merged(input);validate(values);jdbc.update("UPDATE user_setting SET preference_json=? WHERE user_id=?",JSON.toJSONString(values),userId);}
    public static Map<String,Object> defaults(){return new LinkedHashMap<>(Map.of("theme","system","editorFont","Arial","editorSize",15,"editorLineHeight",1.8,"editorWidth","comfortable","defaultView","/dashboard","searchShortcut","k","notifications",new LinkedHashMap<>(Map.of("system",true,"collab",true,"task",true,"meeting",true))));}
    public static Map<String,Object> merged(Map<String,Object> input){var result=defaults();for(var key:result.keySet())if(input.containsKey(key))result.put(key,input.get(key));return result;}
    public static void validate(Map<String,Object> input){
        for(var entry:Map.of("theme",Set.of("light","dark","system"),"editorFont",Set.of("Arial","Microsoft YaHei","SimSun","monospace"),"editorWidth",Set.of("comfortable","wide"),"defaultView",Set.of("/dashboard","/documents","/tasks","/im"),"searchShortcut",Set.of("k","j","p")).entrySet())if(!entry.getValue().contains(input.get(entry.getKey())))throw new BusinessException(400,"偏好选项无效: "+entry.getKey());
        if(!(input.get("editorSize") instanceof Number size)||size.intValue()<12||size.intValue()>24||!(input.get("editorLineHeight") instanceof Number height)||!Double.isFinite(height.doubleValue())||height.doubleValue()<1.2||height.doubleValue()>2.5)throw new BusinessException(400,"编辑器字号或行距无效");
        if(!(input.get("notifications") instanceof Map<?,?> categories)||!categories.keySet().equals(Set.of("system","collab","task","meeting"))||categories.values().stream().anyMatch(v->!(v instanceof Boolean)))throw new BusinessException(400,"通知分类设置无效");
    }
}
