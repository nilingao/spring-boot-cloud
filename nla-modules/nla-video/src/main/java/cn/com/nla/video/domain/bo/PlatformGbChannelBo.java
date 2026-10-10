package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.PlatformGbChannel;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 国标级联关联通道信息输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = PlatformGbChannel.class, reverseConvertGenerate = false)
public class PlatformGbChannelBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 平台国标ID。 */
    @NotBlank(message = "platformId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "platformId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String platformId;

    /** 目录ID。 */
    @NotBlank(message = "catalogId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "catalogId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String catalogId;

    /** 通道国标ID。 */
    @NotBlank(message = "deviceChannelId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "deviceChannelId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String deviceChannelId;
}
