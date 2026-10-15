package cn.com.nla.message.service.impl;

import cn.com.nla.message.api.SmsService;
import cn.com.nla.message.sms.core.SmsSendManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 通用 短信服务实现
 *
 * <p>跨模块入口：把工作流等外部通知短信接上本模块的表驱动发送引擎，
 * 通道选择、失败转移与发送记录落库全部委托 {@link SmsSendManager}。</p>
 *
 * @author TZY
 */
@RequiredArgsConstructor
@Service
public class SmsServiceImpl implements SmsService {

    private final SmsSendManager smsSendManager;

    /**
     * 批量发送通知类短信（免模板，内容原样下发到内容型渠道）。
     *
     * @param mobiles 接收手机号集合，空值与空白项会被跳过
     * @param content 短信内容
     * @return 实际发送成功的手机号数量；无可用内容型通道时返回 0
     */
    @Override
    public int sendNotice(Collection<String> mobiles, String content) {
        return smsSendManager.sendNotice(mobiles, content);
    }

}
