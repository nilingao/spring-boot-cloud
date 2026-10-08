package cn.com.nla.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import cn.com.nla.common.core.validate.AddGroup;

/**
 * 终止任务请求对象
 *
 * @param taskId  任务 ID
 * @param comment 终止意见
 * @author TZY
 */
public record FlowTerminationBo(
    @NotNull(message = "任务id为空", groups = AddGroup.class)
    Long taskId,
    String comment
) {
}
