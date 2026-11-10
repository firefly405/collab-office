package com.collaboffice.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ResourceAccessInterceptor implements HandlerInterceptor {
    private final WorkspaceAccess access;
    private static final Pattern DOCUMENT = Pattern.compile("/api/v1/document/(\\d+)(?:/.*)?");
    private static final Pattern FILE = Pattern.compile("/api/v1/file/(?:download/|preview/)?(\\d+)(?:/.*)?");
    private static final Pattern TASK = Pattern.compile("/api/v1/task/(\\d+)(?:/.*)?");
    private static final Pattern MEETING = Pattern.compile("/api/v1/meeting/(\\d+)(?:/.*)?");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getHeader("X-Internal-Token") != null) return true;
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/admin/")) access.requireSystemAdmin();
        if (path.contains("/share/") || path.contains("/internal/")) return true;
        boolean write = !"GET".equals(request.getMethod());
        if(write && path.startsWith("/api/v1/space/") && !path.matches("/api/v1/space/\\d+/organization/documents/\\d+")) access.requireSystemAdmin();
        var matcher = DOCUMENT.matcher(path);
        if (matcher.matches()) {
            if(path.endsWith("/export"))access.requireDocumentOperation(matcher.group(1),"download");
            if(path.endsWith("/share")||path.contains("/shares"))access.requireDocumentOperation(matcher.group(1),"share");
            if("DELETE".equals(request.getMethod())&&!path.contains("/comments")&&!path.contains("/shares")&&!path.contains("/members"))access.requireDocumentOperation(matcher.group(1),"delete");
            if(write && path.contains("/comments")) access.requireComment(matcher.group(1));
            else access.requireDocument(matcher.group(1), write && !path.endsWith("/favorite") && !path.endsWith("/export") && !path.endsWith("/duplicate") && !path.endsWith("/pin"));
        }
        matcher = FILE.matcher(path);
        if (matcher.matches()) {access.requireFile(matcher.group(1), write && !path.contains("/preview/") && !path.endsWith("/check-conflict"));if("DELETE".equals(request.getMethod())){var file=access.row("SELECT space_id,folder_id FROM file_file WHERE file_id=? AND deleted=0",matcher.group(1));Long space=file.get("space_id")==null?null:((Number)file.get("space_id")).longValue();access.requireOperation(space,access.userId(),"delete");access.requireFolderOperation(java.util.Objects.toString(file.get("folder_id"),null),space,access.userId(),"delete");}}
        matcher = TASK.matcher(path);
        if (matcher.matches()) access.requireTask(matcher.group(1), write && !path.endsWith("/comment"));
        matcher = MEETING.matcher(path);
        if (matcher.matches()) access.requireMeeting(matcher.group(1), write);
        if (path.startsWith("/api/v1/chat/messages/") || path.startsWith("/api/v1/chat/read/")) access.requireSession(path.substring(path.lastIndexOf('/')+1));
        if (path.equals("/api/v1/chat/send")) access.requireSession(request.getParameter("sessionId"));
        if (path.equals("/api/v1/chat/comments")) {
            String type = request.getParameter("sourceType");
            if ("task".equals(type)) access.requireTask(request.getParameter("sourceId"), write);
            else access.requireDocument(request.getParameter("sourceId"), write);
        }
        if (path.startsWith("/api/v1/collaboration/")) {
            var part = path.substring("/api/v1/collaboration/".length()).split("/")[0];
            if (part.matches("\\d+")) access.requireDocument(part, write && !path.endsWith("/join") && !path.endsWith("/leave"));
        }
        var typed = Pattern.compile("/api/v1/(sheet|mindmap|whiteboard)/(?:manage/)?(\\d+)(?:/.*)?").matcher(path);
        if(typed.matches()) {access.requireTypedDocument(typed.group(1),typed.group(2),write && !path.endsWith("/export"));if(path.endsWith("/export"))access.requireDocumentOperation(typed.group(2),"download");}
        var child = Pattern.compile("/api/v1/(mindmap/node|whiteboard/element)/(\\d+)(?:/.*)?").matcher(path);
        if(child.matches()) {
            boolean mindmap=child.group(1).startsWith("mindmap");
            var row=access.row(mindmap?"SELECT map_id AS resource_id FROM mindmap_node WHERE id=?":"SELECT board_id AS resource_id FROM whiteboard_element WHERE id=?",child.group(2));
            access.requireTypedDocument(mindmap?"mindmap":"whiteboard",row.get("resource_id").toString(),write);
        }
        return true;
    }
}
