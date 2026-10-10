package cn.com.nla.system.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序手机号授权请求对象。
 *
 * @author TZY
 */
@Data
public class XcxPhoneBody {

    /**
     * 小程序 appid，须与当前会话中的 appid 一致。
     */
    @NotBlank
    @Pattern(regexp = "wx[0-9a-f]{16}")
    private String appid;

    /**
     * wx.getPhoneNumber() 返回的一次性 code。
     */
    @NotBlank
    @Size(max = 512)
    private String phoneCode;

}
