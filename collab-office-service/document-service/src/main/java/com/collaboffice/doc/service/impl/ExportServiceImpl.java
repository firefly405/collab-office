package com.collaboffice.doc.service.impl;

import com.collaboffice.doc.entity.DocContent;
import com.collaboffice.doc.mapper.DocContentMapper;
import com.collaboffice.doc.vo.ExportTaskVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl {

    private final DocContentMapper docContentMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MinioClient minioClient;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    private static final String EXPORT_TASK_PREFIX = "doc:export:task:";

    @Value("${minio.bucket-name:doc-files}")
    private String bucketName;

    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;
    @Value("${app.export.font-path:}")
    private String pdfFontPath;

    public ExportTaskVO exportDocument(String docId, String format, Long userId) {
        return exportDocument(docId,format,userId,false,false);
    }

    public ExportTaskVO exportDocument(String docId,String format,Long userId,boolean includeComments,boolean includeVersions) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        ExportTaskVO task = ExportTaskVO.builder()
                .taskId(taskId)
                .status("PROCESSING")
                .progress(0)
                .build();
        saveTask(task);
        redisTemplate.opsForValue().set(EXPORT_TASK_PREFIX + taskId + ":owner", userId.toString(), 1, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(EXPORT_TASK_PREFIX + taskId + ":options",(includeComments?"comments ":"")+(includeVersions?"versions":""),1,TimeUnit.DAYS);

        // Trigger asynchronous task execution
        runExportAsync(taskId, docId, format, userId);

        return task;
    }

    @Async
    public void runExportAsync(String taskId, String docId, String format, Long userId) {
        ExportTaskVO task = getTaskStatus(taskId);
        try {
            DocContent content = docContentMapper.selectLatestByDocId(docId);
            if (content == null) {
                task.setStatus("FAILED");
                task.setErrorMsg("文档内容不存在");
                saveTask(task);
                return;
            }
            content.setContent(com.collaboffice.doc.service.DocumentExportContent.normalize(content.getContent()));
            String options=redisTemplate.opsForValue().get(EXPORT_TASK_PREFIX+taskId+":options");
            if(options!=null&&!options.isBlank())content.setContent(withAppendices(docId,content.getContent(),options));

            task.setProgress(20);
            saveTask(task);

            byte[] bytes;
            String contentType;
            String fileExtension;

            switch (format.toLowerCase()) {
                case "txt" -> {
                    bytes = toPlainText(content.getContent()).getBytes(StandardCharsets.UTF_8);
                    contentType = "text/plain";
                    fileExtension = ".txt";
                }
                case "md" -> {
                    bytes = toMarkdown(content.getContent()).getBytes(StandardCharsets.UTF_8);
                    contentType = "text/markdown";
                    fileExtension = ".md";
                }
                case "html" -> {
                    bytes = toHtml(content.getContent()).getBytes(StandardCharsets.UTF_8);
                    contentType = "text/html";
                    fileExtension = ".html";
                }
                case "docx" -> {
                    bytes = toWord(content.getContent());
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    fileExtension = ".docx";
                }
                case "pdf" -> {
                    bytes = toPdf(content.getContent());
                    contentType = "application/pdf";
                    fileExtension = ".pdf";
                }
                default -> {
                    bytes = content.getContent().getBytes(StandardCharsets.UTF_8);
                    contentType = "application/json";
                    fileExtension = ".json";
                }
            }

            task.setProgress(60);
            saveTask(task);

            // Upload to MinIO
            String objectName = "export/" + docId + "/" + taskId + fileExtension;
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(in, bytes.length, -1)
                        .contentType(contentType)
                        .build());

                String fileUrl = minioClient.getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder().bucket(bucketName).object(objectName).method(io.minio.http.Method.GET).expiry(1, TimeUnit.HOURS).build());
                task.setStatus("COMPLETED");
                task.setProgress(100);
                task.setFileUrl(fileUrl);
                log.info("Export finished: docId={}, format={}", docId, format);
            } catch (Exception e) {
                log.error("MinIO upload failed for export task", e);
                task.setStatus("FAILED");
                task.setErrorMsg("文件上传失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Async export failed", e);
            task.setStatus("FAILED");
            task.setErrorMsg("导出失败: " + e.getMessage());
        }
        saveTask(task);
    }

    public ExportTaskVO getTaskStatus(String taskId) {
        String json = redisTemplate.opsForValue().get(EXPORT_TASK_PREFIX + taskId);
        if (json != null) {
            try {
                return objectMapper.readValue(json, ExportTaskVO.class);
            } catch (Exception e) {
                log.error("Failed to parse export task from Redis", e);
            }
        }
        return ExportTaskVO.builder().taskId(taskId).status("NOT_FOUND").build();
    }

    private String withAppendices(String docId,String content,String options) throws Exception {
        var root=objectMapper.readTree(content);
        if(!root.has("blocks"))throw new IllegalArgumentException("文档格式不支持附录导出");
        var blocks=(com.fasterxml.jackson.databind.node.ArrayNode)root.get("blocks");
        if(options.contains("comments")) {
            blocks.addObject().put("type","h1").put("text","评论与批注");
            for(var row:jdbc.queryForList("SELECT id,parent_id,user_name,content,is_resolved,selection_range_json FROM doc_comment WHERE doc_id=? AND deleted=0 ORDER BY create_time,id",docId))
                blocks.addObject().put("type","paragraph").put("text","#"+row.get("id")+" "+row.get("user_name")+(row.get("parent_id")==null?"":" 回复 #"+row.get("parent_id"))+" ["+(Integer.valueOf(1).equals(row.get("is_resolved"))?"已解决":"未解决")+"] "+row.get("content")+(row.get("selection_range_json")==null?"":" · 选区 "+row.get("selection_range_json")));
        }
        if(options.contains("versions")) {
            blocks.addObject().put("type","h1").put("text","版本历史（最近100个版本）");
            for(var row:jdbc.queryForList("SELECT c.version,c.editor_name,c.edit_time,c.content,v.version_name,v.change_summary FROM doc_content c LEFT JOIN doc_version v ON v.id=(SELECT MAX(v2.id) FROM doc_version v2 WHERE BINARY v2.doc_id=BINARY c.doc_id AND v2.version=c.version) WHERE c.doc_id=? ORDER BY c.version DESC LIMIT 100",docId)) {
                blocks.addObject().put("type","h2").put("text","v"+row.get("version")+" "+java.util.Objects.toString(row.get("version_name"),"")+" · "+row.get("editor_name")+" · "+row.get("edit_time"));
                blocks.addObject().put("type","paragraph").put("text",java.util.Objects.toString(row.get("change_summary"),""));
                var previous=objectMapper.readTree(com.collaboffice.doc.service.DocumentExportContent.normalize(java.util.Objects.toString(row.get("content"),"")));
                if(previous.has("blocks"))blocks.addAll((com.fasterxml.jackson.databind.node.ArrayNode)previous.get("blocks"));
            }
        }
        return objectMapper.writeValueAsString(root);
    }

    public void requireTaskOwner(String taskId, Long userId) {
        if (!userId.toString().equals(redisTemplate.opsForValue().get(EXPORT_TASK_PREFIX + taskId + ":owner"))) throw new com.collaboffice.common.exception.BusinessException(403,"无权读取此导出任务");
    }

    private void saveTask(ExportTaskVO task) {
        try {
            String json = objectMapper.writeValueAsString(task);
            redisTemplate.opsForValue().set(EXPORT_TASK_PREFIX + task.getTaskId(), json, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("Failed to save export task to Redis", e);
        }
    }

    private String toPlainText(String content) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(content);
            StringBuilder sb = new StringBuilder();
            if (json.containsKey("blocks")) {
                com.alibaba.fastjson2.JSONArray blocks = json.getJSONArray("blocks");
                for (int i = 0; i < blocks.size(); i++) {
                    Object block = blocks.get(i);
                    if (block instanceof Map<?,?> b) {
                        Object text = b.get("text");
                        sb.append(text == null ? "" : text).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return content;
        }
    }

    private String toMarkdown(String content) {
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(content);
            StringBuilder sb = new StringBuilder();
            if (json.containsKey("blocks")) {
                com.alibaba.fastjson2.JSONArray blocks = json.getJSONArray("blocks");
                for (int i = 0; i < blocks.size(); i++) {
                    Object block = blocks.get(i);
                    if (block instanceof Map<?,?> b) {
                        Object typeVal = b.get("type");
                        String type = "";
                        if (typeVal instanceof Number n) {
                            type = "h" + n.intValue();
                        } else if (typeVal != null) {
                            String typeStr = String.valueOf(typeVal).toLowerCase();
                            if (typeStr.equals("1") || typeStr.equals("2") || typeStr.equals("3") ||
                                    typeStr.equals("4") || typeStr.equals("5") || typeStr.equals("6")) {
                                type = "h" + typeStr;
                            } else {
                                type = typeStr;
                            }
                        }
                        Object textVal = b.get("text");
                        String text = textVal != null ? String.valueOf(textVal) : "";
                        switch (type) {
                            case "h1", "heading1" -> sb.append("# ").append(text).append("\n\n");
                            case "h2", "heading2" -> sb.append("## ").append(text).append("\n\n");
                            case "h3", "heading3" -> sb.append("### ").append(text).append("\n\n");
                            case "h4", "heading4" -> sb.append("#### ").append(text).append("\n\n");
                            case "h5", "heading5" -> sb.append("##### ").append(text).append("\n\n");
                            case "h6", "heading6" -> sb.append("###### ").append(text).append("\n\n");
                            case "list" -> sb.append("- ").append(text).append("\n");
                            default -> sb.append(text).append("\n\n");
                        }
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return content;
        }
    }

    private String toHtml(String content) {
        return "<html><body>" + toPlainText(content).replace("\n", "<br>") + "</body></html>";
    }

    private byte[] toWord(String content) {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(content);
            if (json != null && json.containsKey("blocks")) {
                com.alibaba.fastjson2.JSONArray blocks = json.getJSONArray("blocks");
                for (int i = 0; i < blocks.size(); i++) {
                    Object block = blocks.get(i);
                    if (block instanceof Map<?,?> b) {
                        Object typeVal = b.get("type");
                        String type = "";
                        if (typeVal instanceof Number n) {
                            type = "h" + n.intValue();
                        } else if (typeVal != null) {
                            String typeStr = String.valueOf(typeVal).toLowerCase();
                            if (typeStr.equals("1") || typeStr.equals("2") || typeStr.equals("3") ||
                                    typeStr.equals("4") || typeStr.equals("5") || typeStr.equals("6")) {
                                type = "h" + typeStr;
                            } else {
                                type = typeStr;
                            }
                        }

                        Object textVal = b.get("text");
                        String text = textVal != null ? String.valueOf(textVal) : "";

                        org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = doc.createParagraph();
                        org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
                        run.setText(text);

                        switch (type) {
                            case "h1", "heading1" -> {
                                run.setBold(true);
                                run.setFontSize(22);
                            }
                            case "h2", "heading2" -> {
                                run.setBold(true);
                                run.setFontSize(18);
                            }
                            case "h3", "heading3" -> {
                                run.setBold(true);
                                run.setFontSize(16);
                            }
                            case "h4", "heading4", "h5", "heading5", "h6", "heading6" -> {
                                run.setBold(true);
                                run.setFontSize(14);
                            }
                            case "list" -> {
                                run.setText("• " + text);
                            }
                            default -> {
                                run.setFontSize(12);
                            }
                        }
                    }
                }
            } else {
                org.apache.poi.xwpf.usermodel.XWPFParagraph p = doc.createParagraph();
                p.createRun().setText(content != null ? content : "");
            }
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Word document", e);
            throw new IllegalStateException("Word 导出失败",e);
        }
    }

    private byte[] toPdf(String content) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            String fontPath=pdfFontPath;
            if(fontPath==null||fontPath.isBlank()) {
                fontPath=java.nio.file.Files.isRegularFile(java.nio.file.Path.of("C:/Windows/Fonts/simsun.ttc"))
                    ?"C:/Windows/Fonts/simsun.ttc,0":"/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc,0";
            }
            com.lowagie.text.pdf.BaseFont bfChinese = com.lowagie.text.pdf.BaseFont.createFont(
                    fontPath, com.lowagie.text.pdf.BaseFont.IDENTITY_H, com.lowagie.text.pdf.BaseFont.EMBEDDED);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(bfChinese, 20, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font subTitleFont = new com.lowagie.text.Font(bfChinese, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font boldFont = new com.lowagie.text.Font(bfChinese, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normalFont = new com.lowagie.text.Font(bfChinese, 12, com.lowagie.text.Font.NORMAL);

            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(content);
            if (json != null && json.containsKey("blocks")) {
                com.alibaba.fastjson2.JSONArray blocks = json.getJSONArray("blocks");
                for (int i = 0; i < blocks.size(); i++) {
                    Object block = blocks.get(i);
                    if (block instanceof Map<?,?> b) {
                        Object typeVal = b.get("type");
                        String type = "";
                        if (typeVal instanceof Number n) {
                            type = "h" + n.intValue();
                        } else if (typeVal != null) {
                            String typeStr = String.valueOf(typeVal).toLowerCase();
                            if (typeStr.equals("1") || typeStr.equals("2") || typeStr.equals("3") ||
                                    typeStr.equals("4") || typeStr.equals("5") || typeStr.equals("6")) {
                                type = "h" + typeStr;
                            } else {
                                type = typeStr;
                            }
                        }

                        Object textVal = b.get("text");
                        String text = textVal != null ? String.valueOf(textVal) : "";

                        com.lowagie.text.Paragraph p;
                        switch (type) {
                            case "h1", "heading1" -> {
                                p = new com.lowagie.text.Paragraph(text, titleFont);
                                p.setSpacingAfter(10f);
                            }
                            case "h2", "heading2" -> {
                                p = new com.lowagie.text.Paragraph(text, subTitleFont);
                                p.setSpacingAfter(8f);
                            }
                            case "h3", "heading3", "h4", "heading4", "h5", "heading5", "h6", "heading6" -> {
                                p = new com.lowagie.text.Paragraph(text, boldFont);
                                p.setSpacingAfter(6f);
                            }
                            case "list" -> {
                                p = new com.lowagie.text.Paragraph("• " + text, normalFont);
                                p.setIndentationLeft(20f);
                            }
                            default -> {
                                p = new com.lowagie.text.Paragraph(text, normalFont);
                                p.setSpacingAfter(4f);
                            }
                        }
                        doc.add(p);
                    }
                }
            } else {
                doc.add(new com.lowagie.text.Paragraph(content != null ? content : "", normalFont));
            }

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF document", e);
            throw new IllegalStateException("PDF 导出失败",e);
        }
    }
}
