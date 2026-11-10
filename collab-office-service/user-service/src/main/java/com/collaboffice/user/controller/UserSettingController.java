package com.collaboffice.user.controller;

import com.collaboffice.common.result.CommonResult;
import com.collaboffice.user.entity.UserSetting;
import com.collaboffice.user.entity.UserNotificationPref;
import com.collaboffice.user.mapper.UserSettingMapper;
import com.collaboffice.user.mapper.UserNotificationPrefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingMapper settingMapper;
    private final UserNotificationPrefMapper notificationPrefMapper;
    private final com.collaboffice.user.service.PreferenceService preferences;

    @GetMapping("/settings")
    public CommonResult<Map<String, Object>> getSettings(@RequestHeader("X-User-Id") Long userId) {
        UserSetting setting = settingMapper.selectById(userId);
        if (setting == null) {
            // Return defaults
            Map<String, Object> defaults = new LinkedHashMap<>();
            defaults.put("language", "zh-CN");
            defaults.put("timezone", "Asia/Shanghai");
            defaults.put("emailNotify", true);
            defaults.put("pushNotify", true);
            defaults.put("privacyLevel", 1);
            defaults.put("preferences",preferences.get(userId));
            return CommonResult.success(defaults);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("language", setting.getLanguage());
        result.put("timezone", setting.getTimezone());
        result.put("emailNotify", setting.getEmailNotify() == 1);
        result.put("pushNotify", setting.getPushNotify() == 1);
        result.put("privacyLevel", setting.getPrivacyLevel());
        result.put("preferences",preferences.get(userId));
        return CommonResult.success(result);
    }

    @PutMapping("/settings")
    @org.springframework.transaction.annotation.Transactional
    public CommonResult<Void> updateSettings(@RequestHeader("X-User-Id") Long userId,
                                              @RequestBody Map<String, Object> body) {
        if(body.containsKey("language")&&!Set.of("zh-CN","en-US").contains(body.get("language")))throw new com.collaboffice.common.exception.BusinessException(400,"不支持的语言");
        if(body.containsKey("timezone"))try{java.time.ZoneId.of(Objects.toString(body.get("timezone")));}catch(Exception e){throw new com.collaboffice.common.exception.BusinessException(400,"时区无效");}
        UserSetting setting = settingMapper.selectById(userId);
        boolean isNew = (setting == null);
        if (isNew) {
            setting = new UserSetting();
            setting.setUserId(userId);
            setting.setLanguage("zh-CN");
            setting.setTimezone("Asia/Shanghai");
            setting.setEmailNotify(1);
            setting.setPushNotify(1);
            setting.setPrivacyLevel(1);
        }
        if (body.containsKey("language")) setting.setLanguage((String) body.get("language"));
        if (body.containsKey("timezone")) setting.setTimezone((String) body.get("timezone"));
        if (body.containsKey("emailNotify")) setting.setEmailNotify(Boolean.TRUE.equals(body.get("emailNotify")) ? 1 : 0);
        if (body.containsKey("pushNotify")) setting.setPushNotify(Boolean.TRUE.equals(body.get("pushNotify")) ? 1 : 0);
        if (body.containsKey("privacyLevel") && body.get("privacyLevel") instanceof Number)
            setting.setPrivacyLevel(((Number) body.get("privacyLevel")).intValue());
        if (isNew) {
            settingMapper.insert(setting);
        } else {
            settingMapper.updateById(setting);
        }

        // Update user_notification_pref too
        UserNotificationPref pref = notificationPrefMapper.selectById(userId);
        boolean isPrefNew = (pref == null);
        if (isPrefNew) {
            pref = new UserNotificationPref();
            pref.setUserId(userId);
            pref.setEmailNotify(1);
            pref.setPushNotify(1);
        }
        if (body.containsKey("emailNotify")) pref.setEmailNotify(Boolean.TRUE.equals(body.get("emailNotify")) ? 1 : 0);
        if (body.containsKey("pushNotify")) pref.setPushNotify(Boolean.TRUE.equals(body.get("pushNotify")) ? 1 : 0);
        if (isPrefNew) {
            notificationPrefMapper.insert(pref);
        } else {
            notificationPrefMapper.updateById(pref);
        }

        if(body.containsKey("preferences")){
            if(!(body.get("preferences") instanceof Map<?,?> values))throw new com.collaboffice.common.exception.BusinessException(400,"偏好格式无效");
            preferences.save(userId,(Map)values);
        }
        return CommonResult.success(null);
    }
}
