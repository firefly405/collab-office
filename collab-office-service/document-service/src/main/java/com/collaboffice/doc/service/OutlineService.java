package com.collaboffice.doc.service;

import com.collaboffice.doc.vo.OutlineNodeVO;
import java.util.List;

public interface OutlineService {
    List<OutlineNodeVO> generateOutline(String docId);
}
