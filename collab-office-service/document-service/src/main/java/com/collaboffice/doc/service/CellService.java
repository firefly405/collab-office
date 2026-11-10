package com.collaboffice.doc.service;

import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.vo.SheetDataVO;
import java.util.Map;

public interface CellService {
    void updateCell(Long userId, CellUpdateRequest request);
    void batchUpdate(Long userId, CellBatchUpdateRequest request);
    Map<String, String> getCellsByRange(CellRangeQueryRequest request);
}
