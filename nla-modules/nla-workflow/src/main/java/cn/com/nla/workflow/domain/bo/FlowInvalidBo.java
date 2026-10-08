package cn.com.nla.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import cn.com.nla.common.core.validate.AddGroup;

/**
 * 作废流程请求对象。
 *
 * @param id      流程实例 ID
 * @param comment 作废意见
 * @author TZY
 */
public record FlowInvalidBo(
    @NotNull(message = "流程实例id为空", groups = AddGroup.class)
    Long id,
    String comment
) {
}
