package cn.com.nla.video.spi;

import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.video.core.service.authentication.CurrentUserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 当前用户提供者：把单体登录态暴露给国标点播/下载流程，用于记录发起下载的用户。
 *
 * <p>SIP 回调线程没有请求上下文，此时返回 {@code null}，由协议层回退到默认用户。
 * 仅在 {@code video.enabled=true} 时注册，覆盖自动配置里的兜底实现。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoLoginUserProvider implements CurrentUserProvider {

    /**
     * 获取当前登录用户编号。
     *
     * @return 用户编号，无登录上下文时返回 null
     */
    @Override
    public Long getUserId() {
        return LoginHelper.getUserId();
    }
}
