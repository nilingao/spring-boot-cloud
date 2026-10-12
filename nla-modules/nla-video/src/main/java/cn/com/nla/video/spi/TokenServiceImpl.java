package cn.com.nla.video.spi;

import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.video.core.service.authentication.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 播放器与推流鉴权实现：以单体登录态替代原微服务的 Token 校验接口。
 *
 * <p>仅在 {@code video.enabled=true} 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class TokenServiceImpl implements TokenService {

    /**
     * 校验播放或推流地址携带的登录 Token 是否有效。
     *
     * @param token 登录 Token
     * @return 校验通过返回 true
     */
    @Override
    public boolean authentication(String token) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            return LoginHelper.getLoginUser(token) != null;
        } catch (Exception e) {
            log.info("[视频鉴权] Token 校验失败：{}", e.getMessage());
            return false;
        }
    }
}
