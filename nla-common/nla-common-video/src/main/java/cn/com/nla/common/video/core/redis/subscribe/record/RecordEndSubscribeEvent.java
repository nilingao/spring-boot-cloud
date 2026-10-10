package cn.com.nla.common.video.core.redis.subscribe.record;

import cn.com.nla.common.video.basic.vo.sip.RecordInfo;

@FunctionalInterface
public interface RecordEndSubscribeEvent {
        void  handler(RecordInfo recordInfo);
}