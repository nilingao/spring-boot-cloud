package cn.com.tzy.springbootfs.config.socket.event.agent;

import cn.com.tzy.springbootcomm.common.enumcom.ConstEnum;
import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootfs.config.socket.common.agent.AgentInStatusData;
import cn.com.tzy.springbootfs.config.socket.namespace.agent.AgentNamespace;
import cn.com.tzy.springbootstarterfreeswitch.common.socket.AgentCommon;
import cn.com.tzy.springbootstarterfreeswitch.enums.fs.AgentStateEnum;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstartersocketio.pool.EventListener;
import cn.com.tzy.springbootstartersocketio.pool.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 客服更变自身状态 繁忙或者空闲 状态
 */
@Log4j2
@Component
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
    public void onData(SocketIOClient client, AgentInStatusData data, AckRequest request){
        //主叫
        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,RestResult.result(RespCode.CODE_2.getValue(),String.format("客服 ： %s ,未登录",agentKey)));
            return;
        }else if(data.getUpdateStatus() == null || data.getUpdateStatus()== ConstEnum.Flag.NO.getValue()){
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,RestResult.result(RespCode.CODE_0.getValue(),"客服信息获取成功",agentVoInfo));
            return;
        }
        if(!Arrays.asList(AgentStateEnum.LOGIN, AgentStateEnum.BUSY_OTHER, AgentStateEnum.READY).contains(agentVoInfo.getAgentState())){
            client.sendEvent(AgentCommon.AGENT_OUT_STATUS,RestResult.result(RespCode.CODE_2.getValue(),"客服当前无法更新状态"));
            return;
        }
        //更变状态为空闲状态
        if(Arrays.asList(AgentStateEnum.LOGIN, AgentStateEnum.BUSY_OTHER).contains(agentVoInfo.getAgentState())){
            agentVoInfo.setAgentState(AgentStateEnum.READY);
        } else if ( AgentStateEnum.READY ==agentVoInfo.getAgentState()) {
            agentVoInfo.setAgentState(AgentStateEnum.BUSY_OTHER);
        }
        RedisService.getAgentInfoManager().put(agentVoInfo);
        client.sendEvent(AgentCommon.AGENT_OUT_STATUS,RestResult.result(RespCode.CODE_0.getValue(),"客服状态更新成功",agentVoInfo));
    }
}
