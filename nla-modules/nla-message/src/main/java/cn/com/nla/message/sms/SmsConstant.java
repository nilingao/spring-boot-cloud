package cn.com.nla.message.sms;

/**
 * 短信发送相关常量。
 * <p>
 * 迁移自旧 {@code SmsConstant} / {@code SmsCodeTokenConstant} / {@code MobileMessageType}。
 * 验证码缓存 key 前缀须与鉴权侧校验逻辑保持一致（{@code redis:verificationCode:{type}_{mobile}}）。
 * </p>
 *
 * @author TZY
 */
public final class SmsConstant {

    private SmsConstant() {
    }

    /**
     * 验证码缓存 key 前缀，完整 key = 前缀 + 消息类型 + "_" + 手机号
     */
    public static final String VERIFICATION_CODE_PREFIX = "redis:verificationCode:";

    /**
     * 模板变量占位：验证码（值缺失时自动生成 6 位随机码）
     */
    public static final String VERIFICATION_CODE = "VERIFICATION_CODE";

    /**
     * 模板变量占位：验证码缓存分钟数（值缺失时取 {@link #REDIS_TIME}）
     */
    public static final String REDIS_CODE = "REDIS_CODE";

    /**
     * 验证码默认缓存分钟数
     */
    public static final int REDIS_TIME = 5;

    /**
     * 消息类型：登录验证码
     */
    public static final int TYPE_LOGIN = 1;

    /**
     * 消息类型：注册验证码
     */
    public static final int TYPE_REGISTER = 2;

    /**
     * 消息类型：重置密码验证码
     */
    public static final int TYPE_RESET = 3;

    /**
     * 发送记录状态：成功
     */
    public static final int STATUS_SUCCESS = 1;

    /**
     * 发送记录状态：失败
     */
    public static final int STATUS_FAIL = 0;
}
