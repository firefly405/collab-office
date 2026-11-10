package com.collaboffice.doc.controller;
import com.collaboffice.common.result.CommonResult;
import com.collaboffice.common.security.WorkspaceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/document")
@RequiredArgsConstructor
public class DocumentOrganizationController {
    private final WorkspaceAccess access;
    private final JdbcTemplate jdbc;
    @PutMapping("/{id}/pin")
    public CommonResult<Void> pin(@PathVariable String id,@RequestParam boolean value) {
        access.requireDocument(id,false);
        if(value) jdbc.update("INSERT IGNORE INTO doc_user_pin(user_id,doc_id) VALUES(?,?)",access.userId(),id);
        else jdbc.update("DELETE FROM doc_user_pin WHERE user_id=? AND doc_id=?",access.userId(),id);
        return CommonResult.success(null);
    }
    @GetMapping("/folders")
    public CommonResult<List<Map<String,Object>>> folders() {
        return CommonResult.success(jdbc.queryForList("SELECT folder_id AS folderId,folder_name AS name,parent_id AS parentId FROM file_folder WHERE space_id=? AND deleted=0 ORDER BY folder_name",access.currentSpace()));
    }
}
