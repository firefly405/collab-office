package com.collaboffice.user.vo;

import lombok.Data;

import java.util.List;

@Data
public class DeptTreeNodeVO {

    private Long deptId;
    private String deptName;
    private Long leaderId;
    private String leaderName;
    private Integer sort;
    private List<DeptTreeNodeVO> children;
}
