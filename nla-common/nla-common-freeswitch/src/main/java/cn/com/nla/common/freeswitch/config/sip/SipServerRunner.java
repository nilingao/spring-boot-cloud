package cn.com.nla.common.freeswitch.config.sip;

import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.response.SipResponseEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.timeout.SipTimeoutEvent;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Order(20)
@Component
public class SipServerRunner implements CommandLineRunner {

    @Resource
    private SipServer sipServer;
    @Resource
    private SipTimeoutEvent sipTimeoutEvent;

    @Override
    public void run(String... args) throws Exception {
        Map<String, SipRequestEvent> sipRequestMap = SpringUtil.getBeansOfType(SipRequestEvent.class);
        Map<String, SipResponseEvent> sipResponseMap = SpringUtil.getBeansOfType(SipResponseEvent.class);
        if(sipRequestMap.isEmpty()){
            log.error("SipRequestEvent 实现的所有Bean未获取到");
            return;
        }
        if(sipResponseMap.isEmpty()){
            log.error("SipResponseEvent 实现的所有Bean未获取到");
            return;
        }
        ConcurrentHashMap<String, SipRequestEvent> sipRequestEventMap = sipRequestMap.values().stream().collect(Collectors.toMap(SipRequestEvent::getMethod, o -> o, (u, v) -> {
            throw new IllegalStateException(String.format("[SipConfig error] :Duplicate key %s", u));
        }, ConcurrentHashMap::new));
        ConcurrentHashMap<String, SipResponseEvent> sipResponseEventMap = sipResponseMap.values().stream().collect(Collectors.toMap(SipResponseEvent::getMethod, o -> o, (u, v) -> {
            throw new IllegalStateException(String.format("[SipConfig error] :Duplicate key %s", u));
        }, ConcurrentHashMap::new));
        sipServer.initSipServer(sipTimeoutEvent,sipRequestEventMap,sipResponseEventMap);
    }

}
