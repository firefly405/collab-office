package com.collaboffice.doc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.doc.entity.DocMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DocMemberMapper extends BaseMapper<DocMember> {

    @Select("""
            SELECT *
            FROM doc_member
            WHERE doc_id = #{docId}
            ORDER BY join_time ASC, id ASC
            """)
    List<DocMember> selectByDocId(@Param("docId") String docId);

    @Select("""
            SELECT *
            FROM doc_member
            WHERE doc_id = #{docId}
              AND is_online = 1
            ORDER BY last_active_time DESC, id ASC
            """)
    List<DocMember> selectOnlineMembers(@Param("docId") String docId);

    @Select("""
            SELECT *
            FROM doc_member
            WHERE doc_id = #{docId}
              AND user_id = #{userId}
            LIMIT 1
            """)
    DocMember selectByDocIdAndUserId(@Param("docId") String docId, @Param("userId") Long userId);
}
