package com.collaboffice.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_folder")
public class FileFolder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String folderId;
    private Long parentId;
    private String folderName;
    private Long spaceId;
    private Long creatorId;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
