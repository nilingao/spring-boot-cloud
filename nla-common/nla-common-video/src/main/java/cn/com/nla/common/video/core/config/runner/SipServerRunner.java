package cn.com.nla.common.video.core.config.runner;

import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.response.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.timeout.SipTimeoutEvent;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Order(20)
public class SipServerRunner implements CommandLineRunner {

    @Autowired
    private SipServer sipServer;
    @Autowired
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
