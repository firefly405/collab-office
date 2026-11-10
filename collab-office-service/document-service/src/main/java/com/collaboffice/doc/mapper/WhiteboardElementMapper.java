package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.WhiteboardElement;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface WhiteboardElementMapper extends BaseMapper<WhiteboardElement> {
    @Select("SELECT * FROM whiteboard_element WHERE board_id = #{boardId} ORDER BY z_index ASC, id ASC")
    List<WhiteboardElement> selectByBoardId(@Param("boardId") String boardId);

    @Delete("DELETE FROM whiteboard_element WHERE board_id = #{boardId}")
    void deleteByBoardId(@Param("boardId") String boardId);
}
