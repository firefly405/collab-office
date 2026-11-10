package com.collaboffice.doc.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.DocumentService;
import com.collaboffice.doc.vo.DocumentDetailVO;
import com.collaboffice.doc.vo.VersionDiffVO;
import com.collaboffice.doc.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocDocumentMapper docDocumentMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;
    private final DocContentMapper docContentMapper;
    private final DocVersionMapper docVersionMapper;
    private final DocMemberMapper docMemberMapper;
    private final SheetSpreadsheetMapper sheetSpreadsheetMapper;
    private final SheetSheetMapper sheetSheetMapper;
    private final MindmapMapMapper mindmapMapMapper;
    private final MindmapNodeMapper mindmapNodeMapper;
    private final WhiteboardBoardMapper whiteboardBoardMapper;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    private static final int PERMISSION_MANAGE = 3;

    @Override
    @Transactional
    public String create(Long userId, String userName, DocumentCreateRequest request) {
        String docId = SnowflakeIdGenerator.nextIdStr();

        DocDocument doc = new DocDocument();
        doc.setDocId(docId);
        doc.setTitle(request.getTitle());
        doc.setType(request.getType() != null ? request.getType() : 1);
        doc.setSpaceId(request.getSpaceId());
        doc.setFolderId(request.getFolderId());
        doc.setCreatorId(userId);
        doc.setCreatorName(userName);
        doc.setCurrentVersion(1L);
        doc.setStatus(1);
        doc.setLastEditTime(LocalDateTime.now());
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());
        docDocumentMapper.insert(doc);

        DocContent content = new DocContent();
        content.setDocId(docId);
        content.setVersion(1L);
        content.setContent("{}");
        content.setIsSnapshot(0);
        content.setEditorId(userId);
        content.setEditorName(userName);
        content.setEditTime(LocalDateTime.now());
        docContentMapper.insert(content);

        DocMember member = new DocMember();
        member.setDocId(docId);
        member.setUserId(userId);
        member.setUserName(userName);
        member.setPermission(PERMISSION_MANAGE);
        member.setIsOnline(0);
        member.setJoinTime(LocalDateTime.now());
        docMemberMapper.insert(member);

        createTypedResource(docId, userId, request);

        // Publish search indexing event
        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("id", docId);
            event.put("title", request.getTitle());
            event.put("content", "");
            event.put("type", request.getType() != null && request.getType() == 2 ? "sheet" : "document");
            event.put("spaceId", request.getSpaceId());
            event.put("creatorId", userId);
            event.put("action", "UPDATE");
            rabbitTemplate.convertAndSend("search-index-exchange", "search.document.update", event);
        } catch (Exception e) {
            log.error("Failed to publish document create index event", e);
        }

        return docId;
    }

    private void createTypedResource(String docId, Long userId, DocumentCreateRequest request) {
        Integer type = request.getType() != null ? request.getType() : 1;
        if (type == 2) {
            SheetSpreadsheet spreadsheet = new SheetSpreadsheet();
            spreadsheet.setSheetId(docId);
            spreadsheet.setTitle(request.getTitle());
            spreadsheet.setSpaceId(request.getSpaceId());
            spreadsheet.setFolderId(request.getFolderId());
            spreadsheet.setCreatorId(userId);
            spreadsheet.setSheetCount(1);
            spreadsheet.setRowCount(1000);
            spreadsheet.setColCount(26);
            spreadsheet.setLastEditTime(LocalDateTime.now());
            spreadsheet.setStatus(1);
            sheetSpreadsheetMapper.insert(spreadsheet);

            SheetSheet sheet = new SheetSheet();
            sheet.setSpreadsheetId(spreadsheet.getSheetId());
            sheet.setSheetName("Sheet1");
            sheet.setSheetIndex(0);
            sheet.setRowCount(1000);
            sheet.setColCount(26);
            sheet.setSortOrder(0);
            sheetSheetMapper.insert(sheet);
        } else if (type == 3) {
            MindmapMap map = new MindmapMap();
            map.setMapId(docId);
            map.setTitle(request.getTitle());
            map.setLayoutType("mindmap");
            map.setSpaceId(request.getSpaceId());
            map.setCreatorId(userId);
            map.setLastEditTime(LocalDateTime.now());
            map.setStatus(1);
            mindmapMapMapper.insert(map);

            MindmapNode rootNode = new MindmapNode();
            rootNode.setMapId(docId);
            rootNode.setNodeKey(UUID.randomUUID().toString().substring(0, 8));
            rootNode.setContent(request.getTitle());
            rootNode.setSortOrder(0);
            mindmapNodeMapper.insert(rootNode);
        } else if (type == 4) {
            WhiteboardBoard board = new WhiteboardBoard();
            board.setBoardId(docId);
            board.setTitle(request.getTitle());
            board.setWidth(1920);
            board.setHeight(1080);
            board.setBackgroundColor("#FFFFFF");
            board.setSpaceId(request.getSpaceId());
            board.setCreatorId(userId);
            board.setLastEditTime(LocalDateTime.now());
            board.setStatus(1);
            whiteboardBoardMapper.insert(board);
        }
    }

    @Override
    @Transactional
    public void update(Long userId, DocumentUpdateRequest request) {
        checkPermission(userId, request.getDocId(), 2);
        DocDocument doc = new DocDocument();
        doc.setDocId(request.getDocId());
        if (request.getTitle() != null) doc.setTitle(request.getTitle());
        if (request.getTags() != null) doc.setTags(request.getTags());
        if (request.getFolderId() != null) doc.setFolderId(request.getFolderId());
        docDocumentMapper.update(doc, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                .eq(DocDocument::getDocId, request.getDocId()));

        DocDocument updatedDoc = docDocumentMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                .eq(DocDocument::getDocId, request.getDocId()));
        if (updatedDoc != null) {
            try {
                DocContent latest = docContentMapper.selectLatestByDocId(request.getDocId());
                String txtContent = latest != null ? latest.getContent() : "";

                java.util.Map<String, Object> event = new java.util.HashMap<>();
                event.put("id", updatedDoc.getDocId());
                event.put("title", updatedDoc.getTitle());
                event.put("content", txtContent);
                event.put("type", updatedDoc.getType() == 2 ? "sheet" : "document");
                event.put("spaceId", updatedDoc.getSpaceId());
                event.put("creatorId", updatedDoc.getCreatorId());
                event.put("action", "UPDATE");
                rabbitTemplate.convertAndSend("search-index-exchange", "search.document.update", event);
            } catch (Exception e) {
                log.error("Failed to publish document update index event", e);
            }
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, String docId) {
        checkPermission(userId, docId, PERMISSION_MANAGE);
        DocDocument doc = new DocDocument();
        doc.setDocId(docId);
        doc.setStatus(0);
        docDocumentMapper.update(doc, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                .eq(DocDocument::getDocId, docId));

        try {
            java.util.Map<String, Object> event = new java.util.HashMap<>();
            event.put("id", docId);
            event.put("action", "DELETE");
            rabbitTemplate.convertAndSend("search-index-exchange", "search.document.delete", event);
        } catch (Exception e) {
            log.error("Failed to publish document delete index event", e);
        }
    }

    @Override
    @Transactional
    public void recover(Long userId, String docId) {
        checkPermission(userId, docId, PERMISSION_MANAGE);
        DocDocument doc = new DocDocument();
        doc.setDocId(docId);
        doc.setStatus(1);
        docDocumentMapper.update(doc, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                .eq(DocDocument::getDocId, docId));

        DocDocument recoveredDoc = docDocumentMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                .eq(DocDocument::getDocId, docId));
        if (recoveredDoc != null) {
            try {
                DocContent latest = docContentMapper.selectLatestByDocId(docId);
                String txtContent = latest != null ? latest.getContent() : "";

                java.util.Map<String, Object> event = new java.util.HashMap<>();
                event.put("id", recoveredDoc.getDocId());
                event.put("title", recoveredDoc.getTitle());
                event.put("content", txtContent);
                event.put("type", recoveredDoc.getType() == 2 ? "sheet" : "document");
                event.put("spaceId", recoveredDoc.getSpaceId());
                event.put("creatorId", recoveredDoc.getCreatorId());
                event.put("action", "UPDATE");
                rabbitTemplate.convertAndSend("search-index-exchange", "search.document.update", event);
            } catch (Exception e) {
                log.error("Failed to publish document recover index event", e);
            }
        }
    }

    @Override
    public Page<DocumentDetailVO> getList(DocumentListRequest request) {
        Page<DocumentDetailVO> page = new Page<>(request.getPageNum(), request.getPageSize());
        var candidates=docDocumentMapper.selectListWithFilters(new Page<DocumentDetailVO>(1,-1),request).getRecords();
        var visible=candidates.stream().filter(d->{try{access.requireDocument(d.getDocId(),false);return true;}catch(com.collaboffice.common.exception.BusinessException e){return false;}}).toList();
        int from=(int)Math.min(visible.size(),(page.getCurrent()-1)*page.getSize()),to=(int)Math.min(visible.size(),from+page.getSize());page.setTotal(visible.size());page.setRecords(visible.subList(from,to));return page;
    }

    @Override
    public DocumentDetailVO getDetail(String docId) {
        DocDocument doc = findByDocId(docId);
        List<DocMember> members = docMemberMapper.selectByDocId(docId);
        long onlineCount = members.stream().filter(m -> m.getIsOnline() == 1).count();

        DocumentDetailVO vo = new DocumentDetailVO();
        vo.setDocId(doc.getDocId());
        vo.setTitle(doc.getTitle());
        vo.setType(doc.getType());
        vo.setCreatorName(doc.getCreatorName());
        vo.setLastEditorName(doc.getLastEditorName());
        vo.setCurrentVersion(doc.getCurrentVersion());
        vo.setWordCount(doc.getWordCount());
        vo.setEditCount(doc.getEditCount());
        vo.setViewCount(doc.getViewCount());
        vo.setTags(doc.getTags());
        vo.setIsFavorite(doc.getIsFavorite());
        vo.setCreateTime(doc.getCreateTime());
        vo.setLastEditTime(doc.getLastEditTime());
        vo.setMemberCount(members.size());
        vo.setOnlineMemberCount((int) onlineCount);
        return vo;
    }

    @Override
    @Transactional
    public String duplicate(Long userId, String docId) {
        DocDocument origin = findByDocId(docId);
        String newDocId = SnowflakeIdGenerator.nextIdStr();
        DocDocument copy = new DocDocument();
        copy.setDocId(newDocId);
        copy.setTitle(origin.getTitle() + "(副本)");
        copy.setType(origin.getType());
        copy.setSpaceId(origin.getSpaceId());
        copy.setCreatorId(userId);
        copy.setCreatorName(origin.getCreatorName());
        copy.setCurrentVersion(1L);
        copy.setStatus(1);
        copy.setLastEditTime(LocalDateTime.now());
        docDocumentMapper.insert(copy);

        DocContent latestContent = docContentMapper.selectLatestByDocId(docId);
        if (latestContent != null) {
            DocContent newContent = new DocContent();
            newContent.setDocId(newDocId);
            newContent.setVersion(1L);
            newContent.setContent(latestContent.getContent());
            newContent.setEditorId(userId);
            newContent.setEditTime(LocalDateTime.now());
            newContent.setIsSnapshot(0);
            docContentMapper.insert(newContent);
        }
        return newDocId;
    }

    @Override
    public void move(String docId, Long folderId) {
        access.requireDocument(docId,true);
        var original=findByDocId(docId);
        access.requireFolder(folderId,original.getSpaceId());
        jdbcTemplate.update("UPDATE doc_document SET folder_id=? WHERE doc_id=?",folderId,docId);
        /* legacy mapper ignores null folder ids; explicit update supports moving to the root. */
    }

    @Override
    public void favorite(Long userId, String docId, Boolean fav) {
        if (Boolean.TRUE.equals(fav)) jdbcTemplate.update("INSERT IGNORE INTO doc_user_favorite(user_id,doc_id) VALUES (?,?)", userId, docId);
        else jdbcTemplate.update("DELETE FROM doc_user_favorite WHERE user_id=? AND doc_id=?", userId, docId);
    }

    @Override
    public List<VersionVO> listVersions(String docId, int pageNum, int size) {
        List<DocContent> versions = docContentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocContent>().eq(DocContent::getDocId, docId).orderByDesc(DocContent::getVersion).last("LIMIT " + Math.max(1, Math.min(size, 100))));
        return versions.stream().map(v -> {
            VersionVO vo = new VersionVO();
            vo.setDocId(v.getDocId());
            vo.setVersion(v.getVersion());
            vo.setVersionName("v" + v.getVersion());
            vo.setEditorName(v.getEditorName());
            vo.setChangeSummary(v.getRemark());
            vo.setCreatedTime(v.getEditTime());
            vo.setIsSnapshot(v.getIsSnapshot());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public VersionDiffVO compareVersions(String docId, Long v1, Long v2) {
        DocContent c1 = docContentMapper.selectByDocIdAndVersion(docId, v1);
        DocContent c2 = docContentMapper.selectByDocIdAndVersion(docId, v2);
        if(c1==null||c2==null)throw new BusinessException(ResultCode.NOT_FOUND);
        VersionDiffVO vo = new VersionDiffVO();
        vo.setVersionFrom(v1);
        vo.setVersionTo(v2);
        String t1 = c1 != null && c1.getContent() != null ? c1.getContent() : "";
        String t2 = c2 != null && c2.getContent() != null ? c2.getContent() : "";
        vo.setDiffSegments(computeDiff(t1, t2));
        return vo;
    }

    private List<VersionDiffVO.DiffSegment> computeDiff(String oldText, String newText) {
        oldText=plainDiffText(oldText);newText=plainDiffText(newText);
        List<VersionDiffVO.DiffSegment> segments = new ArrayList<>();
        if (oldText.equals(newText)) return segments;

        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);
        if((long)oldLines.length*newLines.length>1_000_000) {
            var removed=new VersionDiffVO.DiffSegment();removed.setType("delete");removed.setText(oldText);
            var added=new VersionDiffVO.DiffSegment();added.setType("insert");added.setText(newText);
            return List.of(removed,added);
        }
        int[][] lcs = new int[oldLines.length + 1][newLines.length + 1];

        for (int i = 1; i <= oldLines.length; i++) {
            for (int j = 1; j <= newLines.length; j++) {
                if (oldLines[i - 1].equals(newLines[j - 1])) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }

        List<String> result = new ArrayList<>();
        int i = oldLines.length, j = newLines.length;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1].equals(newLines[j - 1])) {
                result.add(0, "= " + oldLines[i - 1]);
                i--; j--;
            } else if (j > 0 && (i == 0 || lcs[i][j - 1] >= lcs[i - 1][j])) {
                result.add(0, "+ " + newLines[j - 1]);
                j--;
            } else {
                result.add(0, "- " + oldLines[i - 1]);
                i--;
            }
        }

        for (String line : result) {
            VersionDiffVO.DiffSegment seg = new VersionDiffVO.DiffSegment();
            if (line.startsWith("+ ")) {
                seg.setType("insert");
                seg.setText(line.substring(2));
            } else if (line.startsWith("- ")) {
                seg.setType("delete");
                seg.setText(line.substring(2));
            } else {
                seg.setType("equal");
                seg.setText(line.substring(2));
            }
            segments.add(seg);
        }
        return segments;
    }

    private String plainDiffText(String content) {
        try {
            var value=com.alibaba.fastjson2.JSON.parseObject(com.collaboffice.doc.service.DocumentExportContent.normalize(content));
            var blocks=value.getJSONArray("blocks");
            if(blocks!=null)return blocks.stream().map(b->((com.alibaba.fastjson2.JSONObject)b).getString("text")).filter(java.util.Objects::nonNull).collect(Collectors.joining("\n"));
        } catch(Exception ignored) { }
        return content;
    }

    @Override
    @Transactional
    public void rollback(Long userId, String docId, Long targetVersion) {
        checkPermission(userId, docId, 2);
        DocContent targetContent = docContentMapper.selectByDocIdAndVersion(docId, targetVersion);
        if (targetContent == null) throw new BusinessException(ResultCode.NOT_FOUND);

        DocDocument doc = findByDocId(docId);
        long newVersion = doc.getCurrentVersion() + 1;
        DocContent rollbackContent = new DocContent();
        rollbackContent.setDocId(docId);
        rollbackContent.setVersion(newVersion);
        rollbackContent.setContent(targetContent.getContent());
        rollbackContent.setEditorId(userId);
        rollbackContent.setEditTime(LocalDateTime.now());
        rollbackContent.setIsSnapshot(0);
        rollbackContent.setRemark("回滚至版本" + targetVersion);
        docContentMapper.insert(rollbackContent);

        doc.setCurrentVersion(newVersion);
        doc.setLastEditTime(LocalDateTime.now());
        docDocumentMapper.updateById(doc);
        redis.delete("cache:doc:content:" + docId);
    }

    @Override
    public void createSnapshot(Long userId, String docId, String name) {
        DocContent latestContent = docContentMapper.selectLatestByDocId(docId);
        if (latestContent == null) return;

        DocVersion snapshot = new DocVersion();
        snapshot.setDocId(docId);
        snapshot.setVersion(latestContent.getVersion());
        snapshot.setVersionName(name);
        snapshot.setSnapshotData(latestContent.getContent());
        snapshot.setEditorId(userId);
        snapshot.setIsSnapshot(1);
        docVersionMapper.insert(snapshot);
    }

    private DocDocument findByDocId(String docId) {
        DocDocument doc = docDocumentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DocDocument>()
                        .eq(DocDocument::getDocId, docId));
        if (doc == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return doc;
    }

    private void checkPermission(Long userId, String docId, int requiredPermission) {
        if (requiredPermission < PERMISSION_MANAGE) { access.requireDocument(docId, userId, true); return; }
        DocMember member = docMemberMapper.selectByDocIdAndUserId(docId, userId);
        if (member == null || member.getPermission() != PERMISSION_MANAGE) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
