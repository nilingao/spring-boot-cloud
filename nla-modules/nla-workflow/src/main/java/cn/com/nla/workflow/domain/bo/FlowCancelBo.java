package cn.com.nla.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import cn.com.nla.common.core.validate.AddGroup;

/**
 * 撤销流程请求对象。
 *
 * @param businessId 业务 ID
 * @param message    撤销说明
 * @author TZY
 */
public record FlowCancelBo(
    @NotBlank(message = "业务ID不能为空", groups = AddGroup.class)
    String businessId,
    String message
) {
}
