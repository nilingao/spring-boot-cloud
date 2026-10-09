package cn.com.nla.common.freeswitch.service.freeswitch;

import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.model.notice.CallMessage;

/**
 * 发送坐席消息
 */
public interface SendAgentMessage {
    void sendMessage(AgentStateEnum agentStateEnum, AgentVoInfo agentVoInfo, CallMessage callMessage);

    void sendErrorMessage( AgentVoInfo agentVoInfo, String errorMessage);

    void sendMessage(String namespace,String agentCommon,String agentKey, RestResultEvent result);
}
