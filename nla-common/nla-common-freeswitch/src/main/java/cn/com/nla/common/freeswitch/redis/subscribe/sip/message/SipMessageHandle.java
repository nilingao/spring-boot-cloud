package cn.com.nla.common.freeswitch.redis.subscribe.sip.message;


import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.utils.SipLogUtils;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.vo.sip.Address;
import cn.com.nla.common.freeswitch.vo.sip.EventResult;
import cn.com.nla.common.freeswitch.vo.sip.MessageTypeVo;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.hutool.extra.spring.SpringUtil;
import cn.com.nla.common.json.utils.JsonUtils;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.stack.SIPClientTransactionImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.annotation.PostConstruct;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class SipMessageHandle {

    @PostConstruct
    public void subscribe() {
        RedisUtils.subscribe(SipConstant.VIDEO_SEND_SIP_MESSAGE, byte[].class, this::onMessage);
    }

    public void onMessage(byte[] body) {
        try {
            SipServer sipServer = SpringUtil.getBean(SipServer.class);
            MessageTypeVo vo = (MessageTypeVo) SerializationUtils.deserialize(body);
            if(MessageTypeVo.TypeEnum.SIP.getValue()== vo.getType()){
                if(!RedisService.getRegisterServerManager().isNotServerDevice(vo.getAgentKey())){
                    log.error("[SIP接收消息] [设备] 未获取注册地址 gbId : {}",vo.getAgentKey());
                    sendErrorMsg(sipServer, vo.getMessage(), String.format("未获取设备注册地址 国标编号 :%s",vo.getAgentKey()));
                    return;
                }
                Address address = RedisService.getRegisterServerManager().getDevice(vo.getAgentKey());
                if(address == null){
                    log.warn("[SIP接收消息] [设备] 在其他服务注册 gbId : {}",vo.getAgentKey());
                    return;
                }
                AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(vo.getAgentKey());
                if(agentVoInfo != null &&  sipServer.getSipConfigProperties().getIp().equals(address.getIp()) &&  sipServer.getSipConfigProperties().getPort() == address.getPort()){
                    String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
                    handleMessage(localIp,vo.getMessage());
                }
            }else if(MessageTypeVo.TypeEnum.SOCKET.getValue()== (vo.getType())){
                if(!RedisService.getRegisterServerManager().isNotPlatformDevice(vo.getAgentKey())){
                    log.error("[SIP接收消息] [国标级联] 未获取注册地址 gbId : {}",vo.getAgentKey());
                    sendErrorMsg(sipServer, vo.getMessage(), String.format("未获取国标级联注册地址 国标编号 :%s",vo.getAgentKey()));
                    return;
                }
                Address address = RedisService.getRegisterServerManager().getPlatform(vo.getAgentKey());
                if(address == null){
                    log.warn("[SIP接收消息] [设备] 在其他服务注册 gbId : {}",vo.getAgentKey());
                    return;
                }
                AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(vo.getAgentKey());
                if(agentVoInfo != null &&  sipServer.getSipConfigProperties().getIp().equals(address.getIp()) &&  sipServer.getSipConfigProperties().getPort() == address.getPort()){
                    String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
                    handleMessage(localIp,vo.getMessage());
                }
            }else {
                log.error("[SIP接收消息] 类型错误:{}", JsonUtils.toJsonString(vo));
                sendErrorMsg(sipServer, vo.getMessage(), String.format("消息类型错误 :%s",JsonUtils.toJsonString(vo)));
            }
        }catch (Exception e){
            log.error("[SIP接收消息] 发生错误:", e);
        }
    }
    /**
     * @param ip        发送端SIP IP
     * @param message   消息体
     * @throws SipException
     */
    public void handleMessage(String ip, Message message) throws SipException {
        SipServer sipServer = SpringUtil.getBean(SipServer.class);
        ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
        String transport = "UDP";
        if (viaHeader == null) {
            log.warn("[消息头缺失]： ViaHeader， 使用默认的UDP方式处理数据");
        } else {
            transport = viaHeader.getTransport();
        }
        //打印日志
        SipLogUtils.sendMessage(sipServer,message);
        if ("TCP".equals(transport)) {
            SipProviderImpl tcpSipProvider = sipServer.getTcpSipProvider(ip);
            if (tcpSipProvider == null) {
                log.error("[发送信息失败] 未找到tcp://{}的监听信息", ip);
                sendErrorMsg(sipServer, message, String.format("未找到tcp://%s的监听信息",ip));
                return;
            }
            sendSip(tcpSipProvider, message);
        } else if ("UDP".equals(transport)) {
            SipProviderImpl sipProvider = sipServer.getUdpSipProvider(ip);
            if (sipProvider == null) {
                log.error("[发送信息失败] 未找到udp://{}的监听信息", ip);
                sendErrorMsg(sipServer, message, String.format("未找到udp://%s的监听信息",ip));
                return;
            }
            sendSip(sipProvider, message);
        }
    }

    /**
     *  sip事务实现类 SIPClientTransactionImpl
     * 除过ACK以及BYE请求后 其他发生消息类型会开启事务sip
     * 开启事务后 如回复状态码为 300 <= statusCode && statusCode <= 699 后会自动回复ACK消息 代码在 SIPClientTransactionImpl 的903行
     */
    private static void sendSip(SipProviderImpl sipProvider, Message message) throws SipException {
        if (message instanceof Request) {
            List<String> methodList = Arrays.asList(Request.ACK,Request.BYE);
            if(methodList.contains(((SIPRequest)message).getMethod())){
                sipProvider.sendRequest((Request)message);
            }else {
                //开启sip事务
                SIPClientTransactionImpl newClientTransaction = (SIPClientTransactionImpl)sipProvider.getNewClientTransaction((Request) message);
                newClientTransaction.setRetransmitTimer(1000);//重试 与 超时 时间间隔 1000毫秒
                newClientTransaction.setTimerD(32000);//32秒未回应超时
                newClientTransaction.disableRetransmissionTimer();//取消重试机制 //目前机制 为 发送消息后 32秒未收到回应则触发 processTimeout 超时机制
                newClientTransaction.setTimerT2(-1000);//取消重试机制
                newClientTransaction.sendRequest();
            }
        } else if (message instanceof Response) {
            sipProvider.sendResponse((Response) message);
        }
    }

    private void sendErrorMsg(SipServer sipServer,Message message,String error){
        if(message == null){
            log.error("[发送信息失败] 发送错误消息时,未获取消息主体");
            return;
        }
        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        SipSubscribeHandle sipSubscribeHandle = sipServer.getSipSubscribeManager();
        List<SipSubscribeEvent> errorSubscribe = sipSubscribeHandle.getErrorSubscribe(callIdHeader.getCallId());
        if(errorSubscribe ==null || errorSubscribe.isEmpty()){
            return;
        }
        for (SipSubscribeEvent sipSubscribeEvent : errorSubscribe) {
            sipSubscribeEvent.response(new EventResult<RestResultEvent>(new RestResultEvent(RespCode.CODE_2.getValue(),error)));
        }
        sipSubscribeHandle.removeAllSubscribe(callIdHeader.getCallId());
    }

}
