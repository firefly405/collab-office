package com.collaboffice.file.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final com.collaboffice.file.mapper.FileFileMapper fileMapper;

    @GetMapping("/{fileId}")
    public CommonResult<com.collaboffice.file.entity.FileFile> detail(@PathVariable String fileId) {
        var file = fileMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.collaboffice.file.entity.FileFile>().eq(com.collaboffice.file.entity.FileFile::getFileId, fileId));
        file.setFileUrl(fileService.getDownloadUrl(fileId));
        return CommonResult.success(file);
    }

    @PostMapping("/upload")
    public CommonResult<Map<String, Object>> upload(@RequestHeader("X-User-Id") Long userId,
                                                    @RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "folderId", required = false) Long folderId) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        long fileSize = file.getSize();
        log.info("文件上传: {} ({} bytes)", fileName, fileSize);
        try {
            return CommonResult.success(fileService
                    .uploadFileWithStream(userId, fileName, fileSize, folderId, file.getInputStream()));
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return CommonResult.failed(500, "文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileId}")
    public CommonResult<Map<String, Object>> download(@PathVariable String fileId) {
        String url = fileService.getDownloadUrl(fileId);
        if (url == null) return CommonResult.failed(404, "文件不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("downloadUrl", url);
        return CommonResult.success(result);
    }

    @PostMapping("/preview/{fileId}")
    public CommonResult<Map<String, Object>> preview(@PathVariable String fileId) {
        return CommonResult.success(fileService.getPreviewStatus(fileId));
    }

    @PostMapping("/upload-meta")
    public CommonResult<Map<String, Object>> uploadMeta(@RequestHeader("X-User-Id") Long userId,
                                                         @RequestParam String fileName,
                                                         @RequestParam Long fileSize,
                                                         @RequestParam(required = false) Long folderId) {
        return CommonResult.success(fileService.uploadFile(userId, fileName, fileSize, folderId));
    }

    @GetMapping("/list")
    public CommonResult<List<Map<String, Object>>> list(@RequestParam Long spaceId,
                                                         @RequestParam(required = false) Long folderId) {
        return CommonResult.success(fileService.getFileList(spaceId, folderId));
    }

    @PostMapping("/folder")
    public CommonResult<String> createFolder(@RequestHeader("X-User-Id") Long userId,
                                             @RequestParam String folderName,
                                             @RequestParam(required = false) Long parentId) {
        return CommonResult.success(fileService.createFolder(userId, folderName, parentId));
    }

    @DeleteMapping("/{fileId}")
    public CommonResult<Void> delete(@PathVariable String fileId) {
        fileService.deleteFile(fileId);
        return CommonResult.success(null);
    }

    @PutMapping("/{fileId}/move")
    public CommonResult<Void> move(@PathVariable String fileId, @RequestParam(required=false) Long targetFolderId) {
        fileService.moveFile(fileId, targetFolderId);
        return CommonResult.success(null);
    }

    @GetMapping("/search")
    public CommonResult<List<Map<String, Object>>> search(@RequestParam String keyword) {
        return CommonResult.success(fileService.searchFiles(keyword));
    }

    @PostMapping("/{fileId}/check-conflict")
    public CommonResult<Map<String, Object>> checkConflict(@PathVariable String fileId,
                                                            @RequestBody Map<String, Object> body) {
        long lastKnownVersion = ((Number) body.get("version")).longValue();
        String lastModifiedStr = (String) body.get("lastModified");
        return CommonResult.success(fileService.checkConflict(fileId, lastKnownVersion, lastModifiedStr));
    }
}
