package com.collaboffice.doc.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.collaboffice.doc.entity.DocContent;
import com.collaboffice.doc.mapper.DocContentMapper;
import com.collaboffice.doc.service.OutlineService;
import com.collaboffice.doc.vo.OutlineNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OutlineServiceImpl implements OutlineService {

    private final DocContentMapper docContentMapper;

    @Override
    public List<OutlineNodeVO> generateOutline(String docId) {
        DocContent content = docContentMapper.selectLatestByDocId(docId);
        if (content == null) return Collections.emptyList();

        List<OutlineNodeVO> outline = new ArrayList<>();
        try {
            JSONObject json = JSONObject.parseObject(content.getContent(), JSONObject.class);
            JSONArray blocks = json.getJSONArray("blocks");
            if (blocks != null) {
                Stack<OutlineNodeVO> stack = new Stack<>();
                int position = 0;
                for (int i = 0; i < blocks.size(); i++) {
                    JSONObject block = blocks.getJSONObject(i);
                    String type = block.getString("type");
                    if (type != null && type.matches("h[1-6]")) {
                        int level = Integer.parseInt(type.substring(1));
                        String text = block.getString("text");
                        OutlineNodeVO node = new OutlineNodeVO();
                        node.setLevel(level);
                        node.setTitle(text != null ? text : "");
                        node.setPosition(position++);
                        node.setChildren(new ArrayList<>());

                        // Pop all nodes with level >= current, re-parent the last popped
                        OutlineNodeVO lastPopped = null;
                        while (!stack.isEmpty() && stack.peek().getLevel() >= level) {
                            lastPopped = stack.pop();
                        }
                        if (lastPopped != null) {
                            node.getChildren().add(lastPopped);
                        }
                        if (stack.isEmpty()) {
                            outline.add(node);
                        } else {
                            stack.peek().getChildren().add(node);
                        }
                        stack.push(node);
                    }
                }
            }
        } catch (Exception e) {
            return outline;
        }
        return outline;
    }
}
