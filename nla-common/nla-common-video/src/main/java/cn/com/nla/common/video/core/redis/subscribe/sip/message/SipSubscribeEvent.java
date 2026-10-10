package cn.com.nla.common.video.core.redis.subscribe.sip.message;

import cn.com.nla.common.video.core.model.EventResult;

@FunctionalInterface
public interface SipSubscribeEvent {
     void response(EventResult eventResult);
}
