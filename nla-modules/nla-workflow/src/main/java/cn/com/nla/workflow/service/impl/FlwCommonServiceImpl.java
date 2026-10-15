package cn.com.nla.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.com.nla.common.core.enums.BusinessStatusEnum;
import cn.com.nla.common.core.enums.PushSourceEnum;
import cn.com.nla.common.core.enums.PushTypeEnum;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.core.utils.StreamUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.core.utils.ThreadUtils;
import cn.com.nla.common.mail.core.MailBuilder;
import cn.com.nla.message.api.SmsService;
import cn.com.nla.system.api.MessageService;
import cn.com.nla.system.api.domain.PushPayloadDTO;
import cn.com.nla.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.orm.entity.FlowTask;
import cn.com.nla.workflow.common.ConditionalOnEnable;
import cn.com.nla.workflow.common.enums.MessageTypeEnum;
import cn.com.nla.workflow.service.IFlwCommonService;
import cn.com.nla.workflow.service.IFlwTaskService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static cn.com.nla.workflow.common.constant.FlowConstant.PATH_MY_DOCUMENT;
import static cn.com.nla.workflow.common.constant.FlowConstant.PATH_TASK_WAITING;

/**
 * 工作流工具
 *
 * @author TZY
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwCommonServiceImpl implements IFlwCommonService {

    private static final String DEFAULT_SUBJECT = "单据审批提醒";
    private final MessageService messageService;
    private final ObjectProvider<SmsService> smsServiceProvider;

    /**
     * 根据流程实例发送消息给当前处理人
     *
     * @param flowName    流程定义名称
     * @param instId      流程实例ID
     * @param messageType 消息类型列表
     * @param message     消息内容，为空则使用默认消息
     */
    @Override
    public void sendMessage(String flowName, Long instId, List<String> messageType, String message) {
        if (CollUtil.isEmpty(messageType)) {
            return;
        }
        IFlwTaskService flwTaskService = SpringUtils.getBean(IFlwTaskService.class);
        List<FlowTask> list = flwTaskService.selectByInstId(instId);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + flowName + "】单据已经提交至您，请您及时处理。";
        }
        List<UserDTO> userList = flwTaskService.currentTaskAllUser(StreamUtils.toList(list, FlowTask::getId));
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        // 发给当前处理人的工作流消息统一进入“我的待办”。
        sendMessage(messageType, message, DEFAULT_SUBJECT, userList, PATH_TASK_WAITING);
    }

    /**
     * 发送消息给指定用户列表
     *
     * @param messageType 消息类型列表
     * @param message     消息内容
     * @param subject     邮件标题
     * @param userList    接收用户列表
     */
    @Override
    public void sendMessage(List<String> messageType, String message, String subject, List<UserDTO> userList) {
        sendMessage(messageType, message, subject, userList, null);
    }

    /**
     * 发送流程结果通知。
     *
     * @param flowName    流程名称
     * @param status      业务状态
     * @param messageType 消息类型列表
     * @param userList    接收用户列表
     */
    @Override
    public void sendResultMessage(String flowName, BusinessStatusEnum status, List<String> messageType, List<UserDTO> userList) {
        if (status == null || CollUtil.isEmpty(messageType) || CollUtil.isEmpty(userList)) {
            return;
        }
        // 审批结果类消息面向发起人查看，统一跳转到“我发起的”。
        String message = "您发起的【" + flowName + "】单据审批已" + status.getDesc() + "。";
        sendMessage(messageType, message, DEFAULT_SUBJECT, userList, PATH_MY_DOCUMENT);
    }

    /**
     * 发送消息给指定用户列表。
     *
     * @param messageType 消息类型列表
     * @param message     消息内容
     * @param subject     邮件标题
     * @param userList    接收用户列表
     * @param path        前端跳转路径
     */
    @Override
    public void sendMessage(List<String> messageType, String message, String subject, List<UserDTO> userList, String path) {
        if (CollUtil.isEmpty(messageType) || CollUtil.isEmpty(userList)) {
            return;
        }
        List<Long> userIds = new ArrayList<>(StreamUtils.toSet(userList, UserDTO::getUserId));
        Set<String> emails = StreamUtils.toSet(userList, UserDTO::getEmail);
        emails.removeIf(StringUtils::isBlank);
        Set<String> mobiles = StreamUtils.toSet(userList, UserDTO::getPhoneNumber);
        mobiles.removeIf(StringUtils::isBlank);

        Runnable[] sendTasks = messageType.stream()
            .map(code -> (Runnable) () -> sendMessageByType(code, message, subject, path, userIds, emails, mobiles))
            .toArray(Runnable[]::new);
        ThreadUtils.virtualInvokeAll(sendTasks);
    }

    /**
     * 按消息类型执行具体发送逻辑。
     *
     * @param code    消息类型编码
     * @param message 消息内容
     * @param subject 邮件标题
     * @param path    前端跳转路径
     * @param userIds 接收用户 id 列表
     * @param emails  接收邮箱集合
     * @param mobiles 接收手机号集合
     */
    private void sendMessageByType(String code, String message, String subject, String path, List<Long> userIds, Set<String> emails, Set<String> mobiles) {
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
        if (ObjectUtil.isEmpty(messageTypeEnum)) {
            return;
        }
        try {
            switch (messageTypeEnum) {
                case SYSTEM_MESSAGE -> {
                    // 站内消息直接携带前端路由，消息盒子点击后可按路径分流。
                    messageService.publishMessage(userIds, PushPayloadDTO.of(
                        PushTypeEnum.MESSAGE,
                        PushSourceEnum.WORKFLOW,
                        message, null, path
                    ));
                }
                case EMAIL_MESSAGE -> MailBuilder.of().to(emails).subject(subject).text(message).send();
                case SMS_MESSAGE -> {
                    SmsService smsService = smsServiceProvider.getIfAvailable();
                    if (smsService == null) {
                        log.warn("【消息发送】未接入短信服务实现，跳过短信通知 {} 位", mobiles.size());
                    } else {
                        int sent = smsService.sendNotice(mobiles, message);
                        log.info("【短信发送】接收人={}，成功={}", mobiles.size(), sent);
                    }
                }
                default -> log.warn("【消息发送】未处理的消息类型：{}", messageTypeEnum);
            }
        } catch (Exception ex) {
            // 记录错误但不抛出，确保主逻辑不受影响
            log.error("【消息发送失败】类型={}，原因={}", messageTypeEnum, ex.getMessage(), ex);
        }
    }

    /**
     * 申请人节点编码
     *
     * @param definitionId 流程定义id
     * @return 申请人节点编码
     */
    @Override
    public String applyNodeCode(Long definitionId) {
        List<Node> firstBetweenNode = FlowEngine.nodeService().getFirstBetweenNode(definitionId, new HashMap<>());
        if (CollUtil.isEmpty(firstBetweenNode)) {
            throw new ServiceException("流程定义缺少申请人节点，请检查流程定义配置");
        }
        return firstBetweenNode.getFirst().getNodeCode();
    }
}
