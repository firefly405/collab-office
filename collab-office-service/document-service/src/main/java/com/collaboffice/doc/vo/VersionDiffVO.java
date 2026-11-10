package com.collaboffice.doc.vo;

import lombok.Data;
import java.util.List;

@Data
public class VersionDiffVO {
    private Long versionFrom;
    private Long versionTo;
    private List<DiffSegment> diffSegments;

    @Data
    public static class DiffSegment {
        private String type; // add/remove/modify
        private String text;
    }
}
