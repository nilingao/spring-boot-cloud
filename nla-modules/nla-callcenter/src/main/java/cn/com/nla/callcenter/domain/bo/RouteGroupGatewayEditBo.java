package cn.com.nla.callcenter.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 路由组网关关系编辑入参: 以完整列表替换该路由组下的全部网关绑定。
 *
 * @author TZY
 */
@Data
public class RouteGroupGatewayEditBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 路由组编号。 */
    @NotNull(message = "路由组编号不能为空")
    private Long routeGroupId;

    /** 网关编号列表, 传空列表表示清空全部绑定。 */
    @NotNull(message = "网关编号列表不能为空")
    private List<Long> gatewayIds;

}
