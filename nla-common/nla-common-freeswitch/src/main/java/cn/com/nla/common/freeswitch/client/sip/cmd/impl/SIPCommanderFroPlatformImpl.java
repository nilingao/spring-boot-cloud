package cn.com.nla.common.freeswitch.client.sip.cmd.impl;

import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.freeswitch.client.sip.cmd.SipSendMessage;
import cn.com.nla.common.freeswitch.client.sip.cmd.build.SIPRequestProvider;
import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import cn.com.nla.common.freeswitch.client.sip.utils.SipUtils;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.enums.sip.CharsetType;
import cn.com.nla.common.freeswitch.enums.sip.TransportType;
import cn.com.nla.common.freeswitch.enums.sip.VideoStreamType;
import cn.com.nla.common.freeswitch.model.bean.ConfigModel;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.sip.PlatformRegisterManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.SipTransactionManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.SsrcTransactionManager;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.AgentSubscribeHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.freeswitch.service.SipService;
import cn.com.nla.common.freeswitch.service.sip.ParentPlatformService;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.vo.sip.*;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.hutool.core.util.RandomUtil;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.List;

@Slf4j
@Component
public class SIPCommanderFroPlatformImpl implements SIPCommanderForPlatform {

    @Override
    public void unregister(SipServer sipServer, AgentVoInfo agentVoInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        register(sipServer, agentVoInfo, null, false, okEvent, errorEvent);
    }

    @Override
    public void register(SipServer sipServer, AgentVoInfo agentVoInfo, @Nullable WWWAuthenticateHeader www, boolean isRegister, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();
        ParentPlatformService parentPlatformService = SipService.getParentPlatformService();
        SipTransactionInfo sipTransactionInfo = sipTransactionManager.findParentPlatform(agentVoInfo.getAgentKey());
        ConfigModel configModel = parentPlatformService.random();
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        agentVoInfo.setFsHost(configModel.getRemoteIp());
        agentVoInfo.setFsPost(configModel.getInternalPort());
        SIPRequest request;
        String callId = null;
        String fromTag = SipUtils.getNewFromTag();
        if (sipTransactionInfo != null) {
            if (StringUtils.isNotEmpty(sipTransactionInfo.getCallId()))   callId  = sipTransactionInfo.getCallId();
            if (StringUtils.isNotEmpty(sipTransactionInfo.getFromTag())) fromTag = sipTransactionInfo.getFromTag();
        }
        PlatformRegisterManager platformRegisterManager = RedisService.getPlatformRegisterManager();
        if (www == null) {
            request = (SIPRequest) SIPRequestProvider.builder(sipServer, null, Request.REGISTER, null)
                    .createSipURI(agentVoInfo.getCalled(), String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()))
                    .addViaHeader(deviceSipConfig.getIp(), deviceSipConfig.getPort(), TransportType.UDP.getName(), true)
                    .createCallIdHeader(deviceSipConfig.getIp(), TransportType.UDP.getName(), callId)
                    .createFromHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), fromTag)
                    .createToHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), null)
                    .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                    .createUserAgentHeader()
                    .createContactHeader(agentVoInfo.getCalled(), String.format("%s:%s", deviceSipConfig.getIp(), deviceSipConfig.getPort()))
                    .createExpiresHeader(isRegister ? agentVoInfo.getExpires() : 0)
                    .buildRequest();
            platformRegisterManager.updatePlatformRegisterInfo(request.getCallIdHeader().getCallId(),
                    PlatformRegisterInfo.builder().agentKey(agentVoInfo.getAgentKey()).register(isRegister).build());
        } else {
            request = (SIPRequest) SIPRequestProvider.builder(sipServer, null, Request.REGISTER, null)
                    .createSipURI(agentVoInfo.getCalled(), String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()))
                    .addViaHeader(deviceSipConfig.getIp(), deviceSipConfig.getPort(), TransportType.UDP.getName(), true)
                    .createCallIdHeader(deviceSipConfig.getIp(), TransportType.UDP.getName(), callId)
                    .createFromHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), fromTag)
                    .createToHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), null)
                    .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                    .createUserAgentHeader()
                    .createContactHeader(agentVoInfo.getCalled(), String.format("%s:%s", deviceSipConfig.getIp(), deviceSipConfig.getPort()))
                    .createExpiresHeader(isRegister ? agentVoInfo.getExpires() : 0)
                    .createAuthorizationHeader(Request.REGISTER, agentVoInfo.getCalled(), agentVoInfo.getCalled(),
                            String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()), agentVoInfo.getPasswd(), www)
                    .buildRequest();
        }
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request, okEvent, error -> {
            log.info("向上级平台 [{}] {}发生错误：{}", agentVoInfo.getAgentKey(), isRegister ? "注册" : "注销", error.getMsg());
            if (isRegister) {
                platformRegisterManager.delPlatformRegisterInfo(request.getCallIdHeader().getCallId());
            }
            if (errorEvent != null) errorEvent.response(error);
        });
        if (isRegister) {
            if (agentVoInfo.getAgentState() == null) agentVoInfo.setAgentState(AgentStateEnum.LOGIN);
            RedisService.getAgentInfoManager().put(agentVoInfo);
        }
    }

    @Override
    public String keepalive(SipServer sipServer, AgentVoInfo agentVoInfo,
                             SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws SipException, InvalidArgumentException, ParseException {
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        String characterSet = CharsetType.GB2312.getName();
        StringBuffer keepaliveXml = new StringBuffer(200);
        keepaliveXml.append("<?xml version=\"1.0\" encoding=\"").append(characterSet).append("\"?>\r\n");
        keepaliveXml.append("<Notify>\r\n");
        keepaliveXml.append("<CmdType>Keepalive</CmdType>\r\n");
        keepaliveXml.append("<SN>").append((int) ((Math.random() * 9 + 1) * 100000)).append("</SN>\r\n");
        keepaliveXml.append("<DeviceID>").append(agentVoInfo.getCalled()).append("</DeviceID>\r\n");
        keepaliveXml.append("<Status>OK</Status>\r\n");
        keepaliveXml.append("</Notify>\r\n");
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, keepaliveXml.toString())
                .createSipURI(agentVoInfo.getCalled(), String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()))
                .addViaHeader(deviceSipConfig.getIp(), deviceSipConfig.getPort(), TransportType.UDP.getName(), true)
                .createCallIdHeader(deviceSipConfig.getIp(), TransportType.UDP.getName(), null)
                .createFromHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), SipUtils.getNewFromTag())
                .createToHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("application", "manscdp+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request, okEvent, errorEvent);
        return ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();
    }

    @Override
    public void streamByeCmd(SipServer sipServer, AgentVoInfo agentVoInfo,                              String audioStream, String videoStream, String callId, String typeName,
                              SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws SipException, InvalidArgumentException, ParseException {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();

        // 按 callId 查找事务（纯 SIP 模式下 stream 字段可能为空）
        SsrcTransaction ssrcTransaction = null;
        if (StringUtils.isNotEmpty(audioStream)) {
            ssrcTransaction = ssrcTransactionManager.getParamOne(agentVoInfo.getAgentKey(), callId, audioStream, typeName);
        }
        if (ssrcTransaction == null) {
            List<SsrcTransaction> all = ssrcTransactionManager.getParamAll(agentVoInfo.getAgentKey(), callId, null, null);
            if (all != null && !all.isEmpty()) ssrcTransaction = all.get(0);
        }
        if (ssrcTransaction == null) {
            log.info("[发送BYE] 未找到事务, agentKey: {}, callId: {}", agentVoInfo.getAgentKey(), callId);
            if (errorEvent != null) errorEvent.response(new EventResult<>(new RestResultEvent(RespCode.CODE_2.getValue(), "未找到通话事务")));
            return;
        }
        SipTransactionInfo sipTransactionInfo = ssrcTransaction.getSipTransactionInfo();
        if (sipTransactionInfo == null) {
            log.info("[发送BYE] SipTransactionInfo 为空, agentKey: {}, callId: {}", agentVoInfo.getAgentKey(), callId);
            if (errorEvent != null) errorEvent.response(new EventResult<>(new RestResultEvent(RespCode.CODE_2.getValue(), "未获取SIP事务信息")));
            return;
        }
        // 清理通话状态
        ssrcTransactionManager.remove(agentVoInfo.getAgentKey(), audioStream, callId, typeName);

        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
        Request request = SIPRequestProvider.builder(sipServer, CharsetType.GB2312.getName(), Request.BYE, null)
                .createSipURI(agentVoInfo.getCalled(), String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()))
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(agentVoInfo.getTransport()), false)
                .createFromHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), sipTransactionInfo.getFromTag())
                .createToHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), sipTransactionInfo.getToTag())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createCallIdHeader(null, null, sipTransactionInfo.getCallId())
                .createUserAgentHeader()
                .createContactHeader(sipConfigProperties.getId(), String.format("%s:%s", localIp, sipConfigProperties.getPort()))
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request,
                ok -> { if (okEvent != null) okEvent.response(ok); },
                error -> { if (errorEvent != null) errorEvent.response(error); });
    }

    @Override
    public SIPRequest presenceSubscribe(SipServer sipServer, AgentVoInfo agentVoInfo, SIPRequest requestOld,
                                        SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException {
        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
        Request request = SIPRequestProvider.builder(sipServer, null, Request.SUBSCRIBE, null)
                .createSipURI(agentVoInfo.getCalled(), String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()))
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.UDP.getName(), true)
                .createFromHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), SipUtils.getNewFromTag())
                .createToHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), null)
                .createCallIdHeader(sipConfigProperties.getIp(), TransportType.UDP.getName(),
                        requestOld == null ? null : requestOld.getCallIdHeader().getCallId())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContactHeader(agentVoInfo.getCalled(), String.format("%s:%s", localIp, sipConfigProperties.getPort()))
                .createExpiresHeader(600)
                .createEventHeader(null, "presence")
                .createAcceptHeader("application", "pidf+xml")
                .createContentTypeHeader("application", "pidf+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request, okEvent, errorEvent);
        return (SIPRequest) request;
    }

    @Override
    public SIPRequest callPhone(SipServer sipServer, AgentVoInfo agentVoInfo,
                                ProxyAuthenticateHeader header, SIPRequest sipRequest, SIPResponse response)
            throws InvalidArgumentException, SipException, ParseException {
        if (agentVoInfo == null || header == null) return null;
        SIPRequest request = (SIPRequest) SIPRequestProvider.builder(sipServer, null, Request.INVITE,
                        sipRequest.getContent().toString())
                .createSipURI(((SipURI) sipRequest.getToHeader().getAddress().getURI()).getUser(),
                        String.format("%s:%s", response.getRemoteAddress().getHostAddress(), response.getRemotePort()))
                .addViaHeader(sipRequest.getViaHost(), sipRequest.getViaPort(), TransportType.UDP.getName(), true)
                .createCallIdHeader(null, null, sipRequest.getCallId().getCallId())
                .createFromHeader(sipRequest.getFromHeader())
                .createToHeader(sipRequest.getToHeader())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("application", "sdp")
                .createProxyAuthenticateHeader(Request.INVITE, agentVoInfo.getSipPhone(),
                        SipUtils.getUserIdToHeader(sipRequest),
                        String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()),
                        agentVoInfo.getPasswd(), header)
                .createContactHeader(agentVoInfo.getCalled(),
                        String.format("%s:%s", sipRequest.getViaHost(), sipRequest.getViaPort()))
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request, null, null);
        return request;
    }

    /**
     * 纯 SIP 模式拨打电话，不依赖 ZLM。
     * SDP 由调用方构建，仅用 SsrcTransactionManager 记录 callId，挂机时通过 streamByeCmd 发送 BYE。
     */
    @Override
    public SIPRequest callPhoneSip(SipServer sipServer, AgentVoInfo agentVoInfo, String sdp, String caller,
                                   SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        if (agentVoInfo == null) return null;
        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
        SIPRequest request = (SIPRequest) SIPRequestProvider.builder(sipServer, null, Request.INVITE, sdp)
                .createSipURI(caller, String.format("%s:%s", agentVoInfo.getFsHost(), agentVoInfo.getFsPost()))
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.UDP.getName(), true)
                .createCallIdHeader(localIp, TransportType.getName(agentVoInfo.getTransport()), null)
                .createFromHeader(agentVoInfo.getCalled(), agentVoInfo.getFsHost(), SipUtils.getNewFromTag())
                .createToHeader(caller, agentVoInfo.getFsHost(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("application", "sdp")
                .createContactHeader(agentVoInfo.getCalled(), String.format("%s:%s", localIp, sipConfigProperties.getPort()))
                .createUserAgentHeader()
                .buildRequest();
        String callId = request.getCallId().getCallId();
        ssrcTransactionManager.put(agentVoInfo.getAgentKey(), callId, null, callId, null, null, null,
                VideoStreamType.CALL_AUDIO_PHONE.getCallName());
        SipSendMessage.sendMessage(sipServer, agentVoInfo, request,
                ok -> {
                    SIPMessage message = null;
                    if (ok.getEvent() instanceof ResponseEvent)
                        message = (SIPMessage) ((ResponseEvent) ok.getEvent()).getResponse();
                    else if (ok.getEvent() instanceof RequestEvent)
                        message = (SIPMessage) ((RequestEvent) ok.getEvent()).getRequest();
                    ssrcTransactionManager.put(agentVoInfo.getAgentKey(), callId, null, callId, null, null,
                            message, VideoStreamType.CALL_AUDIO_PHONE.getCallName());
                    if (okEvent != null) okEvent.response(ok);
                },
                error -> {
                    ssrcTransactionManager.remove(agentVoInfo.getAgentKey(), callId, callId, null);
                    if (errorEvent != null) errorEvent.response(error);
                });
        return request;
    }
}
