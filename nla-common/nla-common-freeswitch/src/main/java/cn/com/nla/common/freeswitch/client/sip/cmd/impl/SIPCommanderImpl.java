package cn.com.nla.common.freeswitch.client.sip.cmd.impl;

import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommander;
import cn.com.nla.common.freeswitch.client.sip.cmd.SipSendMessage;
import cn.com.nla.common.freeswitch.client.sip.cmd.build.SIPRequestProvider;
import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import cn.com.nla.common.freeswitch.enums.sip.TransportType;
import cn.com.nla.common.freeswitch.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.sip.SsrcTransactionManager;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipMessageHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.vo.sip.EventResult;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import cn.com.nla.common.freeswitch.vo.sip.SsrcTransaction;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import javax.sdp.SdpParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.message.Request;
import java.text.ParseException;

/**
 * @description:设备能力接口
 */

@Slf4j
@Component
public class SIPCommanderImpl implements SIPCommander {

    @Resource
    private SipMessageHandle sipMessageHandle;

    @Override
    public void streamByeCmd(SipServer sipServer, AgentVoInfo agentVoInfo, String stream, String callId, String typeName, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        SsrcTransaction ssrcTransaction = ssrcTransactionManager.getParamOne(agentVoInfo.getAgentKey(), callId, stream, typeName);
        if (ssrcTransaction == null) {
            log.info("[视频流停止] 未找到视频流信息，设备：{}, 流ID: {}", agentVoInfo.getDeviceId(), stream);
            if (errorEvent != null) {
                errorEvent.response(new EventResult<>(new RestResultEvent(RespCode.CODE_2.getValue(), "未找到视频流信息")));
            }
            return;
        }
        SipTransactionInfo sipTransactionInfo = ssrcTransaction.getSipTransactionInfo();
        if (sipTransactionInfo == null) {
            log.info("[视频流停止] 当前流未请求成功，无法关闭，设备：{}, 流ID: {}", agentVoInfo.getDeviceId(), stream);
            if (errorEvent != null) {
                errorEvent.response(new EventResult<>(new RestResultEvent(RespCode.CODE_2.getValue(), "当前流未请求成功，无法关闭")));
            }
            return;
        }
        // 清理通话状态（纯 SIP 模式不需要 ZLM SSRC 释放）
        ssrcTransactionManager.remove(agentVoInfo.getAgentKey(), ssrcTransaction.getStream());
        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
        Request request = SIPRequestProvider.builder(sipServer, null, Request.BYE,null)
                .createSipURI(agentVoInfo.getCalled(), agentVoInfo.getRemoteAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(),TransportType.UDP.getName(), false)
                .createFromHeader(agentVoInfo.getCalled(), sipConfigProperties.getIp(), sipTransactionInfo.getFromTag())
                .createToHeader(agentVoInfo.getCalled(), agentVoInfo.getRemoteAddress(), sipTransactionInfo.getToTag())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createCallIdHeader(null,null,sipTransactionInfo.getCallId())
                .createUserAgentHeader()
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request, ok->{
            if(okEvent!= null){
                okEvent.response(ok);
            }
        },error->{
            if(errorEvent!= null){
                errorEvent.response(error);
            }
        });
    }

    @Override
    public void sendAckMessage(SipServer sipServer, SIPResponse response, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SdpParseException {

        Request reqAck = SIPRequestProvider.builder(sipServer, null, Request.ACK, null)
                .createSipURI(((SipURI) response.getToHeader().getAddress().getURI()).getUser(), response.getRemoteAddress().getHostAddress() + ":" + response.getRemotePort(),"udp")
                .addViaHeader(response.getLocalAddress().getHostAddress(), response.getTopmostViaHeader().getPort(), response.getTopmostViaHeader().getTransport(), true)
                .createCallIdHeader(response.getCallIdHeader())
                .createFromHeader(response.getFromHeader())
                .createToHeader(response.getToHeader())
                .createCSeqHeader(response.getCSeq().getSeqNumber())
                .createContactHeader(((SipURI) response.getFromHeader().getAddress().getURI()).getUser(),String.format("%s:%s",response.getLocalAddress().getHostAddress(), response.getLocalPort()))
//                .createUserAgentHeader()
                .buildRequest();
        log.info("[回复ack] {}-> {}:{} ", ((SipURI) response.getFromHeader().getAddress().getURI()).getUser(), response.getRemoteAddress().getHostAddress(), response.getRemotePort());
        SipSendMessage.handleSipEvent(sipServer,response.getCallIdHeader().getCallId(),okEvent,errorEvent);
        sipMessageHandle.handleMessage(response.getLocalAddress().getHostAddress(),reqAck);
    }
}
