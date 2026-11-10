package com.collaboffice.common.security;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.*;

@Component
@RequiredArgsConstructor
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WorkspaceAccess {
    private final JdbcTemplate jdbc;

    public String platformRole(Long actor) {
        var roles = jdbc.queryForList("SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id JOIN sys_user u ON u.id=ur.user_id WHERE ur.user_id=? AND r.status=1 AND r.deleted=0 AND u.status=1 AND u.deleted=0", String.class, actor);
        if (roles.stream().anyMatch(r -> Set.of("ADMIN", "ROLE_ADMIN").contains(r.toUpperCase(Locale.ROOT)))) return "SYSTEM_ADMIN";
        if (roles.stream().anyMatch(r -> "PROJECT_ADMIN".equalsIgnoreCase(r))) return "PROJECT_ADMIN";
        return "MEMBER";
    }

    public void requireSystemAdmin() {
        if (!"SYSTEM_ADMIN".equals(platformRole(userId()))) deny();
    }

    public void requireProjectCreator() {
        if ("MEMBER".equals(platformRole(userId()))) deny();
    }

    public boolean canManageProject(String id) {
        var p = row("SELECT space_id,creator_id FROM task_project WHERE project_id=? AND deleted=0", id);
        requireSpace(number(p.get("space_id")), false);
        String role = platformRole(userId());
        return "SYSTEM_ADMIN".equals(role) || "PROJECT_ADMIN".equals(role) && Objects.equals(number(p.get("creator_id")), userId());
    }

    public void requireProject(String id, boolean manage) {
        var p = row("SELECT space_id,creator_id FROM task_project WHERE project_id=? AND deleted=0", id);
        requireSpace(number(p.get("space_id")), manage);
        String role = platformRole(userId());
        boolean manager = "SYSTEM_ADMIN".equals(role) || "PROJECT_ADMIN".equals(role) && Objects.equals(number(p.get("creator_id")), userId());
        if (manage && !manager) deny();
        if (!manager && !Objects.equals(number(p.get("creator_id")), userId()) && jdbc.queryForObject("SELECT COUNT(*) FROM task_project_member WHERE project_id=? AND user_id=?", Integer.class, id, userId()) == 0) deny();
    }

    public boolean canReadProject(String id) {
        try { requireProject(id, false); return true; } catch (BusinessException e) { return false; }
    }

    public Long userId() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        String value = attributes.getRequest().getHeader("X-User-Id");
        if (value == null) throw new BusinessException(ResultCode.UNAUTHORIZED);
        return Long.valueOf(value);
    }

    public Long currentSpace() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String value = request.getHeader("X-Space-Id");
        if (value != null && !value.isBlank()) return requireSpace(Long.valueOf(value), false);
        List<Long> spaces = jdbc.queryForList("SELECT m.space_id FROM sys_space_member m JOIN sys_space s ON s.id=m.space_id WHERE m.user_id=? AND s.deleted=0 AND s.status=1 ORDER BY m.space_id", Long.class, userId());
        if (spaces.isEmpty()) throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "请先创建或加入团队");
        return requireSpace(spaces.get(0),false);
    }

    public Long requireSpace(Long spaceId, boolean write) {
        List<String> roles = jdbc.queryForList("SELECT m.role_type FROM sys_space_member m JOIN sys_space s ON s.id=m.space_id WHERE m.space_id=? AND m.user_id=? AND s.deleted=0 AND s.status=1 AND (m.expires_at IS NULL OR m.expires_at>NOW())", String.class, spaceId, userId());
        if (roles.isEmpty() || write && ("3".equals(roles.get(0)) || "VISITOR".equalsIgnoreCase(roles.get(0)))) deny();
        requireOperation(spaceId,userId(),write?"edit":"view");
        return spaceId;
    }

    public boolean isAdmin(Long spaceId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_space_member m JOIN sys_space s ON s.id=m.space_id WHERE m.space_id=? AND m.user_id=? AND m.role_type IN ('1','ADMIN') AND s.status=1 AND s.deleted=0 AND (m.expires_at IS NULL OR m.expires_at>NOW())", Integer.class, spaceId, userId()) > 0;
    }

    public void requireFile(String fileId, boolean write) {
        Map<String,Object> file = row("SELECT space_id,uploader_id,folder_id FROM file_file WHERE file_id=? AND deleted=0 AND status=1", fileId);
        Long space = number(file.get("space_id"));
        if (space != null) requireSpace(space, write);
        if(space!=null)requireFolderOperation(Objects.toString(file.get("folder_id"),null),space,userId(),write?"edit":"view");
        if (!Objects.equals(number(file.get("uploader_id")), userId()) && (space == null || write && !isAdmin(space))) deny();
    }

    public void requireFolder(Long folderId, Long spaceId) {
        if (folderId == null) return;
        Map<String,Object> folder = row("SELECT space_id FROM file_folder WHERE folder_id=? AND deleted=0", folderId);
        if (!Objects.equals(number(folder.get("space_id")), spaceId)) deny();
        requireFolderOperation(folderId.toString(),spaceId,userId(),"view");
    }

    public void requireDocument(String docId, boolean write) {
        requireDocument(docId, userId(), write);
    }

    public void requireDocument(String docId, Long actor, boolean write) {
        Map<String,Object> doc = row("SELECT creator_id,space_id,is_public,folder_id FROM doc_document WHERE doc_id=? AND deleted=0", docId);
        requireOperation(number(doc.get("space_id")),actor,write?"edit":"view");
        if (Objects.equals(number(doc.get("creator_id")), actor)) return;
        List<Integer> permissions = jdbc.queryForList("SELECT permission FROM doc_member WHERE doc_id=? AND user_id=?", Integer.class, docId, actor);
        if (!permissions.isEmpty()) {
            if (write && !Set.of(2,3).contains(permissions.get(0))) deny();
            return;
        }
        requireFolderOperation(Objects.toString(doc.get("folder_id"),null),number(doc.get("space_id")),actor,write?"edit":"view");
        if (!write && Objects.equals(number(doc.get("is_public")), 1L)) return;
        List<String> roles = jdbc.queryForList("SELECT role_type FROM sys_space_member WHERE space_id=? AND user_id=?", String.class, doc.get("space_id"), actor);
        if (roles.isEmpty() || write && ("3".equals(roles.get(0)) || "VISITOR".equalsIgnoreCase(roles.get(0)))) deny();
    }

    public void requireTask(String taskId, boolean write) {
        Map<String,Object> task = row("SELECT t.creator_id,t.assignee_id,t.project_id,p.space_id FROM task_task t LEFT JOIN task_project p ON p.project_id=t.project_id AND p.deleted=0 WHERE t.task_id=? AND t.deleted=0", taskId);
        Long space = number(task.get("space_id"));
        if (space != null) {
            requireSpace(space, write);
            String project = task.get("project_id").toString();
            requireProject(project, false);
            if (write && !canManageProject(project) && !Objects.equals(number(task.get("creator_id")), userId()) && !Objects.equals(number(task.get("assignee_id")), userId())) deny();
        }
        else if (!Objects.equals(number(task.get("creator_id")), userId())) deny();
    }

    public void requireComment(String docId) {
        requireDocument(docId,false);
        var doc=row("SELECT creator_id FROM doc_document WHERE doc_id=? AND deleted=0",docId);
        if (Objects.equals(number(doc.get("creator_id")),userId())) return;
        var permissions=jdbc.queryForList("SELECT permission FROM doc_member WHERE doc_id=? AND user_id=?",Integer.class,docId,userId());
        if (!permissions.isEmpty()) { if(!Set.of(2,3,4).contains(permissions.get(0))) deny(); return; }
        requireDocument(docId,true);
    }

    public void requireTypedDocument(String kind, String id, boolean write) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM doc_document WHERE doc_id=? AND deleted=0",Integer.class,id)>0) { requireDocument(id,write); return; }
        String table = switch(kind) { case "sheet" -> "sheet_spreadsheet"; case "mindmap" -> "mindmap_map"; case "whiteboard" -> "whiteboard_board"; default -> throw new IllegalArgumentException("Unknown resource"); };
        String column = switch(kind) { case "sheet" -> "sheet_id"; case "mindmap" -> "map_id"; default -> "board_id"; };
        Map<String,Object> resource = row("SELECT creator_id,space_id FROM "+table+" WHERE "+column+"=? AND deleted=0",id);
        if (!Objects.equals(number(resource.get("creator_id")),userId())) requireSpace(number(resource.get("space_id")),write);
    }

    public void requireSheet(Long sheetId, boolean write) {
        Map<String,Object> sheet = row("SELECT spreadsheet_id FROM sheet_sheet WHERE id=?", sheetId);
        requireTypedDocument("sheet",Objects.toString(sheet.get("spreadsheet_id")),write);
    }

    public void requireMeeting(String meetingId, boolean write) {
        Map<String,Object> room = row("SELECT host_id FROM meeting_room WHERE room_id=? AND deleted=0", meetingId);
        if (Objects.equals(number(room.get("host_id")), userId())) return;
        if (write || jdbc.queryForObject("SELECT COUNT(*) FROM meeting_participant WHERE meeting_id=? AND user_id=?", Integer.class, meetingId, userId()) == 0) deny();
    }

    public void requireSession(String sessionId) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM chat_session_member WHERE session_id=? AND user_id=?", Integer.class, sessionId, userId()) == 0) deny();
    }

    public void requireOperation(Long space,Long actor,String operation) {
        if(space==null)return;
        var rows=jdbc.queryForList("SELECT m.role_type,r.permissions_json FROM sys_space_member m JOIN sys_space s ON s.id=m.space_id LEFT JOIN workspace_role r ON r.id=m.custom_role_id AND r.space_id=m.space_id WHERE m.space_id=? AND m.user_id=? AND s.status=1 AND s.deleted=0 AND (m.expires_at IS NULL OR m.expires_at>NOW())",space,actor);
        if(rows.isEmpty())deny();var member=rows.get(0);
        if(Set.of("1","ADMIN").contains(member.get("role_type").toString()))return;
        if(member.get("permissions_json")!=null&&!Boolean.TRUE.equals(com.alibaba.fastjson2.JSON.parseObject(member.get("permissions_json").toString()).getBoolean(operation)))deny();
    }
    public void requireDocumentOperation(String id,String operation){requireDocument(id,false);var doc=row("SELECT space_id,folder_id,creator_id FROM doc_document WHERE doc_id=? AND deleted=0",id);Long space=number(doc.get("space_id"));requireOperation(space,userId(),operation);if(!Objects.equals(number(doc.get("creator_id")),userId()))requireFolderOperation(Objects.toString(doc.get("folder_id"),null),space,userId(),operation);}
    public void requireFolderOperation(String folderId,Long space,Long actor,String operation){
        if(folderId==null||space==null)return;
        var admins=jdbc.queryForList("SELECT user_id FROM sys_space_member WHERE space_id=? AND user_id=? AND role_type IN ('1','ADMIN')",Long.class,space,actor);if(!admins.isEmpty())return;
        var seen=new HashSet<String>();String current=folderId;
        while(current!=null&&!current.equals("0")&&seen.add(current)&&seen.size()<=50){
            var folder=row("SELECT parent_id,space_id,creator_id FROM file_folder WHERE folder_id=? AND deleted=0",current);if(!Objects.equals(number(folder.get("space_id")),space))deny();
            if(Objects.equals(number(folder.get("creator_id")),actor))return;
            var acl=jdbc.queryForList("SELECT user_id,permissions_json FROM folder_permission WHERE folder_id=?",current);
            if(!acl.isEmpty()){var grant=acl.stream().filter(a->Objects.equals(number(a.get("user_id")),actor)).findFirst();if(grant.isEmpty()||!Boolean.TRUE.equals(com.alibaba.fastjson2.JSON.parseObject(grant.get().get("permissions_json").toString()).getBoolean(operation)))deny();return;}
            current=Objects.toString(folder.get("parent_id"),null);
        }
        if(current!=null&&!current.equals("0"))deny();
    }
    public void requireUploadQuota(Long space,long bytes){if(bytes<0)throw new BusinessException(400,"文件大小无效");var s=row("SELECT storage_quota FROM sys_space WHERE id=? FOR UPDATE",space);long used=jdbc.queryForObject("SELECT COALESCE(SUM(file_size),0) FROM file_file WHERE space_id=? AND deleted=0 AND status=1",Long.class,space);if(bytes>((Number)s.get("storage_quota")).longValue()-used)throw new BusinessException(409,"团队存储配额不足");}

    public Map<String,Object> row(String sql, Object id) {
        List<Map<String,Object>> rows = jdbc.queryForList(sql, id);
        if (rows.isEmpty()) throw new BusinessException(ResultCode.NOT_FOUND);
        return rows.get(0);
    }
    private Long number(Object value) { return value == null ? null : ((Number)value).longValue(); }
    private void deny() { throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "没有访问此团队或资源的权限"); }
}
