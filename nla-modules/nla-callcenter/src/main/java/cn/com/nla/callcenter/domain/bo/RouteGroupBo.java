package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.RouteGroup;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 网关组输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = RouteGroup.class, reverseConvertGenerate = false)
public class RouteGroupBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 网关组名称。 */
    @Size(max = 255, message = "name长度超出限制", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 状态。 */
    private Integer status;
}
