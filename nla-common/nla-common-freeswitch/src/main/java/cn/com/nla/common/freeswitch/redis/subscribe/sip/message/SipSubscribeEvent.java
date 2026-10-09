package cn.com.nla.common.freeswitch.redis.subscribe.sip.message;


import cn.com.nla.common.freeswitch.vo.sip.EventResult;

@FunctionalInterface
public interface SipSubscribeEvent {
     void response(EventResult eventResult);
}
