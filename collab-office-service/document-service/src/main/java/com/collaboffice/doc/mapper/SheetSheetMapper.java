package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.SheetSheet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SheetSheetMapper extends BaseMapper<SheetSheet> {

    @Select("SELECT * FROM sheet_sheet WHERE spreadsheet_id = #{spreadsheetId} ORDER BY sort_order ASC, sheet_index ASC, id ASC")
    List<SheetSheet> selectBySpreadsheetId(@Param("spreadsheetId") String spreadsheetId);
}
