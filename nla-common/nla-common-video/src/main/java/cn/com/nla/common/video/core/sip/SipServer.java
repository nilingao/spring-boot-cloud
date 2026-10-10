package cn.com.nla.common.video.core.sip;

import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.core.pool.sip.GbStringMsgParserFactory;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.sip.listener.SipListenerImpl;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.response.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.timeout.SipTimeoutEvent;
import cn.com.nla.common.video.core.sip.properties.DefaultSipProperties;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.SipStackImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.PreDestroy;
import javax.sip.*;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 信令服务器中心
 */
@Slf4j
public class SipServer implements AutoCloseable {

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
    private SipStackImpl sipStack;
    private boolean registered;
    /**
     * tcp实现
     */
    private final Map<String, SipProviderImpl> tcpSipProviderMap = new ConcurrentHashMap<>();
    /**
     * utp实现
     */
    private final Map<String, SipProviderImpl> udpSipProviderMap = new ConcurrentHashMap<>();

    public SipServer(SipConfigProperties sipConfigProperties, SipSubscribeHandle sipSubscribeHandle, VideoProperties videoProperties){
        this.sipListener = new SipListenerImpl(this);
        this.sipSubscribeHandle = sipSubscribeHandle;
        this.sipConfigProperties = sipConfigProperties;
        this.videoProperties = videoProperties;
        this.sipFactory = SipFactory.getInstance();
    }
    /**
     * 创建信令服务器
     */
    public void initSipServer(SipTimeoutEvent sipTimeoutEvent, ConcurrentHashMap<String, SipRequestEvent> sipRequestEventMap, ConcurrentHashMap<String, SipResponseEvent> sipResponseEventMap){
        //初始化 SipListener 中 SipServer
        ((SipListenerImpl)sipListener).init(sipTimeoutEvent,sipRequestEventMap,sipResponseEventMap);
        if (sipStack != null) throw new IllegalStateException("Video SIP server is already running");
        if (StringUtils.isBlank(sipConfigProperties.getId()) || StringUtils.isBlank(sipConfigProperties.getDomain()) ||
            sipConfigProperties.getPort() == null || sipConfigProperties.getPort() < 1 || sipConfigProperties.getPort() > 65535) {
            throw new IllegalArgumentException("video.sip.id, domain and a valid port are required");
        }
        // Default the advertised address independently of wildcard binding.
        if(StringUtils.isEmpty(sipConfigProperties.getAdvertisedIp())){
            sipConfigProperties.setAdvertisedIp(cn.hutool.core.net.NetUtil.getLocalhostStr());
        }
        addListeningPoint(sipConfigProperties.getId(),getBindIp(), sipConfigProperties.getPort());
    }


    /**
     * 创建开始
     * @param monitorIp
     * @param port
     */
    private void addListeningPoint(String gbId,String monitorIp, int port){
        try {
            // SipFactory reuses stacks by IP even with different names. Own this stack so
            // stopping video never stops a co-located FreeSWITCH SIP transport.
            sipStack = new SipStackImpl(DefaultSipProperties.getProperties(monitorIp, videoProperties.getSipLog()));
            sipStack.setMessageParserFactory(new GbStringMsgParserFactory());
        } catch (PeerUnavailableException e) {
            throw new IllegalStateException("Unable to create video SIP stack for " + monitorIp, e);
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
            close();
            throw new IllegalStateException("Unable to bind video SIP TCP " + monitorIp + ":" + port, e);
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
            close();
            throw new IllegalStateException("Unable to bind video SIP UDP " + monitorIp + ":" + port, e);
        }
        //注册成功后加入缓存备用
        RedisService.getRegisterServerManager().putSip(gbId, Address.builder().gbId(gbId).ip(getAdvertisedIp(monitorIp)).port(port).build());
        registered = true;
    }

    @PreDestroy
    @Override
    public void close(){
        if (sipStack != null) {
            sipStack.stop();
            sipStack = null;
        }
        tcpSipProviderMap.clear();
        udpSipProviderMap.clear();
        if (registered) {
            registered = false;
            RedisService.getRegisterServerManager().delSip(sipConfigProperties.getId());
        }
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
        SipProviderImpl udpSipProvider = getUdpSipProvider();
        if (udpSipProvider == null) {
            return getBindIp();
        }
        return udpSipProvider.getListeningPoint().getIPAddress();
    }

    public String getBindIp() {
        if (StringUtils.isNotEmpty(sipConfigProperties.getBindIp())) {
            return sipConfigProperties.getBindIp();
        }
        return cn.hutool.core.net.NetUtil.getLocalhostStr();
    }

    public String getAdvertisedIp(String localIp) {
        if (StringUtils.isNotEmpty(sipConfigProperties.getAdvertisedIp())) {
            return sipConfigProperties.getAdvertisedIp();
        }
        if (StringUtils.isNotEmpty(localIp)) {
            return localIp;
        }
        return getBindIp();
    }

    public String getAdvertisedAddress(String address) {
        if (StringUtils.isEmpty(address)) {
            return address;
        }
        int portIndex = address.lastIndexOf(':');
        if (portIndex <= 0 || portIndex == address.length() - 1) {
            return getAdvertisedIp(address);
        }
        String host = address.substring(0, portIndex);
        String port = address.substring(portIndex + 1);
        return String.format("%s:%s", getAdvertisedIp(host), port);
    }

    public SipSubscribeHandle getSubscribeManager(){
        return this.sipSubscribeHandle;
    }

    public SipConfigProperties getSipConfigProperties(){
        return sipConfigProperties;
    }

    public VideoProperties getVideoProperties(){return videoProperties;}
}
