package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.vo.fs.AgentNotifyVo;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Slf4j
@Component
public class AgentNotifySubscribeManager {

    public static final String AGENT_NOTIFY = RedisConstant.AGENT_NOTIFY;
    public static final String AGENT_NOTIFY_PRESENCE = RedisConstant.AGENT_NOTIFY_PRESENCE;

    public boolean getPresenceSubscribe(String gbId){
        String key = String.format("%s%s",AGENT_NOTIFY_PRESENCE, gbId);
        return ObjectUtils.isNotEmpty(RedisUtils.getCacheObject(key));
    }

    public boolean addPresenceSubscribe(AgentVoInfo agentNotifyVo){
        if (agentNotifyVo == null) {
            return false;
        }
        log.info("[添加Presence订阅] 设备{}", agentNotifyVo.getAgentKey());
        AgentNotifyVo build = AgentNotifyVo.builder().type(AgentNotifyVo.TypeEnum.PRESENCE.getValue()).operate(AgentNotifyVo.OperateEnum.ADD.getValue()).agentKey(agentNotifyVo.getAgentKey()).build();
        RedisUtils.publish(AGENT_NOTIFY, SerializationUtils.serialize(build));
        return true;
    }

    public boolean removePresenceSubscribe(AgentVoInfo agentNotifyVo) {
        if (agentNotifyVo == null) {
            return false;
        }
        log.info("[移除Presence订阅]: {}",  agentNotifyVo.getAgentKey());
        AgentNotifyVo build = AgentNotifyVo.builder().type(AgentNotifyVo.TypeEnum.PRESENCE.getValue()).operate(AgentNotifyVo.OperateEnum.DEL.getValue()).agentKey(agentNotifyVo.getAgentKey()).build();
        RedisUtils.publish(AGENT_NOTIFY, SerializationUtils.serialize(build));
        return true;
    }

}
