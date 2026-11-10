package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("SELECT * FROM sys_user_role WHERE user_id = #{userId} ORDER BY id")
    List<SysUserRole> selectByUserId(@Param("userId") Long userId);
}
