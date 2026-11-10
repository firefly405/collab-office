package com.collaboffice.doc.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long commentId;
    private Long userId;
    private String userName;
    private String avatar;
    private String content;
    private String selectionRange;
    private Integer isResolved;
    private LocalDateTime createdTime;
    private List<CommentVO> replies;
}
