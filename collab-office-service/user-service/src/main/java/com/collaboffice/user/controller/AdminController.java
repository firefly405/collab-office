package com.collaboffice.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.common.annotation.RequirePermission;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.user.entity.*;
import com.collaboffice.user.mapper.*;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SysLogMapper sysLogMapper;
    private final SysUserMapper sysUserMapper;
    private final SysSpaceMapper sysSpaceMapper;
    private final MinioClient minioClient;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Value("${minio.bucket-name:doc-files}")
    private String bucketName;

    // === Dashboard Stats ===

    @GetMapping("/dashboard/stats")
    public CommonResult<Map<String, Object>> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Active users (status=1)
        long activeUsers = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1)
        );
        stats.put("activeUsers", activeUsers);

        // Space count
        long spaces = sysSpaceMapper.selectCount(new LambdaQueryWrapper<SysSpace>().eq(SysSpace::getStatus, 1));
        stats.put("spaces", spaces);

        // Dashboard counts the existing management audit, not an unpopulated system log.
        long todayOps = jdbc.queryForObject("SELECT COUNT(*) FROM workspace_audit WHERE created_at>=?",Long.class,LocalDate.now().atStartOfDay());
        stats.put("todayOperations", todayOps);

        // Total logs
        long totalLogs = jdbc.queryForObject("SELECT COUNT(*) FROM workspace_audit",Long.class);
        stats.put("totalLogs", totalLogs);

        // Last 7 days operation trend
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            long count = jdbc.queryForObject("SELECT COUNT(*) FROM workspace_audit WHERE created_at>=? AND created_at<?",Long.class,day.atStartOfDay(),day.plusDays(1).atStartOfDay());
            trend.add(Map.of("date", day.toString(), "count", count));
        }
        stats.put("trend", trend);

        return CommonResult.success(stats);
    }

    // === Audit Logs ===

    @RequirePermission("PERMISSION:AUDIT_LOG:READ")
    @GetMapping("/logs")
    public CommonResult<Page<SysLog>> getLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        Page<SysLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
            .orderByDesc(SysLog::getCreateTime);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysLog::getUserName, keyword)
                          .or().like(SysLog::getOperation, keyword)
                          .or().like(SysLog::getRequestUrl, keyword));
        }
        if (module != null && !module.isEmpty()) {
            wrapper.eq(SysLog::getModule, module);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SysLog::getCreateTime, java.time.LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SysLog::getCreateTime, java.time.LocalDateTime.parse(endTime));
        }

        sysLogMapper.selectPage(page, wrapper);
        return CommonResult.success(page);
    }

    @RequirePermission("PERMISSION:AUDIT_LOG:READ")
    @GetMapping("/logs/stats")
    public CommonResult<Map<String, Object>> logsStats() {
        Map<String, Object> s = new LinkedHashMap<>();
        long today = sysLogMapper.selectCount(
            new LambdaQueryWrapper<SysLog>().ge(SysLog::getCreateTime, LocalDate.now().atStartOfDay())
        );
        long error = sysLogMapper.selectCount(
            new LambdaQueryWrapper<SysLog>().eq(SysLog::getStatus, 0).ge(SysLog::getCreateTime, LocalDate.now().atStartOfDay())
        );
        s.put("todayTotal", today);
        s.put("todayErrors", error);
        return CommonResult.success(s);
    }

    @RequirePermission("PERMISSION:AUDIT_LOG:READ")
    @GetMapping("/audit-logs")
    public CommonResult<Page<SysLog>> auditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        Page<SysLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
            .orderByDesc(SysLog::getCreateTime);

        if (userId != null) {
            wrapper.eq(SysLog::getUserId, userId);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(SysLog::getOperation, operationType);
        }
        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(SysLog::getModule, resourceType);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SysLog::getCreateTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SysLog::getCreateTime, LocalDateTime.parse(endTime));
        }

        sysLogMapper.selectPage(page, wrapper);
        return CommonResult.success(page);
    }

    @RequirePermission("PERMISSION:AUDIT_LOG:READ")
    @GetMapping("/audit-logs/export")
    public CommonResult<String> exportAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String resourceType) {

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Logs");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("User ID");
            headerRow.createCell(2).setCellValue("User Name");
            headerRow.createCell(3).setCellValue("Operation");
            headerRow.createCell(4).setCellValue("Module");
            headerRow.createCell(5).setCellValue("IP");
            headerRow.createCell(6).setCellValue("URL");
            headerRow.createCell(7).setCellValue("Cost Time (ms)");
            headerRow.createCell(8).setCellValue("Status");
            headerRow.createCell(9).setCellValue("Create Time");

            LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .orderByDesc(SysLog::getCreateTime);

            if (userId != null) {
                wrapper.eq(SysLog::getUserId, userId);
            }
            if (operationType != null && !operationType.isEmpty()) {
                wrapper.eq(SysLog::getOperation, operationType);
            }
            if (resourceType != null && !resourceType.isEmpty()) {
                wrapper.eq(SysLog::getModule, resourceType);
            }
            if (startTime != null && !startTime.isEmpty()) {
                wrapper.ge(SysLog::getCreateTime, LocalDateTime.parse(startTime));
            }
            if (endTime != null && !endTime.isEmpty()) {
                wrapper.le(SysLog::getCreateTime, LocalDateTime.parse(endTime));
            }

            List<SysLog> logs = sysLogMapper.selectList(wrapper);

            int rowIdx = 1;
            for (SysLog logItem : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(logItem.getId());
                row.createCell(1).setCellValue(logItem.getUserId() != null ? logItem.getUserId() : 0L);
                row.createCell(2).setCellValue(logItem.getUserName() != null ? logItem.getUserName() : "");
                row.createCell(3).setCellValue(logItem.getOperation() != null ? logItem.getOperation() : "");
                row.createCell(4).setCellValue(logItem.getModule() != null ? logItem.getModule() : "");
                row.createCell(5).setCellValue(logItem.getRequestIp() != null ? logItem.getRequestIp() : "");
                row.createCell(6).setCellValue(logItem.getRequestUrl() != null ? logItem.getRequestUrl() : "");
                row.createCell(7).setCellValue(logItem.getCostTime() != null ? logItem.getCostTime() : 0L);
                row.createCell(8).setCellValue(logItem.getStatus() != null ? logItem.getStatus() : 0);
                row.createCell(9).setCellValue(logItem.getCreateTime() != null ? logItem.getCreateTime().toString() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            byte[] bytes = out.toByteArray();

            String objectName = "audit-logs/audit_logs_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ".xlsx";

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .build()
            );

            String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(3600)
                    .build()
            );

            return CommonResult.success(presignedUrl);
        } catch (Exception e) {
            log.error("Failed to export audit logs", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "导出失败: " + e.getMessage());
        }
    }
}
