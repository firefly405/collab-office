package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.SheetChart;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SheetChartMapper extends BaseMapper<SheetChart> {
    @Select("SELECT * FROM sheet_chart WHERE sheet_id = #{sheetId}")
    List<SheetChart> selectBySheetId(@Param("sheetId") String sheetId);
}
