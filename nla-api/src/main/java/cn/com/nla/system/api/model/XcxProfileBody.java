package cn.com.nla.system.api.model;

import cn.com.nla.common.core.xss.Xss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 小程序资料授权请求对象；昵称/性别/头像至少提供一个，只更新提供的字段。
 *
 * @author TZY
 */
@Data
public class XcxProfileBody {

    /**
     * 小程序 appid，须与当前会话中的 appid 一致。
     */
    @NotBlank
    @Pattern(regexp = "wx[0-9a-f]{16}")
    private String appid;

    /**
     * 用户昵称，来自微信 type=nickname 输入组件，最长 30 字符。
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * 用户性别（0男 1女 2未知）。
     */
    @Pattern(regexp = "[012]", message = "用户性别无效")
    private String gender;

    /**
     * 头像 OSS ID；chooseAvatar 选择的临时图片须先经 /resource/oss/upload 上传后回传 ossId。
     */
    @Positive
    private Long avatar;

}
