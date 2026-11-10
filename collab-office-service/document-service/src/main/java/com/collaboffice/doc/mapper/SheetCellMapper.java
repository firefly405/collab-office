package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.SheetCell;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Lang;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import java.util.List;

@Mapper
public interface SheetCellMapper extends BaseMapper<SheetCell> {

    @Select("""
            SELECT *
            FROM sheet_cell
            WHERE sheet_id = #{sheetId}
              AND row_index BETWEEN #{startRow} AND #{endRow}
              AND col_index BETWEEN #{startCol} AND #{endCol}
            ORDER BY row_index ASC, col_index ASC
            """)
    List<SheetCell> selectBySheetIdAndRange(@Param("sheetId") Long sheetId,
                                            @Param("startRow") int startRow,
                                            @Param("startCol") int startCol,
                                            @Param("endRow") int endRow,
                                            @Param("endCol") int endCol);

    @Insert("""
            <script>
            INSERT INTO sheet_cell
                (sheet_id, cell_key, row_index, col_index, content, format_json, formula, result_value)
            VALUES
            <foreach collection="list" item="item" separator=",">
                (#{item.sheetId}, #{item.cellKey}, #{item.rowIndex}, #{item.colIndex},
                 #{item.content}, #{item.formatJson}, #{item.formula}, #{item.resultValue})
            </foreach>
            ON DUPLICATE KEY UPDATE
                content = VALUES(content),
                format_json = VALUES(format_json),
                formula = VALUES(formula),
                result_value = VALUES(result_value),
                update_time = CURRENT_TIMESTAMP
            </script>
            """)
    @Lang(XMLLanguageDriver.class)
    void batchInsertOrUpdate(@Param("list") List<SheetCell> cells);
}
