package com.collaboffice.user.service;

import com.collaboffice.user.dto.TokenPairDTO;

import java.util.Map;

/**
 * OAuth2.0 三方登录服务
 * 支持微信(wechat)、企业微信(wecom)等提供商
 */
public interface OAuthService {

    /**
     * 生成 OAuth 登录 URL 并在 Redis 中缓存 state（TTL=5min 防 CSRF）
     * @param provider 提供商标识（wechat / wecom）
     * @return 包含 authorizationUrl 和 state 的 Map
     */
    Map<String, String> initiateOAuth(String provider);

    /**
     * 处理 OAuth 回调：Code->Token->UserInfo->本地账号映射->颁发 JWT
     * @param provider 提供商标识
     * @param code     授权码
     * @param state    防 CSRF state
     * @return 本地 JWT TokenPair
     */
    TokenPairDTO handleCallback(String provider, String code, String state);
}
