package cn.com.nla.common.freeswitch.client.sip;


import cn.com.nla.common.freeswitch.client.sip.listener.SipListenerImpl;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.response.SipResponseEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.timeout.SipTimeoutEvent;
import cn.com.nla.common.freeswitch.client.sip.properties.DefaultSipProperties;
import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import cn.com.nla.common.freeswitch.client.sip.properties.VideoProperties;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.AgentSubscribeHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.freeswitch.vo.sip.Address;
import cn.com.nla.common.freeswitch.vo.sip.GbStringMsgParserFactory;
import cn.hutool.core.net.NetUtil;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import javax.sip.*;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信令服务器中心
 */
@Slf4j
@Component
public class SipServer {
    private final SipConfigProperties sipConfigProperties;
    private final VideoProperties videoProperties;
    /**
     * 信令服务器工厂
     */
    private final SipFactory sipFactory;
    /**
     * SIP信令处理类事件
     */
    private final SipListener sipListener;
    /**
     * 订阅工厂
     */
    private final SipSubscribeHandle sipSubscribeHandle;
    /**
     * 订阅工厂
     */
    private final AgentSubscribeHandle agentSubscribeHandle;
    /**
     * tcp实现
     */
    private final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
    /**
     * utp实现
     */
    private final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();

    public SipServer(SipConfigProperties sipConfigProperties, SipSubscribeHandle sipSubscribeHandle,AgentSubscribeHandle agentSubscribeHandle, VideoProperties videoProperties){
        this.sipListener = new SipListenerImpl(this);
        this.sipSubscribeHandle = sipSubscribeHandle;
        this.agentSubscribeHandle = agentSubscribeHandle;
        this.sipConfigProperties = sipConfigProperties;
        this.videoProperties = videoProperties;
        this.sipFactory = SipFactory.getInstance();
    }
    /**
     * 创建信令服务器
     */
    public void initSipServer(SipTimeoutEvent sipTimeoutEvent, ConcurrentHashMap<String, SipRequestEvent> sipRequestEventMap, ConcurrentHashMap<String, SipResponseEvent> sipResponseEventMap){
        //初始化 SipListener 中 SipServer
        ((SipListenerImpl)sipListener).init(sipTimeoutEvent, sipRequestEventMap,sipResponseEventMap);
        //判断 sipConfigProperties 中ip是否有值若没有则取本机ip
        if(StringUtils.isEmpty(sipConfigProperties.getIp())){
            sipConfigProperties.setIp(NetUtil.getLocalhostStr());
        }
        addListeningPoint(sipConfigProperties.getId(),sipConfigProperties.getIp(), sipConfigProperties.getPort());
    }


    /**
     * 创建开始
     * @param monitorIp
     * @param port
     */
    private void addListeningPoint(String gbId,String monitorIp, int port){
        SipStackImpl sipStack;
        try {
            sipStack = (SipStackImpl)sipFactory.createSipStack(DefaultSipProperties.getProperties(monitorIp, videoProperties.getSipLog()));
            sipStack.setMessageParserFactory(new GbStringMsgParserFactory());
        } catch (PeerUnavailableException e) {
            log.error("[Sip Server] SIP服务启动失败， 监听地址{}失败,请检查ip是否正确", monitorIp);
            return;
        }

        try {
            ListeningPoint tcpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "TCP");
            SipProviderImpl tcpSipProvider = (SipProviderImpl)sipStack.createSipProvider(tcpListeningPoint);
            tcpSipProvider.setDialogErrorsAutomaticallyHandled();
            tcpSipProvider.addSipListener(sipListener);
            tcpSipProviderMap.put(monitorIp, tcpSipProvider);
            log.info("[Sip Server] tcp://{}:{} 启动成功", monitorIp, port);
        } catch (TransportNotSupportedException
                 | TooManyListenersException
                 | ObjectInUseException
                 | InvalidArgumentException e) {
            log.error("[Sip Server] tcp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
                    , monitorIp, port);
        }

        try {
            ListeningPoint udpListeningPoint = sipStack.createListeningPoint(monitorIp, port, "UDP");
            SipProviderImpl udpSipProvider = (SipProviderImpl)sipStack.createSipProvider(udpListeningPoint);
            udpSipProvider.addSipListener(sipListener);
            udpSipProviderMap.put(monitorIp, udpSipProvider);

            log.info("[Sip Server] udp://{}:{} 启动成功", monitorIp, port);
        } catch (TransportNotSupportedException
                 | TooManyListenersException
                 | ObjectInUseException
                 | InvalidArgumentException e) {
            log.error("[Sip Server] udp://{}:{} SIP服务启动失败,请检查端口是否被占用或者ip是否正确"
                    , monitorIp, port);
        }
        //注册成功后加入缓存备用
        RedisService.getRegisterServerManager().putSip(gbId, Address.builder().agentKey(gbId).ip(monitorIp).port(port).build());
    }

    @PreDestroy
    private void clean(){
        //服务注销时清掉
        RedisService.getRegisterServerManager().delSip(sipConfigProperties.getId());
    }

    public SipListener getSipListener() {
        return sipListener;
    }

    public SipFactory getSipFactory() {
        return sipFactory;
    }

    public SipProviderImpl getUdpSipProvider(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return null;
        }
        return udpSipProviderMap.get(ip);
    }

    public SipProviderImpl getUdpSipProvider() {
        if (udpSipProviderMap.size() != 1) {
            return null;
        }
        return udpSipProviderMap.values().stream().findFirst().get();
    }

    public SipProviderImpl getTcpSipProvider() {
        if (tcpSipProviderMap.size() != 1) {
            return null;
        }
        return tcpSipProviderMap.values().stream().findFirst().get();
    }

    public SipProviderImpl getTcpSipProvider(String ip) {
        if (StringUtils.isEmpty(ip)) {
            return null;
        }
        return tcpSipProviderMap.get(ip);
    }

    public String getLocalIp(String localIp) {
        if (StringUtils.isNotEmpty(localIp) && getUdpSipProvider(localIp) != null) {
            return localIp;
        }
        return getUdpSipProvider().getListeningPoint().getIPAddress();
    }

    public SipSubscribeHandle getSipSubscribeManager(){
        return this.sipSubscribeHandle;
    }

    public AgentSubscribeHandle getAgentSubscribeManager(){
        return this.agentSubscribeHandle;
    }

    public SipConfigProperties getSipConfigProperties(){
        return sipConfigProperties;
    }

    public VideoProperties getVideoProperties(){return videoProperties;}
}
