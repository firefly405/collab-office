package com.collaboffice.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.file.entity.KnowledgeItem;
import com.collaboffice.file.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @GetMapping("/list")
    public CommonResult<Page<KnowledgeItem>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return CommonResult.success(knowledgeService.getKnowledgeList(pageNum, pageSize));
    }

    @GetMapping("/{itemId}")
    public CommonResult<KnowledgeItem> detail(@PathVariable String itemId) {
        return CommonResult.success(knowledgeService.getKnowledgeDetail(itemId));
    }

    @PostMapping("/create")
    public CommonResult<String> create(@RequestBody KnowledgeItem item,
                                        @RequestHeader("X-User-Id") Long userId) {
        return CommonResult.success(knowledgeService.createKnowledge(item, userId));
    }

    @PutMapping("/{itemId}")
    public CommonResult<Void> update(@PathVariable String itemId, @RequestBody KnowledgeItem item) {
        knowledgeService.updateKnowledge(itemId, item);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{itemId}")
    public CommonResult<Void> delete(@PathVariable String itemId) {
        knowledgeService.deleteKnowledge(itemId);
        return CommonResult.success(null);
    }
}
