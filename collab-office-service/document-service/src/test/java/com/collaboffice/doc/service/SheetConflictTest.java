package com.collaboffice.doc.service;
import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.security.WorkspaceAccess;
import com.collaboffice.doc.dto.*;
import com.collaboffice.doc.entity.*;
import com.collaboffice.doc.mapper.*;
import com.collaboffice.doc.service.impl.CellServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class SheetConflictTest {
    @Mock SheetCellMapper cells;@Mock SheetSheetMapper sheets;@Mock WorkspaceAccess access;@Mock JdbcTemplate jdbc;@Spy SheetWorkbookService engine=new SheetWorkbookService();@InjectMocks CellServiceImpl service;
    @Test void staleCellBatchRejectedBeforeAnyPersistence() {
        var sheet=new SheetSheet();sheet.setId(1L);sheet.setSpreadsheetId("900");when(sheets.selectById(1L)).thenReturn(sheet);
        var cell=new SheetCell();cell.setCellKey("A1");cell.setContent("另一人的修改");when(cells.selectList(any())).thenReturn(List.of(cell));
        var input=new CellUpdateRequest();input.setSheetId(1L);input.setCellKey("A1");input.setContent("我的修改");input.setCheckConflict(true);input.setExpectedContent("旧正文");
        var batch=new CellBatchUpdateRequest();batch.setSheetId(1L);batch.setCells(List.of(input));assertThrows(BusinessException.class,()->service.batchUpdate(1L,batch));verify(cells,never()).batchInsertOrUpdate(any());
    }
}
