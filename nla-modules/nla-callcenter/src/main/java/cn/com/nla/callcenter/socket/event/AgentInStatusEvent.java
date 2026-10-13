package cn.com.nla.callcenter.socket.event;

import cn.com.nla.callcenter.socket.AgentNamespace;
import cn.com.nla.callcenter.socket.message.AgentInStatusData;
import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 坐席状态变更入站事件 {@code AGENT:IN:STATUS}。
 *
 * <p>updateStatus 为空或 0 时仅回查当前坐席信息；为 1 时在登录/繁忙与空闲之间切换，
 * 仅登录、繁忙、空闲三种状态允许切换，其余状态回执不可更新。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"socket-io.enabled", "freeswitch.enabled"}, havingValue = "true")
public class AgentInStatusEvent implements EventListener<AgentInStatusData> {

    private final AgentNamespace agentNamespace;

    public AgentInStatusEvent(AgentNamespace agentNamespace) {
        this.agentNamespace = agentNamespace;
    }

    @Override
    public Class<AgentInStatusData> getEventClass() {
        return AgentInStatusData.class;
    }

    @Override
    public String getEventName() {
        return AgentCommon.AGENT_IN_STATUS;
    }

    @Override
    public NamespaceListener getNamespace() {
        return agentNamespace;
    }

    @Override
    public void onData(SocketIOClient client, AgentInStatusData data, AckRequest request) {
        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        if (agentKey == null || agentKey.isBlank()) {
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,
                new RestResultEvent(RespCode.CODE_2.getValue(), "坐席连接未完成认证"));
            return;
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 ： %s ,未登录", agentKey)));
            return;
        } else if (data == null || data.getUpdateStatus() == null || data.getUpdateStatus() == ConstEnum.Flag.NO.getValue()) {
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,
                new RestResultEvent(RespCode.CODE_0.getValue(), "客服信息获取成功", agentVoInfo));
            return;
        }
        if (data.getUpdateStatus() != ConstEnum.Flag.YES.getValue()) {
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,
                new RestResultEvent(RespCode.CODE_2.getValue(), "客服状态更新参数错误"));
            return;
        }
        if (!Arrays.asList(AgentStateEnum.LOGIN, AgentStateEnum.BUSY_OTHER, AgentStateEnum.READY)
            .contains(agentVoInfo.getAgentState())) {
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,
                new RestResultEvent(RespCode.CODE_2.getValue(), "客服当前无法更新状态"));
            return;
        }
        // 更变状态为空闲状态
        if (Arrays.asList(AgentStateEnum.LOGIN, AgentStateEnum.BUSY_OTHER).contains(agentVoInfo.getAgentState())) {
            agentVoInfo.setAgentState(AgentStateEnum.READY);
        } else if (AgentStateEnum.READY == agentVoInfo.getAgentState()) {
            agentVoInfo.setAgentState(AgentStateEnum.BUSY_OTHER);
        }
        RedisService.getAgentInfoManager().put(agentVoInfo);
        client.sendEvent(AgentCommon.AGENT_OUT_STATUS,
            new RestResultEvent(RespCode.CODE_0.getValue(), "客服状态更新成功", agentVoInfo));
    }
}
