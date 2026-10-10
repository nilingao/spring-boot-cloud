package cn.com.nla.system.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import cn.com.nla.common.core.domain.model.LoginBody;

/**
 * 小程序登录请求对象。
 *
 * @author TZY
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class XcxLoginBody extends LoginBody {

    /**
     * 小程序id(多个小程序时使用)
     */
    @NotBlank
    @Pattern(regexp = "wx[0-9a-f]{16}")
    private String appid;

    /**
     * 小程序code
     */
    @NotBlank(message = "{xcx.code.not.blank}")
    @Size(max = 512)
    private String xcxCode;

}
