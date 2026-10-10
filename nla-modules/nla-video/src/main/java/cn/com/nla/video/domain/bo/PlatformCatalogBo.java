package cn.com.nla.video.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.video.domain.PlatformCatalog;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 国标级联-目录输入，不接受审计与删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = PlatformCatalog.class, reverseConvertGenerate = false)
public class PlatformCatalogBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "协议主键不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, groups = {AddGroup.class, EditGroup.class})
    private String id;

    /** 父级目录ID。 */
    @Size(max = 50, message = "parentId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String parentId;

    /** 名称。 */
    @NotBlank(message = "name不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 平台ID。 */
    @NotBlank(message = "platformId不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "platformId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String platformId;

    /** 行政区划。 */
    @Size(max = 50, message = "civilCode长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String civilCode;

    /** 目录分组。 */
    @Size(max = 50, message = "businessGroupId长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String businessGroupId;
}
