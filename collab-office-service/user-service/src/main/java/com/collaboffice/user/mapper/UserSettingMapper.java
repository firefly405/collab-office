package com.collaboffice.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collaboffice.user.entity.UserSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSettingMapper extends BaseMapper<UserSetting> {
}
