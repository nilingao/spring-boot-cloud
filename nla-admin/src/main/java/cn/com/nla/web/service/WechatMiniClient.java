package cn.com.nla.web.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.web.config.properties.XcxProperties;
import lombok.RequiredArgsConstructor;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.springframework.stereotype.Component;

/** 用 wx.login 的一次性 code 换取服务端确认的身份，不保存/返回 session_key。 */
@Component
@RequiredArgsConstructor
public class WechatMiniClient {
    private final XcxProperties properties;

    public Identity exchange(String appid, String code, String clientId) {
        if (StringUtils.isBlank(appid) || !appid.matches("wx[0-9a-f]{16}")
            || StringUtils.isBlank(code) || code.length() > 512) {
            throw new ServiceException("小程序登录参数无效");
        }
        var app = properties.getApps() == null ? null : properties.getApps().get(appid);
        if (!properties.isEnabled() || app == null || !app.isEnabled()
            || StringUtils.isBlank(app.getSecret()) || app.getClientIds() == null
            || StringUtils.isBlank(clientId) || !app.getClientIds().contains(clientId)) {
            throw new ServiceException("当前客户端未配置可用的小程序");
        }
        AuthResponse<AuthUser> response;
        try {
            var callback = new AuthCallback();
            callback.setCode(code);
            response = createRequest(AuthConfig.builder().clientId(appid).clientSecret(app.getSecret())
                .ignoreCheckRedirectUri(true).ignoreCheckState(true).build()).login(callback);
        } catch (RuntimeException e) {
            // 供应商异常可能带有 code、密钥或 session_key，统一对外错误，避免泄露。
            throw new ServiceException("小程序授权失败，请重新获取登录凭证");
        }
        if (response == null || !response.ok() || response.getData() == null
            || response.getData().getToken() == null) {
            throw new ServiceException("小程序授权失败，请重新获取登录凭证");
        }
        var token = response.getData().getToken();
        String openid = token.getOpenId();
        if (StringUtils.isBlank(openid) || !openid.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ServiceException("小程序授权未返回有效身份");
        }
        String unionId = token.getUnionId();
        if (unionId != null && unionId.length() > 128) {
            throw new ServiceException("小程序授权未返回有效身份");
        }
        return new Identity(appid, openid, unionId);
    }

    /** 创建供应商请求；测试替换此边界，不发起外部请求。 */
    protected AuthRequest createRequest(AuthConfig config) {
        return new AuthWechatMiniProgramRequest(config);
    }

    public record Identity(String appid, String openid, String unionId) { }
}
