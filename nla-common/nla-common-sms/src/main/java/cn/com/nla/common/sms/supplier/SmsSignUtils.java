package cn.com.nla.common.sms.supplier;

import cn.hutool.core.util.StrUtil;

/**
 * 短信内容签名与模板编号解析工具。
 * <p>
 * 迁移自旧 {@code SmsHttpClient} 的 handleSign / parseTemplateCode，供 4 个 SPI 自定义供应商共用。
 * </p>
 *
 * @author TZY
 */
public final class SmsSignUtils {

    /**
     * 签名位置：左边
     */
    public static final int SIGN_PLACE_LEFT = 1;

    /**
     * 签名位置：右边
     */
    public static final int SIGN_PLACE_RIGHT = 2;

    private SmsSignUtils() {
    }

    /**
     * 处理签名：内容已含【】则原样返回；否则按签名位置拼接【签名】。
     *
     * @param sign      签名文本（不含括号）
     * @param signPlace 签名位置（1左边 2右边，为空按右边）
     * @param content   原始内容
     * @return 拼接签名后的内容
     */
    public static String handleSign(String sign, Integer signPlace, String content) {
        if (content != null && content.contains("【") && content.contains("】")) {
            return content;
        }
        if (StrUtil.isBlank(sign)) {
            return content;
        }
        String signWrap = "【" + sign + "】";
        if (signPlace == null || signPlace == SIGN_PLACE_RIGHT) {
            return content + signWrap;
        }
        return signWrap + content;
    }

    /**
     * 解析模板编号：templateCode 形如 {@code "6:SMS_83975030,7:13824"}，取当前 smsType 前缀对应的编号。
     *
     * @param smsType      渠道类型
     * @param templateCode 多渠道模板编号串
     * @return 当前渠道的模板编号
     */
    public static String parseTemplateCode(int smsType, String templateCode) {
        if (StrUtil.isBlank(templateCode) || !templateCode.contains(":")) {
            return templateCode;
        }
        String result = templateCode;
        String[] list = templateCode.split(",");
        String prefix = smsType + ":";
        for (String e : list) {
            if (e.startsWith(prefix)) {
                result = e.substring(prefix.length());
            }
        }
        return result;
    }
}
