package com.collaboffice.doc.service.impl;

import com.collaboffice.common.exception.BusinessException;
import com.collaboffice.common.result.ResultCode;
import com.collaboffice.doc.dto.CommentCreateRequest;
import com.collaboffice.doc.entity.DocComment;
import com.collaboffice.doc.mapper.DocCommentMapper;
import com.collaboffice.doc.service.CommentService;
import com.collaboffice.doc.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final DocCommentMapper docCommentMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;

    @Override
    public CommentVO create(CommentCreateRequest request, Long userId, String userName) {
        access.requireComment(request.getDocId());
        if(request.getContent()==null||request.getContent().isBlank()||request.getContent().length()>4000) throw new BusinessException(400,"评论为1至4000字");
        if(request.getSelectionRange()!=null) {
            try {
                var anchor=com.alibaba.fastjson2.JSON.parseObject(request.getSelectionRange());
                if(request.getSelectionRange().length()>500||anchor.getIntValue("index")<0||anchor.getIntValue("length")<=0||anchor.getString("text")==null) throw new IllegalArgumentException();
            } catch(Exception e) {throw new BusinessException(400,"批注选区无效");}
        }
        var mentions=new java.util.LinkedHashSet<Long>();
        if(request.getMentionUsers()!=null) {
            if(request.getMentionUsers().size()>50) throw new BusinessException(400,"单次最多提及50人");
            mentions.addAll(request.getMentionUsers());
            for(Long recipient:mentions) access.requireDocument(request.getDocId(),recipient,false);
        }
        if(request.getParentId()!=null && !findById(request.getParentId()).getDocId().equals(request.getDocId())) throw new BusinessException(400,"回复必须属于同一文档");
        DocComment comment = new DocComment();
        comment.setDocId(request.getDocId());
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(request.getContent());
        comment.setSelectionRange(request.getSelectionRange());
        comment.setParentId(request.getParentId());
        comment.setIsResolved(0);
        docCommentMapper.insert(comment);
        var recipients=new java.util.LinkedHashSet<Long>(mentions);
        recipients.add(jdbc.queryForObject("SELECT creator_id FROM doc_document WHERE doc_id=?",Long.class,request.getDocId()));
        if(request.getParentId()!=null)recipients.add(findById(request.getParentId()).getUserId());
        for(Long recipient:recipients) {
            if(recipient==null||recipient.equals(userId))continue;
            try {rabbit.convertAndSend("notification-exchange","notification.mention",Map.of("userId",recipient,"title",mentions.contains(recipient)?"文档评论提及你":"文档有新评论","content",userName+"："+request.getContent(),"relatedId",request.getDocId()));}
            catch(Exception e){log.error("Comment notification failed: commentId={}",comment.getId(),e);}
        }
        return toVO(comment);
    }

    @Override
    public CommentVO reply(Long commentId, String content, Long userId, String userName) {
        DocComment parent = findById(commentId);
        var request=new CommentCreateRequest();request.setDocId(parent.getDocId());request.setParentId(commentId);request.setContent(content);
        return create(request,userId,userName);
    }

    @Override
    public List<CommentVO> listByDocId(String docId, String filter) {
        List<DocComment> allComments = docCommentMapper.selectByDocId(docId);
        access.requireDocument(docId,false);
        return buildTree(allComments).stream().filter(c->"all".equals(filter)||("resolved".equals(filter)?Integer.valueOf(1).equals(c.getIsResolved()):!Integer.valueOf(1).equals(c.getIsResolved()))).toList();
    }

    @Override
    public void resolve(Long commentId) {
        DocComment comment = findById(commentId);
        access.requireDocument(comment.getDocId(),true);
        comment.setIsResolved(1);
        comment.setResolvedTime(LocalDateTime.now());
        docCommentMapper.updateById(comment);
    }

    @Override
    public void delete(Long commentId) {
        var comment=findById(commentId);access.requireComment(comment.getDocId());
        if(!comment.getUserId().equals(access.userId()))access.requireDocument(comment.getDocId(),true);
        var children=docCommentMapper.selectByDocId(comment.getDocId()).stream().filter(c->commentId.equals(c.getParentId())).toList();
        if(!children.isEmpty())throw new BusinessException(409,"含回复的评论请使用解决功能保留讨论");
        docCommentMapper.deleteById(commentId);
    }

    private List<CommentVO> buildTree(List<DocComment> comments) {
        Map<Long, CommentVO> voMap = new LinkedHashMap<>();
        List<CommentVO> roots = new ArrayList<>();

        for (DocComment c : comments) {
            CommentVO vo = toVO(c);
            voMap.put(c.getId(), vo);
        }

        for (DocComment c : comments) {
            CommentVO vo = voMap.get(c.getId());
            if (c.getParentId() != null && voMap.containsKey(c.getParentId())) {
                voMap.get(c.getParentId()).getReplies().add(vo);
            } else {
                roots.add(vo);
            }
        }

        return roots;
    }

    private CommentVO toVO(DocComment c) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(c.getId());
        vo.setUserId(c.getUserId());
        vo.setUserName(c.getUserName());
        vo.setContent(c.getContent());
        vo.setSelectionRange(c.getSelectionRange());
        vo.setIsResolved(c.getIsResolved());
        vo.setCreatedTime(c.getCreateTime());
        vo.setReplies(new ArrayList<>());
        return vo;
    }

    private DocComment findById(Long id) {
        DocComment c = docCommentMapper.selectById(id);
        if (c == null) throw new BusinessException(ResultCode.NOT_FOUND);
        return c;
    }
}
