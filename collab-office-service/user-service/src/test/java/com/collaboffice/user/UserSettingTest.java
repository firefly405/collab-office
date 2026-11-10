package com.collaboffice.user;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.controller.UserSettingController;
import com.collaboffice.user.entity.UserNotificationPref;
import com.collaboffice.user.entity.UserSetting;
import com.collaboffice.user.mapper.UserNotificationPrefMapper;
import com.collaboffice.user.mapper.UserSettingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserSettingTest {

    @Mock
    private UserSettingMapper settingMapper;

    @Mock
    private UserNotificationPrefMapper notificationPrefMapper;

    @Mock
    private com.collaboffice.user.service.PreferenceService preferences;

    @InjectMocks
    private UserSettingController userSettingController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetSettings_DefaultWhenNull() {
        when(settingMapper.selectById(1L)).thenReturn(null);

        CommonResult<Map<String, Object>> result = userSettingController.getSettings(1L);

        assertEquals(200, result.getCode());
        Map<String, Object> data = result.getData();
        assertEquals("zh-CN", data.get("language"));
        assertEquals("Asia/Shanghai", data.get("timezone"));
        assertEquals(true, data.get("emailNotify"));
        assertEquals(true, data.get("pushNotify"));
    }

    @Test
    public void testUpdateSettings_Existing() {
        UserSetting existingSetting = new UserSetting();
        existingSetting.setUserId(1L);
        existingSetting.setLanguage("zh-CN");
        existingSetting.setTimezone("Asia/Shanghai");
        existingSetting.setEmailNotify(1);
        existingSetting.setPushNotify(1);

        UserNotificationPref existingPref = new UserNotificationPref();
        existingPref.setUserId(1L);
        existingPref.setEmailNotify(1);
        existingPref.setPushNotify(1);

        when(settingMapper.selectById(1L)).thenReturn(existingSetting);
        when(notificationPrefMapper.selectById(1L)).thenReturn(existingPref);

        Map<String, Object> body = new HashMap<>();
        body.put("emailNotify", false);
        body.put("pushNotify", true);
        body.put("language", "en-US");

        CommonResult<Void> result = userSettingController.updateSettings(1L, body);

        assertEquals(200, result.getCode());
        verify(settingMapper, times(1)).updateById(any(UserSetting.class));
        verify(notificationPrefMapper, times(1)).updateById(any(UserNotificationPref.class));
    }
}
