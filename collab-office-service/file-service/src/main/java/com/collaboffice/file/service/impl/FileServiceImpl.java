package com.collaboffice.file.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.file.entity.*;
import com.collaboffice.file.mapper.FileFileMapper;
import com.collaboffice.file.mapper.FileFolderMapper;
import com.collaboffice.file.service.FileService;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileFileMapper fileFileMapper;
    private final FileFolderMapper fileFolderMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final MinioClient minioClient;
    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @Value("${minio.bucket-name:doc-files}")
    private String bucketName;

    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${app.libreoffice.cmd:soffice}")
    private String libreofficeCmd;

    @Value("${app.libreoffice.timeout-seconds:120}")
    private int libreofficeTimeoutSeconds;

    @Value("${app.libreoffice.work-dir:/tmp/libreoffice-work}")
    private String libreofficeWorkDir;

    @Override
    public List<Map<String, Object>> getFileList(Long spaceId, Long folderId) {
        access.requireSpace(spaceId, false);
        access.requireFolder(folderId, spaceId);
        List<Map<String, Object>> result = new ArrayList<>();
        List<FileFolder> folders = fileFolderMapper.selectBySpaceId(spaceId).stream().filter(f -> Objects.equals(f.getParentId(), folderId)).toList();
        for (FileFolder f : folders) {
            try{access.requireFolderOperation(f.getFolderId(),spaceId,access.userId(),"view");}catch(com.collaboffice.common.exception.BusinessException denied){continue;}
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("folderId", f.getFolderId());
            m.put("name", f.getFolderName());
            m.put("type", "folder");
            m.put("createTime", f.getCreateTime());
            result.add(m);
        }
        List<FileFile> files = fileFileMapper.selectBySpaceId(spaceId).stream().filter(f -> Objects.equals(f.getFolderId(), folderId)).toList();
        for (FileFile f : files) {
            try{access.requireFile(f.getFileId(),false);}catch(com.collaboffice.common.exception.BusinessException denied){continue;}
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fileId", f.getFileId());
            m.put("fileName", f.getFileName());
            m.put("fileSize", f.getFileSize());
            m.put("fileType", f.getFileType());
            m.put("type", "file");
            m.put("createTime", f.getCreateTime());
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional
    public String createFolder(Long userId, String folderName, Long parentId) {
        Long spaceId = access.requireSpace(access.currentSpace(), true);
        access.requireFolder(parentId, spaceId);
        access.requireFolderOperation(Objects.toString(parentId,null),spaceId,userId,"edit");
        FileFolder folder = new FileFolder();
        folder.setFolderId(SnowflakeIdGenerator.nextIdStr());
        folder.setFolderName(folderName);
        folder.setParentId(parentId);
        folder.setCreatorId(userId);
        folder.setSpaceId(spaceId);
        folder.setSortOrder(0);
        fileFolderMapper.insert(folder);
        return folder.getFolderId();
    }

    @Override
    @Transactional
    public Map<String, Object> uploadFile(Long userId, String fileName, Long fileSize, Long folderId) {
        return uploadFileWithStream(userId, fileName, fileSize, folderId, null);
    }

    @Override
    @Transactional
    public Map<String, Object> uploadFileWithStream(Long userId, String fileName, Long fileSize, Long folderId, InputStream inputStream) {
        Long spaceId = access.requireSpace(access.currentSpace(), true);
        access.requireUploadQuota(spaceId,fileSize);
        access.requireFolder(folderId, spaceId);
        access.requireFolderOperation(Objects.toString(folderId,null),spaceId,userId,"edit");
        String fileId = SnowflakeIdGenerator.nextIdStr();
        String objectName = fileId + "/" + fileName;

        // Upload to MinIO if stream is provided
        String fileUrl = null;
        if (inputStream != null) {
            try {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, fileSize, -1)
                        .contentType(getContentType(fileName))
                        .build());
                fileUrl = publicEndpoint + "/" + bucketName + "/" + objectName;
                log.info("File uploaded to MinIO: {}", objectName);
            } catch (Exception e) {
                log.error("MinIO upload failed for {}: {}", fileName, e.getMessage());
                throw new com.collaboffice.common.exception.BusinessException(503,"文件存储暂不可用，上传未完成");
            }
        }

        FileFile file = new FileFile();
        file.setFileId(fileId);
        file.setFileName(fileName);
        file.setFileSize(fileSize);
        file.setMimeType(getContentType(fileName));
        file.setFileUrl(fileUrl);
        file.setFolderId(folderId);
        file.setUploaderId(userId);
        file.setSpaceId(spaceId);
        file.setStatus(1);
        file.setVersion(1);
        fileFileMapper.insert(file);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileId", file.getFileId());
        result.put("fileName", fileName);
        result.put("fileSize", fileSize);
        result.put("fileUrl", getDownloadUrl(fileId));
        return result;
    }

    @Override
    public String getDownloadUrl(String fileId) {
        FileFile file = fileFileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                        .eq(FileFile::getFileId, fileId));
        if (file == null || file.getFileUrl() == null) return null;
        access.requireFile(fileId,false);
        access.requireOperation(file.getSpaceId(),access.userId(),"download");
        access.requireFolderOperation(Objects.toString(file.getFolderId(),null),file.getSpaceId(),access.userId(),"download");
        try {
            String objectName = fileId + "/" + file.getFileName();
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            throw new com.collaboffice.common.exception.BusinessException(503, "无法生成下载链接，请稍后重试");
        }
    }

    private String getContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "application/msword";
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return "application/vnd.ms-excel";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }

    @Override
    @Transactional
    public void deleteFile(String fileId) {
        FileFile file = new FileFile();
        file.setFileId(fileId);
        file.setStatus(0);
        fileFileMapper.update(file, new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                .eq(FileFile::getFileId, fileId));
    }

    @Override
    @Transactional
    public void moveFile(String fileId, Long targetFolderId) {
        FileFile origin = fileFileMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>().eq(FileFile::getFileId, fileId));
        access.requireFolder(targetFolderId, origin.getSpaceId());
        access.requireFolderOperation(Objects.toString(targetFolderId,null),origin.getSpaceId(),access.userId(),"edit");
        fileFileMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FileFile>()
                .eq(FileFile::getFileId, fileId).set(FileFile::getFolderId,targetFolderId));
    }

    @Override
    public List<Map<String, Object>> searchFiles(String keyword) {
        Long spaceId = access.currentSpace();
        return fileFileMapper.searchByKeyword(keyword).stream().filter(f -> Objects.equals(f.getSpaceId(), spaceId)).filter(f->{try{access.requireFile(f.getFileId(),false);return true;}catch(com.collaboffice.common.exception.BusinessException e){return false;}}).map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fileId", f.getFileId());
            m.put("fileName", f.getFileName());
            m.put("fileSize", f.getFileSize());
            m.put("type", "file");
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getPreviewStatus(String fileId) {
        FileFile file = fileFileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                        .eq(FileFile::getFileId, fileId));
        if (file == null) {
            return Map.of("status", "error", "message", "文件不存在");
        }

        String fileName = file.getFileName();
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            String url = getDownloadUrl(fileId);
            return Map.of("status", "ready", "url", url != null ? url : "");
        }

        String statusKey = "file:preview:status:" + fileId;
        String urlKey = "file:preview:url:" + fileId;

        String status = redisTemplate.opsForValue().get(statusKey);
        if (status == null) {
            redisTemplate.opsForValue().set(statusKey, "pending", 5, TimeUnit.MINUTES);
            java.util.concurrent.CompletableFuture.runAsync(() -> convertToPdfInternal(fileId));
            status = "pending";
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", status);
        if ("ready".equals(status)) {
            String url = redisTemplate.opsForValue().get(urlKey);
            res.put("url", url != null ? url : "");
        }
        return res;
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void convertToPdfAsync(String fileId) {
        convertToPdfInternal(fileId);
    }

    private void convertToPdfInternal(String fileId) {
        String statusKey = "file:preview:status:" + fileId;
        String urlKey = "file:preview:url:" + fileId;
        try {
            FileFile file = fileFileMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                            .eq(FileFile::getFileId, fileId));
            if (file == null) {
                redisTemplate.opsForValue().set(statusKey, "error");
                return;
            }

            byte[] pdfBytes = convertToPdfViaLibreOffice(file);
            if (pdfBytes == null) {
                // LibreOffice 不可用或转换失败，降级到占位 PDF（保证预览链路不中断）
                log.warn("LibreOffice conversion unavailable for fileId={}, falling back to placeholder PDF", fileId);
                pdfBytes = buildPlaceholderPdf();
            }

            String previewObjectName = "preview/" + fileId + ".pdf";
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(previewObjectName)
                    .stream(new java.io.ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf")
                    .build());

            String presignedUrl = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(previewObjectName)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());

            redisTemplate.opsForValue().set(statusKey, "ready", 1, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(urlKey, presignedUrl, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Failed to convert file {} to PDF preview", fileId, e);
            redisTemplate.opsForValue().set(statusKey, "error", 1, TimeUnit.HOURS);
        }
    }

    /**
     * 调用 LibreOffice headless 模式做真实文档 → PDF 转换。
     * 流程：MinIO 下载源文件 → 本地临时目录 → soffice --convert-to pdf → 读取 PDF 字节。
     * 返回 null 表示 LibreOffice 不可用或转换失败，调用方应做降级处理。
     */
    private byte[] convertToPdfViaLibreOffice(FileFile file) {
        String sourceObjectName = file.getFileId() + "/" + file.getFileName();
        java.nio.file.Path workDir = java.nio.file.Paths.get(libreofficeWorkDir, file.getFileId());
        java.nio.file.Path sourceFile = null;
        java.nio.file.Path pdfFile = null;

        try {
            java.nio.file.Files.createDirectories(workDir);

            // 1. 从 MinIO 下载源文件
            sourceFile = workDir.resolve(sanitizeFileName(file.getFileName()));
            try (InputStream src = minioClient.getObject(
                    io.minio.GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(sourceObjectName)
                            .build())) {
                java.nio.file.Files.copy(src, sourceFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // 2. 调用 LibreOffice
            pdfFile = runLibreOfficeConvert(sourceFile, workDir);
            if (pdfFile == null) {
                return null;
            }

            // 3. 读取 PDF 字节
            return java.nio.file.Files.readAllBytes(pdfFile);
        } catch (Exception e) {
            log.error("LibreOffice conversion failed for fileId={} fileName={}: {}",
                    file.getFileId(), file.getFileName(), e.getMessage());
            return null;
        } finally {
            // 清理临时文件
            safeDelete(sourceFile);
            safeDelete(pdfFile);
            safeDeleteDir(workDir);
        }
    }

    private java.nio.file.Path runLibreOfficeConvert(java.nio.file.Path sourceFile, java.nio.file.Path outDir) {
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    libreofficeCmd,
                    "--headless",
                    "--norestore",
                    "--nologo",
                    "--convert-to", "pdf",
                    "--outdir", outDir.toString(),
                    sourceFile.toString()
            );
            pb.redirectErrorStream(true);
            process = pb.start();

            // 读取输出避免管道阻塞
            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(libreofficeTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("LibreOffice conversion timed out after {}s for {}", libreofficeTimeoutSeconds, sourceFile);
                return null;
            }
            if (process.exitValue() != 0) {
                log.error("LibreOffice conversion failed (exit={}): {}", process.exitValue(), output);
                return null;
            }

            String pdfName = sourceFile.getFileName().toString().replaceAll("\\.[^.]+$", "") + ".pdf";
            java.nio.file.Path pdfPath = outDir.resolve(pdfName);
            if (!java.nio.file.Files.exists(pdfPath)) {
                log.error("LibreOffice reported success but PDF not found: {}", pdfPath);
                return null;
            }
            return pdfPath;
        } catch (java.io.IOException e) {
            // LibreOffice 命令不存在时通常抛出 IOException (Cannot run program "soffice")
            log.warn("LibreOffice command not available '{}': {}", libreofficeCmd, e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            log.error("LibreOffice conversion interrupted for {}", sourceFile);
            return null;
        }
    }

    private byte[] buildPlaceholderPdf() {
        return "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << >> /Contents 4 0 R >>\nendobj\n4 0 obj\n<< /Length 43 >>\nstream\nBT /F1 12 Tf 70 700 Td (Office Preview PDF) Tj ET\nendstream\nendobj\nxref\n0 5\n0000000000 65535 f\n0000000009 00000 n\n0000000058 00000 n\n0000000115 00000 n\n0000000216 00000 n\ntrailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n310\n%%EOF\n".getBytes();
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) return "source";
        // 防止目录穿越/非法字符
        return name.replaceAll("[\\\\/]", "_");
    }

    private void safeDelete(java.nio.file.Path path) {
        if (path == null) return;
        try {
            java.nio.file.Files.deleteIfExists(path);
        } catch (Exception ignored) { /* best effort */ }
    }

    private void safeDeleteDir(java.nio.file.Path dir) {
        if (dir == null) return;
        try {
            java.nio.file.Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { java.nio.file.Files.deleteIfExists(p); } catch (Exception ignored) { }
                    });
        } catch (Exception ignored) { /* best effort */ }
    }

    @Override
    public Map<String, Object> checkConflict(String fileId, long version, String lastModified) {
        var file = fileFileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileFile>()
                        .eq(FileFile::getFileId, fileId));
        Map<String, Object> result = new LinkedHashMap<>();
        if (file == null) {
            result.put("conflict", false);
            return result;
        }
        boolean conflict = file.getUpdateTime() != null
                && lastModified != null
                && file.getUpdateTime().toString().compareTo(lastModified) > 0;
        result.put("conflict", conflict);
        result.put("serverVersion", file.getVersion());
        if (conflict && file.getFileUrl() != null) {
            result.put("serverFileUrl", file.getFileUrl());
        }
        return result;
    }
}
