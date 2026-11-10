package com.collaboffice.task.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.task.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/task")
public class ProjectController {
    private final ProjectService projects;
    @GetMapping("/project/{id}")
    public CommonResult<Map<String,Object>> detail(@PathVariable String id) {return CommonResult.success(projects.detail(id));}
    @PutMapping("/project/{id}")
    public CommonResult<Void> update(@PathVariable String id,@RequestBody Map<String,Object> body) {projects.update(id,body);return CommonResult.success(null);}
    @GetMapping("/statistics")
    public CommonResult<Map<String,Object>> statistics(@RequestParam(required=false) String projectId,@RequestParam(required=false) Long assigneeId) {
        return CommonResult.success(projects.statistics(projectId,assigneeId));
    }
}
