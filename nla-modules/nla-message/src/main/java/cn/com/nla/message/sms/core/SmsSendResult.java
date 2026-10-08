package cn.com.nla.message.sms.core;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短信发送结果。
 * <p>
 * 供上层 Service / Controller 转换为统一响应：{@code success=true} 表示可向调用方返回成功语义
 * （含「防重发拦截」——与旧实现返回成功码一致，仅本次未真正下发）。
 * </p>
 *
 * @author TZY
 */
@Data
public class SmsSendResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否视为成功响应
     */
    private boolean success;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 验证码有效期（秒），供前端倒计时；无则为 {@code null}
     */
    private Long expireSeconds;

    /**
     * 网关回执消息 ID（发送成功时）
     */
    private String msgId;

    /**
     * 防重发拦截：视为成功响应，但本次未真正下发。
     *
     * @param message       提示信息
     * @param expireSeconds 距可再次发送的剩余秒数
     * @return 结果
     */
    public static SmsSendResult blocked(String message, long expireSeconds) {
        SmsSendResult result = new SmsSendResult();
        result.success = true;
        result.message = message;
        result.expireSeconds = expireSeconds;
        return result;
    }

    /**
     * 发送成功。
     *
     * @param message       提示信息
     * @param expireSeconds 验证码有效期（秒），无则为 {@code null}
     * @param msgId         网关回执消息 ID
     * @return 结果
     */
    public static SmsSendResult ok(String message, Long expireSeconds, String msgId) {
        SmsSendResult result = new SmsSendResult();
        result.success = true;
        result.message = message;
        result.expireSeconds = expireSeconds;
        result.msgId = msgId;
        return result;
    }

    /**
     * 发送失败。
     *
     * @param message 失败原因
     * @return 结果
     */
    public static SmsSendResult fail(String message) {
        SmsSendResult result = new SmsSendResult();
        result.success = false;
        result.message = message;
        return result;
    }
}
