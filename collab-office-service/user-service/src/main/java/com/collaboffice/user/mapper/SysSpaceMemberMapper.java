package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.SysSpaceMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysSpaceMemberMapper extends BaseMapper<SysSpaceMember> {

    @Select("SELECT * FROM sys_space_member WHERE space_id = #{spaceId} ORDER BY join_time")
    List<SysSpaceMember> selectBySpaceId(@Param("spaceId") Long spaceId);

    @Select("SELECT * FROM sys_space_member WHERE user_id = #{userId} ORDER BY join_time DESC")
    List<SysSpaceMember> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM sys_space_member WHERE space_id = #{spaceId}")
    Integer countMembers(@Param("spaceId") Long spaceId);
}
