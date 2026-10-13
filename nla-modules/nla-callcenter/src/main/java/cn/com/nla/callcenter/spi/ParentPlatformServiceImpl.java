package cn.com.nla.callcenter.spi;

import cn.com.nla.callcenter.domain.Platform;
import cn.com.nla.callcenter.mapper.PlatformMapper;
import cn.com.nla.common.freeswitch.model.bean.ConfigModel;
import cn.com.nla.common.freeswitch.service.sip.ParentPlatformService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 上级平台（FreeSWITCH）配置协议侧实现。
 *
 * <p>迁移自旧 service.sip.ParentPlatformServiceImpl：取库内首条平台记录装配 SIP 栈
 * 配置模型，旧 MapStruct PlatformConvert 的同名隐式转换由 {@link FsSpiConvert} 承担。
 * 仅在 freeswitch.enabled=true 时注册，父类信令依赖（SipServer/SIPCommander）同门控装配。</p>
 *
 * @author TZY
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class ParentPlatformServiceImpl extends ParentPlatformService {

    private final PlatformMapper platformMapper;

    /**
     * 取一条平台配置（旧语义：库内首条）。
     *
     * @return SIP 栈配置模型，无平台数据时返回 null
     */
    @Override
    public ConfigModel random() {
        Platform platform = platformMapper.selectOne(Wrappers.<Platform>lambdaQuery().last("limit 1"));
        return FsSpiConvert.to(platform, ConfigModel.class);
    }
}
