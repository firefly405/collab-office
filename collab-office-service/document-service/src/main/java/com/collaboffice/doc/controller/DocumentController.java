package com.collaboffice.doc.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.DocDocument;
import com.collaboffice.doc.entity.DocMember;
import com.collaboffice.doc.entity.DocShare;
import com.collaboffice.doc.mapper.DocMemberMapper;
import com.collaboffice.doc.mapper.DocShareMapper;
import com.collaboffice.doc.service.*;
import com.collaboffice.doc.service.impl.ExportServiceImpl;
import com.collaboffice.doc.vo.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.collaboffice.doc.mapper.DocDocumentMapper;
import com.collaboffice.doc.mapper.SheetSpreadsheetMapper;
import com.collaboffice.doc.entity.DocDocument;
import com.collaboffice.doc.entity.SheetSpreadsheet;

@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final TemplateService templates;
    private final DocumentContentService contentService;
    private final CommentService commentService;
    private final DocShareMapper docShareMapper;
    private final DocMemberMapper docMemberMapper;
    private final ExportServiceImpl exportService;
    private final DocDocumentMapper docDocumentMapper;
    private final SheetSpreadsheetMapper sheetSpreadsheetMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final com.collaboffice.doc.mapper.DocContentMapper contents;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final DocumentHistoryService history;

    @Value("${app.internal-token}")
    private String internalToken;

    @PostMapping("/create")
    public CommonResult<String> create(@RequestHeader("X-User-Id") Long userId,
                                       @RequestHeader(value = "X-User-Name", defaultValue = "") String userName,
                                       @Valid @RequestBody DocumentCreateRequest request) {
        access.requireSpace(request.getSpaceId(), true);
        userName = jdbc.queryForObject("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,userId);
        return CommonResult.success(documentService.create(userId, userName, request));
    }

    @PutMapping("/{docId}")
    public CommonResult<Void> update(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId,
                                     @Valid @RequestBody DocumentUpdateRequest request) {
        request.setDocId(docId);
        documentService.update(userId, request);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{docId}")
    public CommonResult<Void> delete(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId) {
        documentService.delete(userId, docId);
        return CommonResult.success(null);
    }

    @PostMapping("/{docId}/recover")
    public CommonResult<Void> recover(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId) {
        documentService.recover(userId, docId);
        return CommonResult.success(null);
    }

    @GetMapping("/list")
    public CommonResult<Page<DocumentDetailVO>> getList(@Valid DocumentListRequest request) {
        access.requireSpace(request.getSpaceId(), false);
        request.setUserId(access.userId());
        return CommonResult.success(documentService.getList(request));
    }

    @GetMapping("/{docId}")
    public CommonResult<DocumentDetailVO> getDetail(@PathVariable String docId) {
        DocumentDetailVO detail = documentService.getDetail(docId);
        try { access.requireDocument(docId, true); detail.setCanEdit(true); }
        catch (com.collaboffice.common.exception.BusinessException e) { detail.setCanEdit(false); }
        try { access.requireComment(docId); detail.setCanComment(true); }
        catch (com.collaboffice.common.exception.BusinessException e) { detail.setCanComment(false); }
        return CommonResult.success(detail);
    }

    @PostMapping("/{docId}/duplicate")
    public CommonResult<String> duplicate(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId) {
        return CommonResult.success(templates.copyDocument(docId));
    }

    @PutMapping("/{docId}/move")
    public CommonResult<Void> move(@PathVariable String docId, @RequestParam(required=false) Long folderId) {
        documentService.move(docId, folderId);
        return CommonResult.success(null);
    }

    @PostMapping("/{docId}/favorite")
    public CommonResult<Void> favorite(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId,
                                       @RequestParam Boolean fav) {
        documentService.favorite(userId, docId, fav);
        return CommonResult.success(null);
    }

    @GetMapping("/{docId}/content")
    public CommonResult<DocumentContentVO> getContent(@PathVariable String docId) {
        return CommonResult.success(contentService.getContent(docId));
    }

    @PutMapping("/{docId}/content")
    public CommonResult<Void> saveContent(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId,
                                          @RequestBody String content) {
        contentService.saveContent(docId, content, userId);
        return CommonResult.success(null);
    }

    @GetMapping("/{docId}/versions")
    public CommonResult<List<VersionVO>> getVersions(@PathVariable String docId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return CommonResult.success(history.list(docId,page,size));
    }

    @GetMapping("/{docId}/versions/compare")
    public CommonResult<VersionDiffVO> compareVersions(@PathVariable String docId,
                                                       @RequestParam Long v1, @RequestParam Long v2) {
        return CommonResult.success(documentService.compareVersions(docId, v1, v2));
    }

    @PostMapping("/{docId}/versions/rollback")
    public CommonResult<Void> rollback(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId,
                                       @RequestParam Long targetVersion) {
        return CommonResult.failed(400, "请在编辑器版本记录中恢复内容，以保持协同编辑一致");
    }

    @GetMapping("/{docId}/versions/{version}/content")
    public CommonResult<String> versionContent(@PathVariable String docId, @PathVariable Long version) {
        var content = contents.selectByDocIdAndVersion(docId, version);
        if (content == null) return CommonResult.failed(ResultCode.NOT_FOUND);
        return CommonResult.success(content.getContent());
    }

    @PostMapping("/{docId}/versions/snapshot")
    public CommonResult<Void> createSnapshot(@RequestHeader("X-User-Id") Long userId, @PathVariable String docId,
                                             @RequestParam String name, @RequestParam(defaultValue="") String remark) {
        history.snapshot(docId,userId,name,remark);
        return CommonResult.success(null);
    }

    @PutMapping("/{docId}/versions/{version}/metadata")
    public CommonResult<Void> nameVersion(@PathVariable String docId,@PathVariable Long version,@RequestBody Map<String,String> body) {
        history.name(docId,version,body.get("name"),body.get("remark"));return CommonResult.success(null);
    }

    @GetMapping("/{docId}/versions/policy")
    public CommonResult<Map<String,Object>> versionPolicy(@PathVariable String docId) {return CommonResult.success(history.policy(docId));}

    @PutMapping("/{docId}/versions/policy")
    public CommonResult<Void> versionPolicy(@PathVariable String docId,@RequestBody Map<String,Integer> body) {history.policy(docId,body.get("limit"));return CommonResult.success(null);}

    @GetMapping("/{docId}/comment-members")
    public CommonResult<List<Map<String,Object>>> commentMembers(@PathVariable String docId) {
        return CommonResult.success(jdbc.queryForList("SELECT u.id AS userId,COALESCE(u.real_name,u.username) AS userName FROM sys_user u JOIN sys_space_member m ON m.user_id=u.id JOIN doc_document d ON d.space_id=m.space_id WHERE d.doc_id=? AND d.deleted=0",docId));
    }

    @PostMapping("/{docId}/comments")
    public CommonResult<CommentVO> createComment(@RequestHeader("X-User-Id") Long userId,
                                                 @RequestHeader(value = "X-User-Name", defaultValue = "") String userName,
                                                 @PathVariable String docId,
                                                 @Valid @RequestBody CommentCreateRequest request) {
        request.setDocId(docId);
        userName = jdbc.queryForObject("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,userId);
        return CommonResult.success(commentService.create(request, userId, userName));
    }

    @GetMapping("/{docId}/comments")
    public CommonResult<List<CommentVO>> getComments(@PathVariable String docId,
                                                     @RequestParam(defaultValue = "all") String filter) {
        return CommonResult.success(commentService.listByDocId(docId, filter));
    }

    @PostMapping("/{docId}/comments/{commentId}/reply")
    public CommonResult<CommentVO> replyComment(@RequestHeader("X-User-Id") Long userId,
                                                @RequestHeader(value = "X-User-Name", defaultValue = "") String userName,
                                                @PathVariable String docId, @PathVariable Long commentId,
                                                @RequestBody String content) {
        return CommonResult.success(commentService.reply(commentId, content, userId, userName));
    }

    @PutMapping("/{docId}/comments/{commentId}/resolve")
    public CommonResult<Void> resolveComment(@PathVariable String docId, @PathVariable Long commentId) {
        commentService.resolve(commentId);
        return CommonResult.success(null);
    }

    @DeleteMapping("/{docId}/comments/{commentId}")
    public CommonResult<Void> deleteComment(@PathVariable String docId, @PathVariable Long commentId) {
        commentService.delete(commentId);
        return CommonResult.success(null);
    }

    @PostMapping("/{docId}/share")
    public CommonResult<ShareVO> createShare(@RequestHeader("X-User-Id") Long userId,
                                             @PathVariable String docId,
                                             @Valid @RequestBody DocumentShareRequest request) {
        request.setDocId(docId);
        documentService.getDetail(docId);
        DocShare share = new DocShare();
        share.setDocId(docId);
        share.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        share.setSharePassword(request.getPassword() == null || request.getPassword().isEmpty() ? null
                : cn.hutool.crypto.digest.BCrypt.hashpw(request.getPassword()));
        share.setPermission(1);
        share.setExpireTime(request.getExpireDays() != null ? LocalDateTime.now().plusDays(request.getExpireDays()) : null);
        share.setViewCount(0);
        share.setCreatorId(userId);
        share.setStatus(1);
        docShareMapper.insert(share);
        return CommonResult.success(ShareVO.builder()
                .shareId(share.getId())
                .shareToken(share.getShareToken())
                .shareUrl("/#/share/" + share.getShareToken())
                .permission(share.getPermission())
                .expireTime(share.getExpireTime())
                .hasPassword(share.getSharePassword() != null && !share.getSharePassword().isEmpty())
                .build());
    }

    @GetMapping("/share/{shareToken}")
    public CommonResult<DocumentDetailVO> getShare(@PathVariable String shareToken,
                                                    @RequestParam(required = false) String password) {
        DocShare share = docShareMapper.selectByShareToken(shareToken);
        if (!isActiveShare(share)) {
            return CommonResult.failed(ResultCode.NOT_FOUND);
        }
        if (share.getSharePassword() != null && !share.getSharePassword().isEmpty()
                && !matchesSharePassword(share.getSharePassword(), password)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "分享密码错误");
        }
        share.setViewCount((share.getViewCount() == null ? 0 : share.getViewCount()) + 1);
        docShareMapper.updateById(share);
        return CommonResult.success(documentService.getDetail(share.getDocId()));
    }

    @GetMapping("/{docId}/shares")
    public CommonResult<List<Map<String,Object>>> shares(@PathVariable String docId) {
        access.requireDocument(docId,true);
        return CommonResult.success(jdbc.queryForList("SELECT id,share_token AS shareToken,permission,expire_time AS expireTime,view_count AS viewCount,status,create_time AS createTime,(share_password IS NOT NULL AND share_password<>'') AS hasPassword FROM doc_share WHERE doc_id=? ORDER BY create_time DESC",docId));
    }

    @DeleteMapping("/{docId}/shares/{shareId}")
    public CommonResult<Void> revokeShare(@PathVariable String docId,@PathVariable Long shareId) {
        access.requireDocument(docId,true);
        jdbc.update("UPDATE doc_share SET status=0 WHERE id=? AND doc_id=?",shareId,docId);
        return CommonResult.success(null);
    }

    @GetMapping("/share/{shareToken}/content")
    public CommonResult<DocumentContentVO> getShareContent(@PathVariable String shareToken,
                                                            @RequestParam(required = false) String password) {
        DocShare share = docShareMapper.selectByShareToken(shareToken);
        if (!isActiveShare(share)) {
            return CommonResult.failed(ResultCode.NOT_FOUND);
        }
        if (share.getSharePassword() != null && !share.getSharePassword().isEmpty()
                && !matchesSharePassword(share.getSharePassword(), password)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "分享密码错误");
        }
        return CommonResult.success(contentService.getContent(share.getDocId()));
    }

    private boolean matchesSharePassword(String stored, String input) {
        if (input == null || input.length() > 20) return false;
        if (stored.startsWith("$2")) return cn.hutool.crypto.digest.BCrypt.checkpw(input, stored);
        // Accept links created before passwords were hashed.
        return java.security.MessageDigest.isEqual(stored.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private boolean isActiveShare(DocShare share) {
        if (share == null || !Integer.valueOf(1).equals(share.getStatus())
                || share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) return false;
        return docDocumentMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                .eq(DocDocument::getDocId, share.getDocId()).eq(DocDocument::getStatus, 1)) > 0
                && jdbc.queryForObject("SELECT COUNT(*) FROM doc_document d JOIN sys_space s ON s.id=d.space_id WHERE d.doc_id=? AND s.status=1 AND s.deleted=0",Integer.class,share.getDocId())>0;
    }

    @PostMapping("/{docId}/export")
    public CommonResult<ExportTaskVO> export(@RequestHeader("X-User-Id") Long userId,
                                             @PathVariable String docId,
                                             @Valid @RequestBody DocumentExportRequest request) {
        request.setDocId(docId);
        return CommonResult.success(exportService.exportDocument(docId, request.getFormat(), userId,Boolean.TRUE.equals(request.getIncludeComments()),Boolean.TRUE.equals(request.getIncludeVersions())));
    }

    @GetMapping("/export/tasks/{taskId}")
    public CommonResult<ExportTaskVO> getExportTask(@PathVariable String taskId) {
        exportService.requireTaskOwner(taskId, access.userId());
        return CommonResult.success(exportService.getTaskStatus(taskId));
    }

    @PostMapping("/internal/search")
    public CommonResult<List<Map<String, Object>>> searchInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String keyword,
            @RequestParam(required = false) String scope) {
        if (!internalToken.equals(token)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "未授权的内部访问");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        String like = "%" + keyword.trim() + "%";

        if (scope == null || scope.isEmpty() || scope.contains("document")) {
            List<DocDocument> docs = docDocumentMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                            .like(DocDocument::getTitle, keyword)
                            .eq(DocDocument::getDeleted, 0)
                            .eq(DocDocument::getStatus, 1)
                            .last("LIMIT 20")
            );
            for (DocDocument d : docs) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", d.getDocId());
                map.put("title", d.getTitle());
                map.put("type", "document");
                results.add(map);
            }
        }

        if (scope == null || scope.isEmpty() || scope.contains("sheet")) {
            List<SheetSpreadsheet> sheets = sheetSpreadsheetMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SheetSpreadsheet>()
                            .like(SheetSpreadsheet::getTitle, keyword)
                            .eq(SheetSpreadsheet::getDeleted, 0)
                            .eq(SheetSpreadsheet::getStatus, 1)
                            .last("LIMIT 10")
            );
            for (SheetSpreadsheet s : sheets) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", s.getSheetId());
                map.put("title", s.getTitle());
                map.put("type", "spreadsheet");
                results.add(map);
            }
        }

        return CommonResult.success(results);
    }

    @PostMapping("/internal/suggest")
    public CommonResult<List<String>> suggestInternal(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String keyword) {
        if (!internalToken.equals(token)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "未授权的内部访问");
        }

        List<DocDocument> docs = docDocumentMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                        .like(DocDocument::getTitle, keyword)
                        .eq(DocDocument::getDeleted, 0)
                        .eq(DocDocument::getStatus, 1)
                        .orderByDesc(DocDocument::getLastEditTime)
                        .last("LIMIT 5")
        );
        List<String> suggestions = docs.stream().map(DocDocument::getTitle).toList();
        return CommonResult.success(suggestions);
    }

    /**
     * 内部接口: 校验用户是否有权访问文档(供 ws-service 在 DOC_JOIN 时调用)
     * 通过条件: 文档创建者 / 文档成员 / 公开文档
     */
    @GetMapping("/internal/permission/check")
    public CommonResult<Boolean> checkDocPermission(
            @RequestHeader("X-Internal-Token") String token,
            @RequestParam String docId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean write) {
        if (!internalToken.equals(token)) {
            return CommonResult.failed(ResultCode.UNAUTHORIZED.getCode(), "未授权的内部访问");
        }
        try { access.requireDocument(docId, userId, write); return CommonResult.success(true); }
        catch (com.collaboffice.common.exception.BusinessException e) { return CommonResult.success(false); }
    }
}
