package com.collaboffice.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.file.entity.KnowledgeItem;
import com.collaboffice.file.mapper.KnowledgeItemMapper;
import com.collaboffice.file.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeItemMapper knowledgeItemMapper;
    private final com.collaboffice.common.security.WorkspaceAccess access;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Override
    public Page<KnowledgeItem> getKnowledgeList(int pageNum, int pageSize) {
        Page<KnowledgeItem> page = new Page<>(Math.max(1,pageNum), Math.max(1,Math.min(100,pageSize)));
        knowledgeItemMapper.selectPage(page,
            new LambdaQueryWrapper<KnowledgeItem>()
                .eq(KnowledgeItem::getStatus, 1)
                .eq(KnowledgeItem::getSpaceId, access.currentSpace())
                .orderByDesc(KnowledgeItem::getCreateTime)
        );
        return page;
    }

    @Override
    @Transactional
    public KnowledgeItem getKnowledgeDetail(String itemId) {
        KnowledgeItem item = knowledgeItemMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeItem>().eq(KnowledgeItem::getKnowledgeId, itemId)
        );
        if (item == null) throw new com.collaboffice.common.exception.BusinessException(404,"知识条目不存在");
        access.requireSpace(item.getSpaceId(), false);
        if (item != null) {
            Integer viewCount = item.getViewCount() != null ? item.getViewCount() : 0;
            item.setViewCount(viewCount + 1);
            knowledgeItemMapper.updateById(item);
        }
        return item;
    }

    @Override
    @Transactional
    public String createKnowledge(KnowledgeItem item, Long userId) {
        validate(item);
        item.setId(null);
        item.setSpaceId(access.requireSpace(access.currentSpace(),true));
        validateCategory(item.getCategoryId(),item.getSpaceId());
        item.setKnowledgeId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        item.setContributorId(userId);
        item.setContributorName(jdbc.queryForObject("SELECT COALESCE(real_name,username) FROM sys_user WHERE id=?",String.class,userId));
        item.setDeleted(0);
        item.setReviewerId(null);
        item.setViewCount(0);
        item.setStarCount(0);
        item.setStatus(1); // default as published
        item.setCreateTime(LocalDateTime.now());
        knowledgeItemMapper.insert(item);
        return item.getKnowledgeId();
    }

    @Override
    @Transactional
    public void updateKnowledge(String itemId, KnowledgeItem item) {
        KnowledgeItem existing = knowledgeItemMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeItem>().eq(KnowledgeItem::getKnowledgeId, itemId)
        );
        requireWrite(existing);
        validate(item); validateCategory(item.getCategoryId(),existing.getSpaceId());
        existing.setTitle(item.getTitle()); existing.setContent(item.getContent());
        existing.setTagsJson(item.getTagsJson()); existing.setCategoryId(item.getCategoryId());
        existing.setUpdateTime(LocalDateTime.now()); knowledgeItemMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteKnowledge(String itemId) {
        KnowledgeItem existing = knowledgeItemMapper.selectOne(
            new LambdaQueryWrapper<KnowledgeItem>().eq(KnowledgeItem::getKnowledgeId, itemId)
        );
        requireWrite(existing);
        knowledgeItemMapper.deleteById(existing.getId());
    }

    private void requireWrite(KnowledgeItem item) {
        if(item==null) throw new com.collaboffice.common.exception.BusinessException(404,"知识条目不存在");
        access.requireSpace(item.getSpaceId(),true);
        if(!java.util.Objects.equals(item.getContributorId(),access.userId())&&!access.isAdmin(item.getSpaceId()))
            throw new com.collaboffice.common.exception.BusinessException(403,"只有贡献者或管理员可编辑");
    }
    private void validateCategory(Long category,Long space) {
        if(category!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_category WHERE id=? AND space_id=?",Integer.class,category,space)==0)
            throw new com.collaboffice.common.exception.BusinessException(400,"分类不属于当前团队");
    }
    private void validate(KnowledgeItem item) {
        if(item.getTitle()==null||item.getTitle().isBlank()||item.getTitle().length()>200||item.getContent()==null||item.getContent().length()>1000000)
            throw new com.collaboffice.common.exception.BusinessException(400,"知识标题或正文无效");
        if(item.getTagsJson()!=null) try {
            var tags=new com.fasterxml.jackson.databind.ObjectMapper().readTree(item.getTagsJson());
            if(!tags.isArray()||tags.size()>20) throw new IllegalArgumentException();
            for(var tag:tags) if(!tag.isTextual()||tag.asText().length()>50) throw new IllegalArgumentException();
        } catch(Exception e) { throw new com.collaboffice.common.exception.BusinessException(400,"标签格式无效"); }
    }
}
