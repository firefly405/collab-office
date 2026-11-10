package com.collaboffice.doc.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DocumentHistoryService {
    private final JdbcTemplate jdbc;
    private final WorkspaceAccess access;

    public List<VersionVO> list(String docId,int page,int size) {
        access.requireDocument(docId,false);
        int limit=((Number)policy(docId).get("limit")).intValue();
        int count=Math.min(Math.max(size,1),100),offset=(Math.max(page,1)-1)*count;
        return jdbc.query("""
            SELECT c.version,COALESCE(v.version_name,CONCAT('v',c.version)) AS version_name,
            c.editor_name,c.edit_time,COALESCE(v.change_summary,c.remark) AS summary,c.is_snapshot
            FROM doc_content c LEFT JOIN doc_version v ON v.id=(SELECT MAX(v2.id) FROM doc_version v2 WHERE BINARY v2.doc_id=BINARY c.doc_id AND v2.version=c.version)
            WHERE c.doc_id=? AND (c.version >= (SELECT GREATEST(COALESCE(MAX(c2.version),1)-?+1,1) FROM doc_content c2 WHERE c2.doc_id=?) OR v.version_name IS NOT NULL)
            ORDER BY c.version DESC LIMIT ? OFFSET ?
            """,(rs,row)->{
                var result=new VersionVO();result.setDocId(docId);result.setVersion(rs.getLong("version"));
                result.setVersionName(rs.getString("version_name"));result.setEditorName(rs.getString("editor_name"));
                result.setChangeSummary(rs.getString("summary"));result.setIsSnapshot(rs.getInt("is_snapshot"));
                var time=rs.getTimestamp("edit_time");result.setCreatedTime(time==null?null:time.toLocalDateTime());return result;
            },docId,limit,docId,count,offset);
    }

    @Transactional
    public void snapshot(String docId,Long userId,String name,String remark) {
        access.requireDocument(docId,true);
        jdbc.queryForObject("SELECT current_version FROM doc_document WHERE doc_id=? FOR UPDATE",Long.class,docId);
        var versions=jdbc.queryForList("SELECT version FROM doc_content WHERE doc_id=? ORDER BY version DESC LIMIT 1",Long.class,docId);
        if(versions.isEmpty()) throw new BusinessException(404,"文档内容不存在");
        name(docId,versions.get(0),name,remark);
    }

    @Transactional
    public void name(String docId,Long version,String name,String remark) {
        access.requireDocument(docId,true);
        if(name==null||name.isBlank()||name.length()>100||remark!=null&&remark.length()>500) throw new BusinessException(400,"名称为1至100字，备注最多500字");
        jdbc.queryForObject("SELECT current_version FROM doc_document WHERE doc_id=? FOR UPDATE",Long.class,docId);
        var rows=jdbc.queryForList("SELECT content,editor_id,editor_name FROM doc_content WHERE doc_id=? AND version=?",docId,version);
        if(rows.isEmpty()) throw new BusinessException(404,"版本不存在");
        var content=rows.get(0);
        var ids=jdbc.queryForList("SELECT id FROM doc_version WHERE doc_id=? AND version=? ORDER BY id DESC LIMIT 1",Long.class,docId,version);
        if(ids.isEmpty()) jdbc.update("INSERT INTO doc_version(doc_id,version,version_name,change_summary,snapshot_data,editor_id,editor_name,is_snapshot) VALUES (?,?,?,?,?,?,?,1)",docId,version,name.trim(),Objects.toString(remark,""),content.get("content"),content.get("editor_id"),content.get("editor_name"));
        else jdbc.update("UPDATE doc_version SET version_name=?,change_summary=? WHERE id=?",name.trim(),Objects.toString(remark,""),ids.get(0));
        jdbc.update("UPDATE doc_content SET is_snapshot=1 WHERE doc_id=? AND version=?",docId,version);
    }

    public Map<String,Object> policy(String docId) {
        access.requireDocument(docId,false);
        return Map.of("limit",jdbc.queryForObject("SELECT version_retention FROM doc_document WHERE doc_id=?",Integer.class,docId));
    }

    public void policy(String docId,Integer limit) {
        access.requireDocument(docId,true);
        if(limit==null||limit<10||limit>1000) throw new BusinessException(400,"版本保留数量为10至1000");
        // shortcut: retention hides old unnamed versions without deleting content; add a backed-up purge job when storage reclamation is required.
        jdbc.update("UPDATE doc_document SET version_retention=? WHERE doc_id=?",limit,docId);
    }
}
