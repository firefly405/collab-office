package com.collaboffice.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptCreateRequest {

    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称最长50字")
    private String deptName;

    private Long leaderId;
    private Integer sort;
}
