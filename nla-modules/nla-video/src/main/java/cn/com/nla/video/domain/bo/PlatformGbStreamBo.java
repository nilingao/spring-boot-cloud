package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.PlatformGbStream;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 国标级联关联直播流输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = PlatformGbStream.class, reverseConvertGenerate = false)
public class PlatformGbStreamBo implements Serializable {
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

    /** 直播流国标ID。 */
    @NotBlank(message = "gbStreamId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "gbStreamId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String gbStreamId;
}
