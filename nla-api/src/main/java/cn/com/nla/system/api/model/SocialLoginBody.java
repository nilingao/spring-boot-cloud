package cn.com.nla.system.api.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.com.nla.common.core.domain.model.LoginBody;

/**
 * 第三方平台登录绑定请求对象。
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SocialLoginBody extends LoginBody {

    /**
     * 第三方登录平台
     */
    @NotBlank(message = "{social.source.not.blank}")
    private String source;

    /**
     * 第三方登录code
     */
    @NotBlank(message = "{social.code.not.blank}")
    private String socialCode;

    /**
     * 第三方登录socialState
     */
    @NotBlank(message = "{social.state.not.blank}")
    private String socialState;

}
