package com.collaboffice.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_file")
public class FileFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String fileUrl;
    private String thumbnailUrl;
    private Long folderId;
    private Long spaceId;
    private Long uploaderId;
    private String uploaderName;
    private Integer version;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
