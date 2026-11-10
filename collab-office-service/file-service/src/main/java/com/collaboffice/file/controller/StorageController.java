package com.collaboffice.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.file.entity.FileFile;
import com.collaboffice.file.mapper.FileFileMapper;
import com.collaboffice.file.mapper.FileFolderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class StorageController {

    private final FileFileMapper fileFileMapper;
    private final FileFolderMapper fileFolderMapper;

    @GetMapping("/storage/stats")
    public CommonResult<Map<String, Object>> getStorageStats(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Total files
        long totalFiles = fileFileMapper.selectCount(
            new LambdaQueryWrapper<FileFile>().eq(FileFile::getStatus, 1)
        );
        stats.put("totalFiles", totalFiles);

        // Total folders
        long totalFolders = fileFolderMapper.selectCount(new LambdaQueryWrapper<>());
        stats.put("totalFolders", totalFolders);

        // Total size
        long totalSize = 0;
        try {
            List<FileFile> allFiles = fileFileMapper.selectList(
                new LambdaQueryWrapper<FileFile>().eq(FileFile::getStatus, 1)
            );
            totalSize = allFiles.stream()
                .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0)
                .sum();
        } catch (Exception ignored) {}
        stats.put("totalSize", totalSize);
        stats.put("totalSizeReadable", formatSize(totalSize));

        // File type distribution
        Map<String, Long> typeCount = new LinkedHashMap<>();
        long docCount = fileFileMapper.selectCount(
            new LambdaQueryWrapper<FileFile>().eq(FileFile::getStatus, 1)
                .likeRight(FileFile::getMimeType, "application/")
        );
        long mediaCount = fileFileMapper.selectCount(
            new LambdaQueryWrapper<FileFile>().eq(FileFile::getStatus, 1)
                .and(w -> w.likeRight(FileFile::getMimeType, "image/")
                       .or().likeRight(FileFile::getMimeType, "video/")
                       .or().likeRight(FileFile::getMimeType, "audio/"))
        );
        typeCount.put("document", docCount);
        typeCount.put("media", mediaCount);
        typeCount.put("other", totalFiles - docCount - mediaCount);
        stats.put("typeDistribution", typeCount);

        // Quota (configurable, hardcoded 4TB for now)
        stats.put("quota", 4L * 1024 * 1024 * 1024 * 1024); // 4TB in bytes
        stats.put("usagePercent", totalSize > 0 ? Math.round((double) totalSize / (4L * 1024 * 1024 * 1024 * 1024) * 100) : 0);

        return CommonResult.success(stats);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
