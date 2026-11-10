package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.UserLoginDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserLoginDeviceMapper extends BaseMapper<UserLoginDevice> {
    @Select("SELECT * FROM user_login_device WHERE user_id = #{userId} ORDER BY last_login_time DESC")
    List<UserLoginDevice> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_login_device WHERE user_id = #{userId} AND is_current = 1")
    List<UserLoginDevice> selectCurrentDevices(@Param("userId") Long userId);
}
