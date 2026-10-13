package cn.com.nla.callcenter.spi;

import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.model.notice.CallMessage;
import cn.com.nla.common.freeswitch.service.freeswitch.SendAgentMessage;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 坐席消息推送协议侧实现。
 *
 * <p>对齐旧 SendAgentMessageImpl 的 Socket.IO 房间推送语义：按
 * {@code namespace:agentKey} 拼接房间键，向房间内坐席客户端推送事件。
 * 旧 RestResult 响应包装替换为协议层 RestResultEvent。仅在 freeswitch.enabled=true
 * 时注册，避免协议组件缺失导致启动失败；SocketIOServer 通过 ObjectProvider
 * 延迟获取，socket-io.enabled 未开启时降级为日志输出。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class SendAgentMessageImpl implements SendAgentMessage {

    /**
     * Socket.IO 服务提供器（socket-io.enabled=true 时才有实例）。
     */
    private final ObjectProvider<SocketIOServer> socketIOServerProvider;

    public SendAgentMessageImpl(ObjectProvider<SocketIOServer> socketIOServerProvider) {
        this.socketIOServerProvider = socketIOServerProvider;
    }

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
     * <p>推送目标为 {@code namespace:agentKey} 房间；Socket.IO 服务或命名空间
     * 缺失时降级为日志输出，不影响调用方流程。</p>
     *
     * @param namespace   命名空间
     * @param agentCommon 事件名
     * @param agentKey    坐席账户
     * @param result      消息体
     */
    @Override
    public void sendMessage(String namespace, String agentCommon, String agentKey, RestResultEvent result) {
        SocketIOServer socketIOServer = socketIOServerProvider.getIfAvailable();
        if (socketIOServer == null) {
            log.warn("[坐席消息] Socket.IO 未启用，丢弃推送 event:{} agentKey:{} code:{} message:{}",
                    agentCommon, agentKey, result.getCode(), result.getMessage());
            return;
        }
        String roomKey = String.format("%s:%s", namespace, agentKey);
        SocketIONamespace socketIONamespace = socketIOServer.getNamespace(namespace);
        if (socketIONamespace != null) {
            BroadcastOperations roomOperations = socketIONamespace.getRoomOperations(roomKey);
            if (roomOperations != null) {
                roomOperations.sendEvent(agentCommon, result);
            }
        }
    }
}
