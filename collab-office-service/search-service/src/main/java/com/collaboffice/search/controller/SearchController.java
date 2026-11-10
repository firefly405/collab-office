package com.collaboffice.search.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final com.collaboffice.search.service.SearchQueryService queries;

    @GetMapping("/global")
    public CommonResult<List<Map<String, Object>>> globalSearch(@RequestParam String keyword,
                                                                 @RequestParam(required = false) String scope,
                                                                 @RequestParam(required=false) java.time.LocalDate from,
                                                                 @RequestParam(required=false) java.time.LocalDate to,
                                                                 @RequestParam(required=false) Long creatorId,
                                                                 @RequestParam(required=false) String tag) {
        return CommonResult.success(queries.search(keyword, scope,from,to,creatorId,tag));
    }

    @GetMapping("/suggest")
    public CommonResult<List<String>> suggest(@RequestParam String keyword) {
        return CommonResult.success(queries.search(keyword,"document",null,null,null,null).stream().limit(5).map(r->Objects.toString(r.get("title"),"")).toList());
    }

    @GetMapping("/health")
    public CommonResult<Map<String, Object>> health() {
        return CommonResult.success(searchService.getHealthStatus());
    }
}
