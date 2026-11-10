package com.collaboffice.user.service.impl;

import com.collaboffice.common.util.SnowflakeIdGenerator;
import com.collaboffice.user.dto.TokenPairDTO;
import com.collaboffice.user.entity.SysUser;
import com.collaboffice.user.mapper.SysUserMapper;
import com.collaboffice.user.service.OAuthService;
import com.collaboffice.user.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OAuth2.0 三方登录实现（手动 Code->Token->UserInfo 三步流程）
 * 支持微信公众号 / 企业微信
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final SysUserMapper userMapper;
    private final TokenService tokenService;
    private final StringRedisTemplate redisTemplate;
    private final WebClient.Builder webClientBuilder;

    private static final String OAUTH_STATE_PREFIX = "oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(5);

    // OAuth 配置（实际项目从 application.yml 注入）
    @Value("${oauth.wechat.app-id:wx_placeholder_app_id}")
    private String wechatAppId;

    @Value("${oauth.wechat.app-secret:wx_placeholder_app_secret}")
    private String wechatAppSecret;

    @Value("${oauth.wechat.redirect-uri:http://localhost:8080/api/v1/auth/oauth/wechat/callback}")
    private String wechatRedirectUri;

    @Value("${oauth.wecom.corp-id:wecom_placeholder_corp_id}")
    private String wecomCorpId;

    @Value("${oauth.wecom.corp-secret:wecom_placeholder_corp_secret}")
    private String wecomCorpSecret;

    @Value("${oauth.wecom.redirect-uri:http://localhost:8080/api/v1/auth/oauth/wecom/callback}")
    private String wecomRedirectUri;

    // 钉钉配置
    @Value("${oauth.dingtalk.app-id:dingtalk_placeholder_app_id}")
    private String dingtalkAppId;

    @Value("${oauth.dingtalk.app-secret:dingtalk_placeholder_app_secret}")
    private String dingtalkAppSecret;

    @Value("${oauth.dingtalk.redirect-uri:http://localhost:8080/api/v1/auth/oauth/dingtalk/callback}")
    private String dingtalkRedirectUri;

    // 飞书配置
    @Value("${oauth.feishu.app-id:feishu_placeholder_app_id}")
    private String feishuAppId;

    @Value("${oauth.feishu.app-secret:feishu_placeholder_app_secret}")
    private String feishuAppSecret;

    @Value("${oauth.feishu.redirect-uri:http://localhost:8080/api/v1/auth/oauth/feishu/callback}")
    private String feishuRedirectUri;

    @Override
    public Map<String, String> initiateOAuth(String provider) {
        // 1. 生成 state 并写入 Redis（5min TTL 防 CSRF）
        String state = UUID.randomUUID().toString().replace("-", "");
        String redisKey = OAUTH_STATE_PREFIX + state;
        redisTemplate.opsForValue().set(redisKey, provider, STATE_TTL);
        log.info("OAuth state cached: provider={}, state={}", provider, state);

        // 2. 构造授权 URL
        String authorizationUrl = buildAuthorizationUrl(provider, state);

        Map<String, String> result = new HashMap<>();
        result.put("authorizationUrl", authorizationUrl);
        result.put("state", state);
        return result;
    }

    @Override
    public TokenPairDTO handleCallback(String provider, String code, String state) {
        // 1. 验证 state 防 CSRF
        String redisKey = OAUTH_STATE_PREFIX + state;
        String cachedProvider = redisTemplate.opsForValue().get(redisKey);
        if (cachedProvider == null) {
            throw new RuntimeException("OAuth state 已过期或无效，请重新登录");
        }
        if (!provider.equals(cachedProvider)) {
            throw new RuntimeException("OAuth state provider 不匹配，疑似 CSRF 攻击");
        }
        // 消费 state（一次性）
        redisTemplate.delete(redisKey);

        // 2. 用 code 换取 access_token + openId
        Map<String, Object> tokenInfo = exchangeCodeForToken(provider, code);
        String openId = (String) tokenInfo.get("openid");
        String accessToken = (String) tokenInfo.get("access_token");
        log.info("OAuth token exchanged: provider={}, openId={}", provider, openId);

        // 3. 获取用户信息
        Map<String, Object> userInfo = fetchUserInfo(provider, accessToken, openId);
        String nickname = (String) userInfo.getOrDefault("nickname", "第三方用户");
        String avatarUrl = (String) userInfo.getOrDefault("headimgurl", "");

        // 4. 本地账号映射或自动注册
        SysUser localUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getOauthProvider, provider)
                        .eq(SysUser::getOauthOpenId, openId)
                        .last("limit 1")
        );

        if (localUser == null) {
            // 自动注册
            localUser = new SysUser();
            localUser.setUsername("oauth_" + provider + "_" + openId.substring(0, Math.min(8, openId.length())));
            localUser.setRealName(nickname);
            localUser.setAvatar(avatarUrl);
            localUser.setOauthProvider(provider);
            localUser.setOauthOpenId(openId);
            localUser.setStatus(1);
            localUser.setMfaEnabled(0);
            // OAuth 用户无密码登录，设置随机密码占位
            localUser.setPassword(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class
                    .getName() + "_OAUTH_NO_PASSWORD");
            localUser.setCreateTime(LocalDateTime.now());
            userMapper.insert(localUser);
            log.info("Auto-registered OAuth user: provider={}, openId={}, userId={}", provider, openId, localUser.getId());
        } else {
            // 更新头像和昵称
            localUser.setAvatar(avatarUrl);
            localUser.setRealName(nickname);
            localUser.setLastLoginTime(LocalDateTime.now());
            userMapper.updateById(localUser);
        }

        // 5. 颁发 JWT
        return tokenService.generateTokenPair(localUser.getId());
    }

    private String buildAuthorizationUrl(String provider, String state) {
        return switch (provider.toLowerCase()) {
            case "wechat" -> String.format(
                    "https://open.weixin.qq.com/connect/qrconnect?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_login&state=%s#wechat_redirect",
                    wechatAppId, java.net.URLEncoder.encode(wechatRedirectUri, java.nio.charset.StandardCharsets.UTF_8), state
            );
            case "wecom" -> String.format(
                    "https://login.work.weixin.qq.com/wwlogin/sso/login?login_type=CorpApp&appid=%s&redirect_uri=%s&state=%s",
                    wecomCorpId, java.net.URLEncoder.encode(wecomRedirectUri, java.nio.charset.StandardCharsets.UTF_8), state
            );
            case "dingtalk" -> String.format(
                    "https://login.dingtalk.com/oauth2/auth?redirect_uri=%s&response_type=code&client_id=%s&scope=openid&state=%s&prompt=consent",
                    java.net.URLEncoder.encode(dingtalkRedirectUri, java.nio.charset.StandardCharsets.UTF_8), dingtalkAppId, state
            );
            case "feishu" -> String.format(
                    "https://open.feishu.cn/open-apis/authen/v1/authorize?app_id=%s&redirect_uri=%s&state=%s",
                    feishuAppId, java.net.URLEncoder.encode(feishuRedirectUri, java.nio.charset.StandardCharsets.UTF_8), state
            );
            default -> throw new RuntimeException("不支持的 OAuth 提供商: " + provider);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchangeCodeForToken(String provider, String code) {
        try {
            String tokenUrl = buildTokenUrl(provider, code);
            WebClient webClient = webClientBuilder.build();
            String response;
            if ("dingtalk".equalsIgnoreCase(provider)) {
                Map<String, Object> body = Map.of("clientId", dingtalkAppId,
                        "clientSecret", dingtalkAppSecret, "code", code, "grantType", "authorization_code");
                response = webClient.post().uri(tokenUrl)
                        .bodyValue(body).retrieve().bodyToMono(String.class).block();
            } else if ("feishu".equalsIgnoreCase(provider)) {
                Map<String, Object> body = Map.of("app_id", feishuAppId,
                        "app_secret", feishuAppSecret, "code", code, "grant_type", "authorization_code");
                response = webClient.post().uri(tokenUrl)
                        .bodyValue(body).retrieve().bodyToMono(String.class).block();
            } else {
                response = webClient.get().uri(tokenUrl).retrieve().bodyToMono(String.class).block();
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response, Map.class);
        } catch (Exception e) {
            log.error("OAuth token exchange failed: provider={}, error={}", provider, e.getMessage());
            throw new RuntimeException("OAuth 授权码换取 Token 失败: " + e.getMessage());
        }
    }

    private String buildTokenUrl(String provider, String code) {
        return switch (provider.toLowerCase()) {
            case "wechat" -> String.format(
                    "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code",
                    wechatAppId, wechatAppSecret, code
            );
            case "wecom" -> String.format(
                    "https://qyapi.weixin.qq.com/cgi-bin/user/getuserinfo?access_token=%s&code=%s",
                    wecomCorpSecret, code
            );
            case "dingtalk" -> String.format(
                    "https://api.dingtalk.com/v1.0/oauth2/userAccessToken",
                    code
            );
            case "feishu" -> "https://open.feishu.cn/open-apis/authen/v1/access_token";
            default -> throw new RuntimeException("不支持的 OAuth 提供商: " + provider);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(String provider, String accessToken, String openId) {
        try {
            WebClient webClient = webClientBuilder.build();
            String response;
            if ("dingtalk".equalsIgnoreCase(provider)) {
                response = webClient.get()
                        .uri("https://api.dingtalk.com/v1.0/contact/users/" + openId)
                        .header("x-acs-dingtalk-access-token", accessToken)
                        .retrieve().bodyToMono(String.class).block();
            } else if ("feishu".equalsIgnoreCase(provider)) {
                response = webClient.get()
                        .uri("https://open.feishu.cn/open-apis/authen/v1/user_info")
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve().bodyToMono(String.class).block();
            } else if ("wechat".equalsIgnoreCase(provider)) {
                String url = String.format(
                        "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN",
                        accessToken, openId);
                response = webClient.get().uri(url).retrieve().bodyToMono(String.class).block();
            } else {
                Map<String, Object> result = new HashMap<>();
                result.put("nickname", "企业微信用户");
                result.put("openid", openId);
                return result;
            }
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(response, Map.class);
        } catch (Exception e) {
            log.warn("OAuth UserInfo fetch failed, using defaults: {}", e.getMessage());
            return Map.of("nickname", "第三方用户", "openid", openId);
        }
    }
}
