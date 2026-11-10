package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.SysSpace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysSpaceMapper extends BaseMapper<SysSpace> {

    @Select("SELECT * FROM sys_space WHERE owner_id = #{ownerId} AND deleted = 0 ORDER BY create_time DESC")
    List<SysSpace> selectByOwnerId(@Param("ownerId") Long ownerId);
}
