package com.collaboffice.msg.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.msg.service.ChatService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/v1/im")
@RequiredArgsConstructor
public class VoiceMessageController {

    private final ChatService chatService;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:doc-files}")
    private String bucketName;

    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @PostMapping("/message/upload-voice")
    public CommonResult<Map<String, Object>> uploadVoice(@RequestHeader("X-User-Id") Long userId,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam(required = false) Double duration) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "webm";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            }
            
            String objectName = "voice/" + userId + "/" + System.currentTimeMillis() + "." + extension;
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType("audio/" + extension)
                    .build());

            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("url", url);
            result.put("duration", duration != null ? duration : 0.0);
            result.put("format", extension);
            return CommonResult.success(result);
        } catch (Exception e) {
            log.error("Voice upload failed", e);
            return CommonResult.failed(500, "上传失败: " + e.getMessage());
        }
    }
}
