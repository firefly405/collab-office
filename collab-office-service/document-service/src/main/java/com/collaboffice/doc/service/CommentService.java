package com.collaboffice.doc.service;

import com.collaboffice.doc.dto.CommentCreateRequest;
import com.collaboffice.doc.vo.CommentVO;
import java.util.List;

public interface CommentService {
    CommentVO create(CommentCreateRequest request, Long userId, String userName);
    CommentVO reply(Long commentId, String content, Long userId, String userName);
    List<CommentVO> listByDocId(String docId, String filter);
    void resolve(Long commentId);
    void delete(Long commentId);
}
