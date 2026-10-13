package cn.com.nla.callcenter.spi;

import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.model.notice.CallMessage;
import cn.com.nla.common.freeswitch.service.freeswitch.SendAgentMessage;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 坐席消息推送协议侧实现。
 *
 * <p>第一轮迁移保留消息装配语义（旧 SendAgentMessageImpl 的 Socket.IO 房间推送），
 * 推送动作暂以日志占位；Socket.IO 坐席事件推送在第二轮接入时替换
 * {@link #sendMessage(String, String, String, RestResultEvent)} 的实现。
 * 旧 RestResult 响应包装替换为协议层 RestResultEvent。仅在 freeswitch.enabled=true
 * 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class SendAgentMessageImpl implements SendAgentMessage {

    /**
     * 推送坐席通话状态消息。
     *
     * @param agentStateEnum 坐席状态
     * @param agentVoInfo    坐席信息
     * @param callMessage    通话消息
     */
    @Override
    public void sendMessage(AgentStateEnum agentStateEnum, AgentVoInfo agentVoInfo, CallMessage callMessage) {
        callMessage.setAgentState(agentStateEnum);
        sendMessage(AgentCommon.SOCKET_AGENT, AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                agentVoInfo.getAgentKey(), new RestResultEvent(RespCode.CODE_0.getValue(), null, callMessage));
    }

    /**
     * 推送坐席错误消息。
     *
     * @param agentVoInfo   坐席信息
     * @param errorMessage 错误消息
     */
    @Override
    public void sendErrorMessage(AgentVoInfo agentVoInfo, String errorMessage) {
        sendMessage(AgentCommon.SOCKET_AGENT, AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                agentVoInfo.getAgentKey(), new RestResultEvent(RespCode.CODE_2.getValue(), errorMessage, null));
    }

    /**
     * 按命名空间与事件名推送消息。
     *
     * <p>第二轮 Socket.IO 接入前仅记录日志，用于验证协议链消息装配正确。</p>
     *
     * @param namespace   命名空间
     * @param agentCommon 事件名
     * @param agentKey    坐席账户
     * @param result      消息体
     */
    @Override
    public void sendMessage(String namespace, String agentCommon, String agentKey, RestResultEvent result) {
        log.info("[坐席消息] namespace:{} event:{} agentKey:{} code:{} message:{}（Socket.IO 推送待第二轮接入）",
                namespace, agentCommon, agentKey, result.getCode(), result.getMessage());
    }
}
