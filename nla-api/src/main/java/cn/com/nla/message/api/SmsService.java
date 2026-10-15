package cn.com.nla.message.api;

import java.util.Collection;

/**
 * 通用 短信服务
 *
 * <p>跨模块出口：短信通道与模板由 {@code nla-message} 表驱动管理，其它模块（如工作流通知）
 * 只依赖本接口，不直接依赖 sms4j 与短信表。</p>
 *
 * @author TZY
 */
public interface SmsService {

    /**
     * 批量发送通知类短信（免模板，内容原样下发到内容型渠道）。
     *
     * <p>无可用内容型渠道、手机号为空或全部发送失败时返回 {@code 0}，不抛异常，
     * 调用方无需为短信失败中断主流程。</p>
     *
     * @param mobiles 接收手机号集合，空值与空白项会被跳过
     * @param content 短信内容（渠道签名由通道配置自行拼接）
     * @return 实际发送成功的手机号数量
     */
    int sendNotice(Collection<String> mobiles, String content);

}
