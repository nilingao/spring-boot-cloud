package cn.com.nla.common.freeswitch.client.fs.assign;


import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.client.fs.AgentStrategy;

/**
 * Created by caoliang on 2021/8/3
 * <p>
 * 累计话后时间长优先
 */
public class TotalAfterTimeAssign implements AgentStrategy {

    @Override
    public Long calculateLevel(AgentVoInfo agentInfo) {
        return agentInfo.getTotalAfterTime();
    }
}
