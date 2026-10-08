package cn.com.nla.message.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 短信发送业务对象。
 * <p>
 * 迁移自旧 {@code SendParam}：{@code type} 为消息类型（非渠道类型），
 * 具体渠道由「活跃且配置了该消息类型模板」的账号决定。变量取自模板，无需外部传入。
 * </p>
 *
 * @author TZY
 */
@Data
public class SmsSendBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型（1登录验证码 2注册验证码 3重置密码验证码）
     */
    @NotNull(message = "发送类型不能为空")
    private Integer type;

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
    private String mobile;
}
