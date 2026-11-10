package com.collaboffice.search.service;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class SearchQueryService {
    private final SearchService engine;
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;

    public List<Map<String,Object>> search(String keyword,String scope,LocalDate from,LocalDate to,Long creatorId,String tag){
        validate(keyword,scope,from,to,tag);if(keyword==null||keyword.isBlank())return List.of();
        String term=keyword.trim(),kind=Objects.toString(scope,"");Long space=access.currentSpace();
        var results=new ArrayList<Map<String,Object>>();
        if(Set.of("","document","sheet","mindmap","whiteboard").contains(kind)){
            // Keep indexed full-text matches, then load current metadata and apply access checks.
            var indexed=engine.globalSearch(term,kind.isEmpty()?"document":kind).stream().filter(r->!Set.of("task","user").contains(r.get("type"))).map(r->Objects.toString(r.get("id"))).limit(100).toList();
            String docTime="COALESCE(d.create_time,(SELECT MIN(dc.edit_time) FROM doc_content dc WHERE dc.doc_id=d.doc_id))";
            var args=new ArrayList<Object>();args.add(space);String sql="SELECT d.doc_id AS id,d.title,d.type AS documentType,d.creator_id AS creatorId,COALESCE(u.real_name,u.username,d.creator_name) AS creatorName,"+docTime+" AS createdAt,(d.create_time IS NULL) AS firstVersionTime,d.tags,LEFT(c.content,1000) AS snippet FROM doc_document d LEFT JOIN sys_user u ON u.id=d.creator_id LEFT JOIN doc_content c ON c.doc_id=d.doc_id AND c.version=d.current_version WHERE d.space_id=? AND d.status=1 AND d.deleted=0";
            if(!kind.isEmpty()){sql+=" AND d.type=?";args.add(switch(kind){case "sheet"->2;case "mindmap"->3;case "whiteboard"->4;default->1;});}
            sql+=" AND (d.title LIKE ? ESCAPE '!' OR c.content LIKE ? ESCAPE '!'";args.add(like(term));args.add(like(term));
            sql+=" OR EXISTS (SELECT 1 FROM sheet_sheet ss JOIN sheet_cell sc ON sc.sheet_id=ss.id WHERE ss.spreadsheet_id=d.doc_id AND sc.content LIKE ? ESCAPE '!') OR EXISTS (SELECT 1 FROM mindmap_node mn WHERE mn.map_id=d.doc_id AND mn.content LIKE ? ESCAPE '!')";args.add(like(term));args.add(like(term));
            if(!indexed.isEmpty()){sql+=" OR d.doc_id IN ("+String.join(",",Collections.nCopies(indexed.size(),"?"))+")";args.addAll(indexed);}sql+=")";
            sql+=filters(docTime,"d.creator_id","d.tags",from,to,creatorId,tag,args);
            for(var row:jdbc.queryForList(sql+" ORDER BY createdAt DESC,d.id DESC LIMIT 100",args.toArray()))try{access.requireDocument(row.get("id").toString(),false);row.put("type",switch(((Number)row.get("documentType")).intValue()){case 2->"sheet";case 3->"mindmap";case 4->"whiteboard";default->"document";});results.add(row);}catch(BusinessException ignored){}
        }
        if(kind.isEmpty()||kind.equals("task")){
            var args=new ArrayList<Object>(List.of(space,like(term),like(term)));String sql="SELECT t.task_id AS id,t.title,'task' AS type,t.description AS snippet,t.creator_id AS creatorId,COALESCE(u.real_name,u.username) AS creatorName,t.create_time AS createdAt,t.tags FROM task_task t JOIN task_project p ON p.project_id=t.project_id LEFT JOIN sys_user u ON u.id=t.creator_id WHERE p.space_id=? AND p.deleted=0 AND t.deleted=0 AND (t.title LIKE ? OR t.description LIKE ?)";
            sql=sql.replace("t.title LIKE ? OR t.description LIKE ?","t.title LIKE ? ESCAPE '!' OR t.description LIKE ? ESCAPE '!'");sql+=filters("t.create_time","t.creator_id","t.tags",from,to,creatorId,tag,args);
            for(var row:jdbc.queryForList(sql+" ORDER BY t.create_time DESC,t.id DESC LIMIT 100",args.toArray()))try{access.requireTask(row.get("id").toString(),false);results.add(row);}catch(BusinessException ignored){}
        }
        if((kind.isEmpty()||kind.equals("message"))&&(tag==null||tag.isBlank())){
            var args=new ArrayList<Object>(List.of(access.userId(),space,like(term)));String sql="SELECT m.msg_id AS id,m.session_id AS sessionId,s.session_name AS sessionName,'message' AS type,m.content AS title,m.content AS snippet,m.sender_id AS creatorId,COALESCE(u.real_name,u.username,m.sender_name) AS creatorName,m.send_time AS createdAt FROM chat_message m JOIN chat_session s ON s.session_id=m.session_id JOIN chat_session_member sm ON sm.session_id=m.session_id AND sm.user_id=? LEFT JOIN sys_user u ON u.id=m.sender_id WHERE s.space_id=? AND s.status=1 AND m.status=1 AND COALESCE(m.is_recalled,0)=0 AND m.msg_type=1 AND m.content LIKE ?";
            sql+=" ESCAPE '!'";sql+=filters("m.send_time","m.sender_id",null,from,to,creatorId,null,args);results.addAll(jdbc.queryForList(sql+" ORDER BY m.send_time DESC,m.id DESC LIMIT 100",args.toArray()));
        }
        if((kind.isEmpty()||kind.equals("user"))&&from==null&&to==null&&creatorId==null&&(tag==null||tag.isBlank()))results.addAll(engine.globalSearch(term,"user"));
        return results;
    }
    static void validate(String keyword,String scope,LocalDate from,LocalDate to,String tag){
        if(keyword!=null&&keyword.length()>100||tag!=null&&tag.length()>50)throw new BusinessException(400,"搜索词最多100字，标签最多50字");
        if(!Set.of("","document","sheet","mindmap","whiteboard","task","message","user").contains(Objects.toString(scope,"")))throw new BusinessException(400,"不支持的搜索范围");
        if(from!=null&&to!=null&&from.isAfter(to))throw new BusinessException(400,"开始日期不能晚于结束日期");
    }
    static String like(String text){return "%"+text.replace("!","!!").replace("%","!%").replace("_","!_")+"%";}
    static String filters(String time,String creator,String tags,LocalDate from,LocalDate to,Long creatorId,String tag,List<Object> args){
        String sql="";if(from!=null){sql+=" AND "+time+">=?";args.add(from.atStartOfDay());}if(to!=null){sql+=" AND "+time+"<?";args.add(to.plusDays(1).atStartOfDay());}if(creatorId!=null){sql+=" AND "+creator+"=?";args.add(creatorId);}if(tags!=null&&tag!=null&&!tag.isBlank()){sql+=" AND "+tags+" LIKE ? ESCAPE '!'";args.add(like(tag.trim()));}return sql;
    }
}
