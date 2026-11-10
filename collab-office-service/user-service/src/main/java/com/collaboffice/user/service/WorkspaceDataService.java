package com.collaboffice.user.service;

import com.alibaba.fastjson2.JSON;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.time.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

@Service @RequiredArgsConstructor
public class WorkspaceDataService {
    private final JdbcTemplate jdbc;
    private final WorkspaceAccess access;
    private final MinioClient minio;
    private final String minioBucketName;
    private List<Map<String,Object>> documents(boolean personal){
        Long actor=access.userId(),space=access.currentSpace();
        return jdbc.queryForList("SELECT * FROM doc_document WHERE space_id=? AND status=1 AND deleted=0"+(personal?" AND creator_id=?":"")+" ORDER BY id",personal?new Object[]{space,actor}:new Object[]{space}).stream().filter(d->{try{access.requireDocument(d.get("doc_id").toString(),false);return true;}catch(BusinessException e){return false;}}).toList();
    }
    private List<Map<String,Object>> files(boolean personal){return jdbc.queryForList("SELECT * FROM file_file WHERE space_id=? AND deleted=0 AND status=1"+(personal?" AND uploader_id=?":"")+" ORDER BY id",personal?new Object[]{access.currentSpace(),access.userId()}:new Object[]{access.currentSpace()}).stream().filter(f->{try{access.requireFolderOperation(Objects.toString(f.get("folder_id"),null),access.currentSpace(),access.userId(),"view");return true;}catch(BusinessException e){return false;}}).toList();}
    private List<Map<String,Object>> tasks(boolean personal){return jdbc.queryForList("SELECT t.* FROM task_task t JOIN task_project p ON p.project_id=t.project_id WHERE p.space_id=? AND p.deleted=0 AND t.deleted=0"+(personal?" AND (t.creator_id=? OR t.assignee_id=?)":"")+" ORDER BY t.id",personal?new Object[]{access.currentSpace(),access.userId(),access.userId()}:new Object[]{access.currentSpace()}).stream().filter(t->{try{access.requireTask(t.get("task_id").toString(),false);return true;}catch(BusinessException e){return false;}}).toList();}
    public Map<String,Object> statistics(boolean personal){
        var docs=documents(personal);var files=files(personal);var tasks=tasks(personal);var days=new ArrayList<Map<String,Object>>();
        long bytes=files.stream().mapToLong(f->((Number)f.get("file_size")).longValue()).sum();
        var visibleIds=new HashSet<>(docs.stream().map(d->d.get("doc_id").toString()).toList());
        var editRows=jdbc.queryForList("SELECT c.doc_id,c.edit_time FROM doc_content c JOIN doc_document d ON d.doc_id=c.doc_id WHERE d.space_id=? AND d.deleted=0 AND c.edit_time>=DATE_SUB(CURDATE(),INTERVAL 13 DAY)"+(personal?" AND c.editor_id=?":""),personal?new Object[]{access.currentSpace(),access.userId()}:new Object[]{access.currentSpace()});
        for(int i=13;i>=0;i--){String date=LocalDate.now().minusDays(i).toString();long added=files.stream().filter(f->dateOf(f.get("create_time")).equals(date)).mapToLong(f->((Number)f.get("file_size")).longValue()).sum();long edits=editRows.stream().filter(e->visibleIds.contains(e.get("doc_id").toString())&&dateOf(e.get("edit_time")).equals(date)).count();days.add(Map.of("date",date,"newFileBytes",added,"edits",edits));}
        return Map.of("documents",docs.size(),"files",files.size(),"bytes",bytes,"tasks",tasks.size(),"completed",tasks.stream().filter(t->Objects.equals(((Number)t.get("status")).intValue(),2)).count(),"trend",days,"scope",personal?"personal":"team","generatedAt",LocalDateTime.now().toString());
    }
    static String dateOf(Object value){return value==null?"":value.toString().substring(0,10);}
    public static String objectName(String url,String bucket){try{String path=java.net.URI.create(url).getPath(),prefix="/"+bucket+"/";if(!path.startsWith(prefix)||path.length()==prefix.length())throw new IllegalArgumentException();return path.substring(prefix.length());}catch(Exception e){throw new BusinessException(400,"文件不属于当前对象存储桶");}}
    public byte[] export(Set<String> kinds,boolean personal) throws Exception {
        if(kinds.isEmpty()||!Set.of("documents","files","messages","tasks").containsAll(kinds))throw new BusinessException(400,"请选择有效导出类型");
        access.requireOperation(access.currentSpace(),access.userId(),"download");
        var docs=kinds.contains("documents")?documents(personal).stream().filter(d->{try{access.requireDocumentOperation(d.get("doc_id").toString(),"download");return true;}catch(BusinessException e){return false;}}).toList():List.<Map<String,Object>>of();var files=kinds.contains("files")?files(personal).stream().filter(f->{try{access.requireFolderOperation(Objects.toString(f.get("folder_id"),null),access.currentSpace(),access.userId(),"download");return true;}catch(BusinessException e){return false;}}).toList():List.<Map<String,Object>>of();var tasks=kinds.contains("tasks")?tasks(personal):List.<Map<String,Object>>of();
        if(docs.size()+files.size()+tasks.size()>5000||files.stream().mapToLong(f->((Number)f.get("file_size")).longValue()).sum()>100*1024*1024)throw new BusinessException(400,"单次导出最多5000项、云盘文件总计100MB，请缩小范围");
        Path temp=Files.createTempFile("workspace-export-",".zip");
        try{
            try(var zip=new ZipOutputStream(Files.newOutputStream(temp),StandardCharsets.UTF_8)){
                json(zip,"manifest.json",Map.of("spaceId",access.currentSpace(),"scope",personal?"personal":"team","generatedAt",LocalDateTime.now().toString(),"kinds",kinds,"documentFormat","JSON with content and structured editor data"));
                for(var doc:docs){String id=doc.get("doc_id").toString();var item=new LinkedHashMap<String,Object>();item.put("document",doc);item.put("content",jdbc.queryForList("SELECT * FROM doc_content WHERE doc_id=? ORDER BY version DESC LIMIT 1",id));item.put("sheets",jdbc.queryForList("SELECT * FROM sheet_sheet WHERE spreadsheet_id=? ORDER BY id",id));item.put("cells",jdbc.queryForList("SELECT c.* FROM sheet_cell c JOIN sheet_sheet s ON s.id=c.sheet_id WHERE s.spreadsheet_id=? ORDER BY c.id",id));item.put("nodes",jdbc.queryForList("SELECT * FROM mindmap_node WHERE map_id=? ORDER BY id",id));item.put("elements",jdbc.queryForList("SELECT * FROM whiteboard_element WHERE board_id=? ORDER BY id",id));json(zip,"documents/"+id+".json",item);}
                if(kinds.contains("tasks"))json(zip,"tasks.json",tasks);
                if(kinds.contains("messages")){var messages=jdbc.queryForList("SELECT m.* FROM chat_message m JOIN chat_session s ON s.session_id=m.session_id JOIN chat_session_member sm ON sm.session_id=s.session_id AND sm.user_id=? WHERE s.space_id=? AND s.status=1 AND m.is_recalled=0 ORDER BY m.id LIMIT 10001",access.userId(),access.currentSpace());if(messages.size()>10000)throw new BusinessException(400,"消息超过10000条，请分会话导出");json(zip,"messages.json",messages);}
                if(kinds.contains("files")){json(zip,"files/index.json",files);long copied=0;for(var file:files){String id=file.get("file_id").toString();zip.putNextEntry(new ZipEntry("files/"+id+"-"+file.get("file_name").toString().replaceAll("[\\\\/:*?\"<>|]","_")));try(var input=minio.getObject(GetObjectArgs.builder().bucket(minioBucketName).object(objectName(file.get("file_url").toString(),minioBucketName)).build())){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))>=0){copied+=n;if(copied>100*1024*1024)throw new BusinessException(400,"文件实际大小超过100MB");zip.write(buffer,0,n);}}zip.closeEntry();}}
            }
            if(Files.size(temp)>150*1024*1024)throw new BusinessException(400,"压缩包超过150MB");return Files.readAllBytes(temp);
        }finally{Files.deleteIfExists(temp);}
    }
    private void json(ZipOutputStream zip,String name,Object body)throws Exception{zip.putNextEntry(new ZipEntry(name));zip.write(JSON.toJSONBytes(body));zip.closeEntry();}
}
