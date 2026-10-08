package cn.com.nla.message.sms;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 短信渠道类型 ↔ sms4j supplier 映射，迁移自旧 {@code SmsTypeEnum}。
 * <p>
 * 创蓝网（smsType=2）已废弃，不在此列。{@code templateBased} 标识该渠道走「模板+参数」发送，
 * 否则走「纯内容」发送（内容型由实现类自行拼接签名）。
 * </p>
 *
 * @author TZY
 */
@Getter
@AllArgsConstructor
public enum SmsChannelEnum {

    /**
     * 短信网（内容型，SPI 自定义）
     */
    DXW(1, "dxw", "短信网", false),
    /**
     * 维纳多（内容型，SPI 自定义）
     */
    WND(3, "wnd", "维纳多", false),
    /**
     * 商务领航（内容型，SPI 自定义）
     */
    SWLH(4, "swlh", "商务领航", false),
    /**
     * 阿里云大于（模板型，sms4j 内置 alibaba）
     */
    ALIBABA(5, "alibaba", "阿里云大于", true),
    /**
     * 网易易盾（模板型，SPI 自定义）
     */
    WYYD(6, "wyyd", "网易易盾", true),
    /**
     * 云通讯/容联（模板型，sms4j 内置 cloopen）
     */
    CLOOPEN(7, "cloopen", "云通讯", true),
    /**
     * 腾讯云（模板型，sms4j 内置 tencent）
     */
    TENCENT(8, "tencent", "腾讯云", true),
    ;

    /**
     * 渠道类型值（对应 sms_sms_config.sms_type）
     */
    private final int smsType;

    /**
     * sms4j 供应商标识
     */
    private final String supplier;

    /**
     * 渠道名称
     */
    private final String name;

    /**
     * 是否模板型渠道
     */
    private final boolean templateBased;

    /**
     * 按渠道类型值查枚举。
     *
     * @param smsType 渠道类型值
     * @return 匹配的枚举，未知或已废弃返回 {@code null}
     */
    public static SmsChannelEnum of(Integer smsType) {
        if (smsType == null) {
            return null;
        }
        for (SmsChannelEnum e : values()) {
            if (e.smsType == smsType) {
                return e;
            }
        }
        return null;
    }
}
