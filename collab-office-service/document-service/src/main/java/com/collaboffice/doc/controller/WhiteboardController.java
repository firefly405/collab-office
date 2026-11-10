package com.collaboffice.doc.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.doc.entity.WhiteboardTemplate;
import com.collaboffice.doc.mapper.WhiteboardTemplateMapper;
import com.collaboffice.doc.service.WhiteboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/whiteboard")
@RequiredArgsConstructor
public class WhiteboardController {

    private final WhiteboardService whiteboardService;
    private final WhiteboardTemplateMapper templateMapper;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/create")
    public CommonResult<String> create(@RequestHeader("X-User-Id") Long userId,
                                       @RequestParam String title,
                                       @RequestParam(defaultValue = "1920") int width,
                                       @RequestParam(defaultValue = "1080") int height,
                                       @RequestParam(required = false) Long spaceId) {
        return CommonResult.success(whiteboardService.createBoard(userId, title, width, height, spaceId));
    }

    @GetMapping("/{boardId}")
    public CommonResult<Map<String, Object>> detail(@PathVariable String boardId) {
        return CommonResult.success(whiteboardService.getBoardData(boardId));
    }

    @PostMapping("/{boardId}/element")
    public CommonResult<Void> addElement(@PathVariable String boardId,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String positionJson,
                                         @RequestParam(required = false) String styleJson,
                                         @RequestParam(required = false) String content,
                                         @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            type = body.get("type") != null ? String.valueOf(body.get("type")) : type;
            positionJson = body.get("positionJson") != null ? String.valueOf(body.get("positionJson")) : positionJson;
            styleJson = body.get("styleJson") != null ? String.valueOf(body.get("styleJson")) : styleJson;
            content = body.get("content") != null ? String.valueOf(body.get("content")) : content;
        }
        whiteboardService.addElement(boardId, type, positionJson, styleJson, content);
        return CommonResult.success(null);
    }

    @DeleteMapping("/element/{elementId}")
    public CommonResult<Void> deleteElement(@PathVariable Long elementId) {
        whiteboardService.deleteElement(elementId);
        return CommonResult.success(null);
    }

    @GetMapping("/{boardId}/elements")
    public CommonResult<List<Map<String, Object>>> getElements(@PathVariable String boardId) {
        return CommonResult.success(whiteboardService.getElements(boardId));
    }

    @DeleteMapping("/{boardId}/clear")
    public CommonResult<Void> clear(@PathVariable String boardId) {
        whiteboardService.clearBoard(boardId);
        return CommonResult.success(null);
    }

    @GetMapping("/templates")
    public CommonResult<List<Map<String, Object>>> listTemplates(@RequestParam(required = false) String category) {
        List<WhiteboardTemplate> templates = templateMapper.selectByCategory(category);
        return CommonResult.success(templates.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("thumbnailUrl", t.getThumbnailUrl());
            m.put("category", t.getCategory());
            m.put("isSystem", t.getIsSystem());
            return m;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/from-template/{templateId}")
    public CommonResult<String> createFromTemplate(@RequestHeader("X-User-Id") Long userId,
                                                    @PathVariable Long templateId,
                                                    @RequestParam String title,
                                                    @RequestParam(required = false) Long spaceId) {
        WhiteboardTemplate template = templateMapper.selectById(templateId);
        if (template == null) return CommonResult.failed(404, "模版不存在");
        String boardId = whiteboardService.createBoard(userId, title, 1920, 1080, spaceId);
        if (template.getTemplateData() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(template.getTemplateData(), Map.class);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> elements = (List<Map<String, Object>>) data.get("elements");
                if (elements != null) {
                    for (Map<String, Object> el : elements) {
                        whiteboardService.addElement(boardId,
                                String.valueOf(el.getOrDefault("type", "rect")),
                                String.valueOf(el.getOrDefault("positionJson", "{}")),
                                String.valueOf(el.getOrDefault("styleJson", "{}")),
                                String.valueOf(el.getOrDefault("content", "")));
                    }
                }
            } catch (Exception e) {
                return CommonResult.failed(500, "模版应用失败: " + e.getMessage());
            }
        }
        return CommonResult.success(boardId);
    }
}
