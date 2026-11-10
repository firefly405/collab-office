package com.collaboffice.search.service;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.search.client.DocumentClient;
import com.collaboffice.search.client.TaskSearchClient;
import com.collaboffice.search.client.UserSearchClient;
import com.collaboffice.search.entity.DocumentIndex;
import com.collaboffice.search.entity.TaskIndex;
import com.collaboffice.search.repository.DocumentIndexRepository;
import com.collaboffice.search.repository.TaskIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final DocumentClient documentClient;
    private final TaskSearchClient taskSearchClient;
    private final UserSearchClient userSearchClient;
    private final DocumentIndexRepository documentIndexRepository;
    private final TaskIndexRepository taskIndexRepository;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Value("${app.internal-token}")
    private String internalToken;

    public List<Map<String, Object>> globalSearch(String keyword, String scope) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        
        List<Map<String,Object>> indexed = Collections.emptyList();
        try {
            indexed = searchWithElasticsearch(keyword, scope);
        } catch (Exception e) {
            log.warn("Elasticsearch query failed, falling back to database search: {}", e.getMessage());
        }
        // Include persisted records even while asynchronous indexing catches up.
        Map<String,Map<String,Object>> merged = new LinkedHashMap<>();
        for (var row : indexed) merged.put(row.get("type")+":"+row.get("id"), row);
        for (var row : searchWithDatabaseFallback(keyword, scope)) merged.put(row.get("type")+":"+row.get("id"), row);
        return visible(new ArrayList<>(merged.values()));
    }

    private List<Map<String, Object>> searchWithElasticsearch(String keyword, String scope) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        // 1. Document search
        if (scope == null || scope.isEmpty() || scope.contains("document") || scope.contains("sheet")) {
            List<DocumentIndex> docs = documentIndexRepository.findByTitleContainingOrContentContaining(keyword, keyword);
            if (docs != null) {
                for (DocumentIndex doc : docs) {
                    if (scope != null && !scope.isEmpty()) {
                        if (scope.contains("document") && !"document".equals(doc.getType())) continue;
                        if (scope.contains("sheet") && !"sheet".equals(doc.getType())) continue;
                    }
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", doc.getId());
                    map.put("title", doc.getTitle());
                    map.put("type", doc.getType());
                    map.put("spaceId", doc.getSpaceId());
                    map.put("creatorId", doc.getCreatorId());
                    results.add(map);
                }
            }
        }
        
        // 2. Task search
        if (scope == null || scope.isEmpty() || scope.contains("task")) {
            List<TaskIndex> tasks = taskIndexRepository.findByTitleContainingOrDescriptionContaining(keyword, keyword);
            if (tasks != null) {
                for (TaskIndex t : tasks) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", t.getId());
                    map.put("title", t.getTitle());
                    map.put("type", "task");
                    map.put("spaceId", t.getSpaceId());
                    map.put("assigneeId", t.getAssigneeId());
                    map.put("status", t.getStatus());
                    results.add(map);
                }
            }
        }
        
        // 3. User search (query via user-service Feign client)
        if (scope == null || scope.isEmpty() || scope.contains("user")) {
            try {
                CommonResult<List<Map<String, Object>>> res = userSearchClient.searchInternal(internalToken, keyword);
                if (res != null && res.getCode() == 200 && res.getData() != null) {
                    results.addAll(res.getData());
                }
            } catch (Exception ex) {
                log.error("Failed to query user-service: {}", ex.getMessage());
            }
        }
        
        return results;
    }

    private List<Map<String, Object>> searchWithDatabaseFallback(String keyword, String scope) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (scope == null || scope.isEmpty() || scope.contains("document") || scope.contains("sheet")) {
            try {
                CommonResult<List<Map<String, Object>>> res = documentClient.searchInternal(internalToken, keyword, scope);
                if (res != null && res.getCode() == 200 && res.getData() != null) {
                    results.addAll(res.getData());
                }
            } catch (Exception e) {
                log.error("Failed to query document-service via Feign fallback: {}", e.getMessage());
            }
        }

        if (scope == null || scope.isEmpty() || scope.contains("task")) {
            try {
                CommonResult<List<Map<String, Object>>> res = taskSearchClient.searchInternal(internalToken, keyword);
                if (res != null && res.getCode() == 200 && res.getData() != null) {
                    results.addAll(res.getData());
                }
            } catch (Exception e) {
                log.error("Failed to query task-service via Feign fallback: {}", e.getMessage());
            }
        }

        if (scope == null || scope.isEmpty() || scope.contains("user")) {
            try {
                CommonResult<List<Map<String, Object>>> res = userSearchClient.searchInternal(internalToken, keyword);
                if (res != null && res.getCode() == 200 && res.getData() != null) {
                    results.addAll(res.getData());
                }
            } catch (Exception e) {
                log.error("Failed to query user-service via Feign fallback: {}", e.getMessage());
            }
        }

        return results;
    }

    public List<String> suggest(String keyword) {
        return globalSearch(keyword,"document").stream().limit(5).map(r -> Objects.toString(r.get("title"),"")).toList();
    }

    private List<Map<String,Object>> visible(List<Map<String,Object>> rows) {
        Long space = access.currentSpace();
        return rows.stream().filter(r -> {
            String id = Objects.toString(r.get("id"), "");
            String type = Objects.toString(r.get("type"), "");
            if ("user".equals(type)) return jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member WHERE space_id=? AND user_id=?",Integer.class,space,id)>0;
            if ("task".equals(type)) {
                if(jdbc.queryForObject("SELECT COUNT(*) FROM task_task t JOIN task_project p ON p.project_id=t.project_id WHERE t.task_id=? AND p.space_id=? AND t.deleted=0 AND p.deleted=0",Integer.class,id,space)==0)return false;
                try{access.requireTask(id,false);return true;}catch(com.collaboffice.common.exception.BusinessException e){return false;}
            }
            boolean inTeam = jdbc.queryForObject("SELECT COUNT(*) FROM doc_document WHERE doc_id=? AND space_id=? AND status=1 AND deleted=0",Integer.class,id,space)>0;
            if (!inTeam) return false;
            try { access.requireDocument(id,false); return true; } catch (com.collaboffice.common.exception.BusinessException e) { return false; }
        }).toList();
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            documentIndexRepository.count();
            status.put("engine", "elasticsearch");
            status.put("status", "UP");
        } catch (Exception e) {
            status.put("engine", "sql");
            status.put("status", "DOWN");
            status.put("reason", e.getMessage());
        }
        return status;
    }
}
