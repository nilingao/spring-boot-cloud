package cn.com.nla.workflow.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import lombok.RequiredArgsConstructor;
import cn.com.nla.common.core.enums.BusinessStatusEnum;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.system.api.UserService;
import cn.com.nla.system.api.domain.UserDTO;
import cn.com.nla.workflow.common.ConditionalOnEnable;
import cn.com.nla.workflow.common.enums.MessageTypeEnum;
import cn.com.nla.workflow.event.WorkflowCopyEvent;
import cn.com.nla.workflow.event.WorkflowResultMessageEvent;
import cn.com.nla.workflow.event.WorkflowTaskMessageEvent;
import cn.com.nla.workflow.service.IFlwCommonService;
import cn.com.nla.workflow.service.IFlwTaskService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 工作流副作用事件监听器。
 *
 * @author TZY
 */
@ConditionalOnEnable
@Component
@RequiredArgsConstructor
public class WorkflowSideEffectListener {

    private final IFlwTaskService flwTaskService;
    private final IFlwCommonService flwCommonService;
    private final UserService userService;

    /**
     * 保存工作流抄送记录。
     *
     * @param event 工作流抄送事件
     */
    @EventListener
    public void handleCopy(WorkflowCopyEvent event) {
        flwTaskService.setCopy(event.task(), event.flowCopyList());
    }

    /**
     * 发送工作流待办消息。
     *
     * @param event 工作流待办消息事件
     */
    @EventListener
    public void handleTaskMessage(WorkflowTaskMessageEvent event) {
        flwCommonService.sendMessage(event.flowName(), event.instanceId(), event.messageType(), event.notice());
    }

    /**
     * 发送工作流结果消息。
     *
     * @param event 工作流结果消息事件
     */
    @EventListener
    public void handleResultMessage(WorkflowResultMessageEvent event) {
        if (!StringUtils.equalsAny(event.status(), BusinessStatusEnum.FINISH.getStatus(), BusinessStatusEnum.BACK.getStatus())) {
            return;
        }
        Long createBy = Convert.toLong(event.createBy(), null);
        if (createBy == null) {
            return;
        }
        BusinessStatusEnum status = BusinessStatusEnum.getByStatus(event.status());
        if (status == null) {
            return;
        }
        UserDTO initiator = userService.selectById(createBy);
        if (initiator == null || initiator.getUserId() == null) {
            return;
        }
        List<String> messageType = CollUtil.isNotEmpty(event.messageType())
            ? event.messageType()
            : Collections.singletonList(MessageTypeEnum.SYSTEM_MESSAGE.getCode());
        flwCommonService.sendResultMessage(event.flowName(), status, messageType, Collections.singletonList(initiator));
    }

}
