package com.collaboffice.task.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.task.mapper.TaskTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/task/internal")
@RequiredArgsConstructor
public class TaskInternalController {

    private final TaskTaskMapper taskMapper;

    @PostMapping("/search")
    public CommonResult<List<Map<String, Object>>> searchInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return CommonResult.success(Collections.emptyList());
        }
        String like = "%" + keyword.trim() + "%";
        var tasks = taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.collaboffice.task.entity.TaskTask>()
                        .like(com.collaboffice.task.entity.TaskTask::getTitle, keyword)
                        .last("LIMIT 10"));
        return CommonResult.success(tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getTaskId());
            m.put("title", t.getTitle());
            m.put("type", "task");
            return m;
        }).collect(Collectors.toList()));
    }
}
