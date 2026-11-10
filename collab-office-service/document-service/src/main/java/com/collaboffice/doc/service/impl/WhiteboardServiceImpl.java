package com.collaboffice.doc.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.doc.entity.WhiteboardBoard;
import com.collaboffice.doc.entity.WhiteboardElement;
import com.collaboffice.doc.mapper.WhiteboardBoardMapper;
import com.collaboffice.doc.mapper.WhiteboardElementMapper;
import com.collaboffice.doc.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhiteboardServiceImpl implements WhiteboardService {

    private final WhiteboardBoardMapper boardMapper;
    private final WhiteboardElementMapper elementMapper;

    @Override
    @Transactional
    public String createBoard(Long userId, String title, int width, int height, Long spaceId) {
        String boardId = SnowflakeIdGenerator.nextIdStr();
        WhiteboardBoard board = new WhiteboardBoard();
        board.setBoardId(boardId);
        board.setTitle(title);
        board.setWidth(width > 0 ? width : 1920);
        board.setHeight(height > 0 ? height : 1080);
        board.setBackgroundColor("#FFFFFF");
        board.setSpaceId(spaceId);
        board.setCreatorId(userId);
        board.setStatus(1);
        boardMapper.insert(board);
        return boardId;
    }

    @Override
    public Map<String, Object> getBoardData(String boardId) {
        WhiteboardBoard board = boardMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WhiteboardBoard>()
                        .eq(WhiteboardBoard::getBoardId, boardId));
        if (board == null) return Collections.emptyMap();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("boardId", board.getBoardId());
        data.put("title", board.getTitle());
        data.put("width", board.getWidth());
        data.put("height", board.getHeight());
        data.put("backgroundColor", board.getBackgroundColor());
        data.put("elements", getElements(boardId));
        return data;
    }

    @Override
    @Transactional
    public void addElement(String boardId, String type, String positionJson, String styleJson, String content) {
        WhiteboardElement element = new WhiteboardElement();
        element.setBoardId(boardId);
        element.setElementType(type);
        element.setPositionJson(positionJson);
        element.setStyleJson(styleJson);
        element.setContent(content);
        elementMapper.insert(element);
    }

    @Override
    @Transactional
    public void updateElement(Long elementId, String positionJson, String styleJson, String content) {
        WhiteboardElement element = elementMapper.selectById(elementId);
        if (element == null) return;
        if (positionJson != null) element.setPositionJson(positionJson);
        if (styleJson != null) element.setStyleJson(styleJson);
        if (content != null) element.setContent(content);
        elementMapper.updateById(element);
    }

    @Override
    @Transactional
    public void deleteElement(Long elementId) {
        elementMapper.deleteById(elementId);
    }

    @Override
    public List<Map<String, Object>> getElements(String boardId) {
        return elementMapper.selectByBoardId(boardId).stream().map(element -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", element.getId());
            data.put("type", element.getElementType());
            data.put("position", element.getPositionJson());
            data.put("style", element.getStyleJson());
            data.put("content", element.getContent());
            return data;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearBoard(String boardId) {
        elementMapper.deleteByBoardId(boardId);
    }
}
