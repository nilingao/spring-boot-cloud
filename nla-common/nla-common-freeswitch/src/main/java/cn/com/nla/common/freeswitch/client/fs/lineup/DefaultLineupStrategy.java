package cn.com.nla.common.freeswitch.client.fs.lineup;


import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.client.fs.LineupStrategy;

import java.time.ZoneId;

/**
 * Created by caoliang on 2021/8/2
 * <p>
 * 默认按照进技能组时间，时间越小转坐席越早
 */
public class DefaultLineupStrategy implements LineupStrategy {

    @Override
    public Long calculateLevel(CallInfo callInfo) {
        return -callInfo.getQueueStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
