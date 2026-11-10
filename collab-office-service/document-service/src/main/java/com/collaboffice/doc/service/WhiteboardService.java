package com.collaboffice.doc.service;

import java.util.List;
import java.util.Map;

public interface WhiteboardService {
    String createBoard(Long userId, String title, int width, int height, Long spaceId);
    Map<String, Object> getBoardData(String boardId);
    void addElement(String boardId, String elementType, String positionJson, String styleJson, String content);
    void updateElement(Long elementId, String positionJson, String styleJson, String content);
    void deleteElement(Long elementId);
    List<Map<String, Object>> getElements(String boardId);
    void clearBoard(String boardId);
}
