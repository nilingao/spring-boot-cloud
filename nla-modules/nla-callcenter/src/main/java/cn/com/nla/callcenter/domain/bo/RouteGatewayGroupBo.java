package cn.com.nla.callcenter.domain.bo;

import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.callcenter.domain.RouteGatewayGroup;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 路由与网关组关系表输入，不接受审计/删除字段。
 * @author TZY
 */
@Data
@AutoMapper(target = RouteGatewayGroup.class, reverseConvertGenerate = false)
public class RouteGatewayGroupBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Null(message = "新增时不能指定主键", groups = AddGroup.class)
    @NotNull(message = "修改时必须指定主键", groups = EditGroup.class)
    private Long id;

    /** 网关组。 */
    @Min(value = 0, message = "routeGroupId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long routeGroupId;

    /** 媒体网关。 */
    @Min(value = 0, message = "gatewayId不能为负数", groups = {AddGroup.class, EditGroup.class})
    private Long gatewayId;
}
