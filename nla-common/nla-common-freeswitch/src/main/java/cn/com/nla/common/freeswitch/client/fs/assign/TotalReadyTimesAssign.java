package cn.com.nla.common.freeswitch.client.fs.assign;


import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.client.fs.AgentStrategy;

/**
 * Created by caoliang on 2021/8/3
 * <p>
 * 空闲次数最多优先
 */
public class TotalReadyTimesAssign implements AgentStrategy {
    @Override
    public Long calculateLevel(AgentVoInfo agentInfo) {
        return agentInfo.getReadyTimes();
    }
}
