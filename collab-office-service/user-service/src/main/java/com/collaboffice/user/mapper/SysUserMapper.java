package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    Optional<SysUser> selectByPhone(@Param("phone") String phone);

    Optional<SysUser> selectByEmail(@Param("email") String email);

    Optional<SysUser> selectByUsername(@Param("username") String username);
}
