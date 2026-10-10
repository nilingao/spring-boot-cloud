package cn.com.nla.system.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 绑定请求仅含小程序与授权凭证；绑定用户来自当前会话。 */
@Data
public class XcxBindBody {
    @NotBlank
    @Pattern(regexp = "wx[0-9a-f]{16}")
    private String appid;
    @NotBlank
    @Size(max = 512)
    private String xcxCode;
}
