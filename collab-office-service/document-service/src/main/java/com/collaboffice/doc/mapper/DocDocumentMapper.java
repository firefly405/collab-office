package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collaboffice.doc.dto.DocumentListRequest;
import com.collaboffice.doc.entity.DocDocument;
import com.collaboffice.doc.vo.DocumentDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DocDocumentMapper extends BaseMapper<DocDocument> {

    Page<DocumentDetailVO> selectListWithFilters(Page<DocumentDetailVO> page, @Param("request") DocumentListRequest request);

    List<DocDocument> selectBySpaceId(@Param("spaceId") Long spaceId);

    Long countUserDocs(@Param("userId") Long userId);
}
