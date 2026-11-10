package com.collaboffice.doc.vo;

import lombok.Data;
import java.util.List;

@Data
public class OutlineNodeVO {
    private Integer level;
    private String title;
    private Integer position;
    private List<OutlineNodeVO> children;
}
