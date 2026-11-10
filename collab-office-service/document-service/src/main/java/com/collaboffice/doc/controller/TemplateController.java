package com.collaboffice.doc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.entity.TemplateTemplate;
import com.collaboffice.doc.mapper.TemplateTemplateMapper;
import com.collaboffice.doc.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/template")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateTemplateMapper templates;
    private final TemplateService service;
    private final WorkspaceAccess access;

    @GetMapping("/list")
    public CommonResult<Page<TemplateTemplate>> list(@RequestParam(required=false) Integer type,
            @RequestParam(required=false) String category, @RequestParam(required=false) String keyword,
            @RequestParam(defaultValue="1") int pageNum, @RequestParam(defaultValue="50") int pageSize) {
        Long space = access.currentSpace();
        var query = new LambdaQueryWrapper<TemplateTemplate>().eq(TemplateTemplate::getStatus,1)
            .and(q -> q.eq(TemplateTemplate::getSpaceId,space).or().eq(TemplateTemplate::getIsOfficial,1))
            .eq(type!=null,TemplateTemplate::getType,type)
            .eq(category!=null&&!category.isBlank(),TemplateTemplate::getCategory,category)
            .like(keyword!=null&&!keyword.isBlank(),TemplateTemplate::getName,keyword)
            .orderByDesc(TemplateTemplate::getUsageCount);
        return CommonResult.success(templates.selectPage(new Page<>(Math.max(1,pageNum),Math.max(1,Math.min(100,pageSize))),query));
    }
    @GetMapping("/{id}")
    public CommonResult<TemplateTemplate> detail(@PathVariable String id) { return CommonResult.success(service.require(id,false)); }
    @PostMapping("/create")
    public CommonResult<String> create(@RequestBody Map<String,String> body) {
        return CommonResult.success(service.capture(body.get("sourceDocId"),body.get("name"),body.get("category")));
    }
    @PutMapping("/{id}")
    public CommonResult<Void> update(@PathVariable String id,@RequestBody Map<String,String> body) {
        var tpl=service.require(id,true);
        String name=Objects.toString(body.get("name"),"").trim();
        String category=Objects.toString(body.get("category"),"通用");
        if(name.isBlank()||name.length()>100||category.length()>50) throw new BusinessException(400,"模板名称或分类无效");
        tpl.setName(name); tpl.setCategory(category); templates.updateById(tpl);
        return CommonResult.success(null);
    }
    @DeleteMapping("/{id}")
    public CommonResult<Void> delete(@PathVariable String id) {
        templates.deleteById(service.require(id,true).getId()); return CommonResult.success(null);
    }
    @PostMapping("/{id}/use")
    public CommonResult<String> use(@PathVariable String id) { return CommonResult.success(service.use(id)); }
}
