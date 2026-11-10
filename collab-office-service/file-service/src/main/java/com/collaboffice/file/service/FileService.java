package com.collaboffice.file.service;

import java.io.InputStream;
import java.util.*;

public interface FileService {
    List<Map<String, Object>> getFileList(Long spaceId, Long folderId);
    String createFolder(Long userId, String folderName, Long parentId);
    Map<String, Object> uploadFile(Long userId, String fileName, Long fileSize, Long folderId);
    Map<String, Object> uploadFileWithStream(Long userId, String fileName, Long fileSize, Long folderId, InputStream inputStream);
    String getDownloadUrl(String fileId);
    void deleteFile(String fileId);
    void moveFile(String fileId, Long targetFolderId);
    List<Map<String, Object>> searchFiles(String keyword);
    Map<String, Object> getPreviewStatus(String fileId);
    void convertToPdfAsync(String fileId);
    Map<String, Object> checkConflict(String fileId, long version, String lastModified);
}
