package com.collaboffice.doc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.vo.DocumentDetailVO;
import com.collaboffice.doc.vo.VersionVO;
import com.collaboffice.doc.vo.VersionDiffVO;

import java.util.List;

public interface DocumentService {
    String create(Long userId, String userName, DocumentCreateRequest request);
    void update(Long userId, DocumentUpdateRequest request);
    void delete(Long userId, String docId);
    void recover(Long userId, String docId);
    Page<DocumentDetailVO> getList(DocumentListRequest request);
    DocumentDetailVO getDetail(String docId);
    String duplicate(Long userId, String docId);
    void move(String docId, Long folderId);
    void favorite(Long userId, String docId, Boolean fav);
    List<VersionVO> listVersions(String docId, int page, int size);
    VersionDiffVO compareVersions(String docId, Long v1, Long v2);
    void rollback(Long userId, String docId, Long targetVersion);
    void createSnapshot(Long userId, String docId, String name);
}
