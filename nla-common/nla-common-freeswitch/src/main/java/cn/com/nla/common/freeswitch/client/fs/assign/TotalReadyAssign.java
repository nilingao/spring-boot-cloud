package cn.com.nla.common.freeswitch.client.fs.assign;


import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.client.fs.AgentStrategy;

/**
 * Created by caoliang on 2021/8/3
 *
 * 累计空闲最长时间，不包含当前的空闲时间
 */
public class TotalReadyAssign implements AgentStrategy {

    @Override
    public Long calculateLevel(AgentVoInfo agentInfo) {
        return agentInfo.getTotalReadyTime();
    }
}
