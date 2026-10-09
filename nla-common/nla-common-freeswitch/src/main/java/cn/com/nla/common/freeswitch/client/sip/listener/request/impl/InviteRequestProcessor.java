package cn.com.nla.common.freeswitch.client.sip.listener.request.impl;

import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.client.sip.cmd.SipSendMessage;
import cn.com.nla.common.freeswitch.client.sip.listener.request.AbstractSipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.utils.SipUtils;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.enums.fs.LoginTypeEnum;
import cn.com.nla.common.freeswitch.enums.sip.VideoStreamType;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.model.notice.CallMessage;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.freeswitch.service.FsService;
import cn.com.nla.common.freeswitch.vo.sip.DeviceRawContent;
import cn.com.nla.common.redis.utils.RedisUtils;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 处理 FreeSWITCH 发来的 SIP INVITE（呼叫坐席）。
 * 纯 SIP 模式：不再依赖 ZLM，通过 Socket.IO 通知坐席来电，
 * 坐席接听后用最小 SDP 响应 200 OK，媒体由 FS 与坐席 SIP 设备直接协商。
 */
@Slf4j
@Component
public class InviteRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {

    @Resource
    private DynamicTask dynamicTask;

    @Override
    public String getMethod() { return Request.INVITE; }

    @Override
    public void process(RequestEvent evt) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        try {
            responseAck(request, Response.TRYING, null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[INVITE TRYING 失败] {}", e.getMessage());
        }

        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        String formUserId = SipUtils.getUserIdFromHeader(request);
        if (formUserId == null) {
            sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "未获取主动拨打坐席编号");
            return;
        }
        CallInfo callInfo = RedisService.getCallInfoManager().findCaller(formUserId);
        if (callInfo == null) {
            sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "未获取客服拨打信息");
            return;
        }
        String toUserId = SipUtils.getUserIdToHeader(request);
        if (toUserId == null) {
            sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "未获取坐席编号");
            return;
        }
        AgentVoInfo toAgentVoInfo = RedisService.getAgentInfoManager().getCompanyAgentId(callInfo.getCompanyId(), toUserId);
        if (toAgentVoInfo == null) {
            sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "未获取拨打坐席信息");
            return;
        } else if (toAgentVoInfo.getAgentState() != AgentStateEnum.READY) {
            try {
                responseAck(request, Response.TEMPORARILY_UNAVAILABLE,
                        String.format("拨打客服%s中，请稍后再拨",
                                toAgentVoInfo.getAgentState() == null ? "未正常登录" : toAgentVoInfo.getAgentState().getName()));
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[INVITE TRYING 失败] {}", e.getMessage());
            }
            return;
        }
        if (LoginTypeEnum.getLoginType(toAgentVoInfo.getLoginType()) == null) {
            sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "未获取登陆方式");
            return;
        }
        DeviceRawContent deviceRawContent = SipUtils.handleDeviceRawContent(request);
        if (deviceRawContent == null) {
            sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "未解析出 DeviceRawContent");
            log.error("[INVITE] agentKey: {} 未解析出 DeviceRawContent", toAgentVoInfo.getAgentKey());
            return;
        }

        // 每隔 1 秒向坐席推送来电通知
        String sentAgentInviteKey = String.format("SENT_AGENT_INVITE_%s", callIdHeader.getCallId());
        dynamicTask.startCron(sentAgentInviteKey, 0, 1, () ->
                FsService.getSendAgentMessage().sendMessage(AgentStateEnum.IN_CALL_RING, toAgentVoInfo,
                        CallMessage.builder()
                                .callId(callIdHeader.getCallId())
                                .onVideo(deviceRawContent.getVideoInfo() != null
                                        ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
                                .direction(callInfo.getDirection())
                                .callType(callInfo.getCallType())
                                .caller(callInfo.getCallerDisplay())
                                .called(callInfo.getCalledDisplay())
                                .groupId(callInfo.getGroupId())
                                .build()));

        // 15 秒未接听则超时挂断
        String timeoutKey = String.format("INVITE_REQUEST_%s", callIdHeader.getCallId());
        AgentVoInfo finalToAgentVoInfo = toAgentVoInfo;
        dynamicTask.startDelay(timeoutKey, 15, () -> {
            log.warn("[INVITE 超时] 断开电话，向客服发送挂断");
            try {
                responseAck(request, Response.TEMPORARILY_UNAVAILABLE, "接听电话超时，电话挂断");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[INVITE 超时 BYE 失败] {}", e.getMessage());
            }
            FsService.getSendAgentMessage().sendErrorMessage(finalToAgentVoInfo, "接听电话超时，电话挂断");
            dynamicTask.stop(sentAgentInviteKey);
        });

        SipSendMessage.handleAgentEvent(sipServer, callIdHeader.getCallId(),
                ok -> {
                    dynamicTask.stop(timeoutKey);
                    dynamicTask.stop(sentAgentInviteKey);
                    try {
                        responseAck(request, Response.RINGING, null);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[INVITE RINGING 失败] {}", e.getMessage());
                        String key = String.format("%s%s",
                                SipSubscribeHandle.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, callIdHeader.getCallId());
                        RedisUtils.publish(key, SerializationUtils.serialize(evt));
                        FsService.getSendAgentMessage().sendErrorMessage(finalToAgentVoInfo, "[INVITE RINGING 失败]");
                        return;
                    }
                    try {
                        inviteHandle(deviceRawContent, request, callIdHeader, finalToAgentVoInfo);
                    } catch (Exception e) {
                        sendErrorMessage(request, Response.TEMPORARILY_UNAVAILABLE, "拨打电话时报错");
                        String key = String.format("%s%s",
                                SipSubscribeHandle.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, callIdHeader.getCallId());
                        RedisUtils.publish(key, SerializationUtils.serialize(evt));
                        FsService.getSendAgentMessage().sendErrorMessage(finalToAgentVoInfo, e.getMessage());
                        return;
                    }
                    String key = String.format("%s%s",
                            SipSubscribeHandle.VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER, callIdHeader.getCallId());
                    RedisUtils.publish(key, SerializationUtils.serialize(evt));
                },
                error -> {
                    dynamicTask.stop(timeoutKey);
                    dynamicTask.stop(sentAgentInviteKey);
                    try {
                        responseAck(request, Response.DECLINE, "客服挂断电话");
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[INVITE DECLINE 失败] {}", e.getMessage());
                    }
                });
    }

    /**
     * 坐席接听后发送200 OK + 最小 SDP。
     * 纯 SIP 模式：媒体由 FreeSWITCH 与坐席 SIP 设备直接协商，不经过 ZLM。
     */
    private void inviteHandle(DeviceRawContent deviceRawContent, SIPRequest request,
                              CallIdHeader callIdHeader, AgentVoInfo agentVoInfo) throws SipException {
        String localIp = sipServer.getLocalIp(request.getRemoteAddress().getHostAddress());
        String sdp = buildMinimalSdp(localIp, deviceRawContent);
        try {
            responseSdpAck(request, sdp, agentVoInfo);
        } catch (InvalidArgumentException | ParseException e) {
            sendErrorExceptionMessage(String.format("[SIP 响应失败] agentKey: %s 错误: %s",
                    agentVoInfo.getAgentKey(), e.getMessage()));
        }
    }

    private static String buildMinimalSdp(String localIp, DeviceRawContent deviceRawContent) {
        StringBuilder sdp = new StringBuilder();
        sdp.append("v=0\r\n");
        sdp.append("o=- 0 0 IN IP4 ").append(localIp).append("\r\n");
        sdp.append("s=SIP Call\r\n");
        sdp.append("c=IN IP4 ").append(localIp).append("\r\n");
        sdp.append("t=0 0\r\n");
        sdp.append("m=audio 0 RTP/AVP 0 8 101\r\n");
        sdp.append("a=rtpmap:0 PCMU/8000\r\n");
        sdp.append("a=rtpmap:8 PCMA/8000\r\n");
        sdp.append("a=rtpmap:101 telephone-event/8000\r\n");
        sdp.append("a=sendrecv\r\n");
        if (deviceRawContent != null && deviceRawContent.getVideoInfo() != null) {
            sdp.append("m=video 0 RTP/AVP 98\r\n");
            sdp.append("a=rtpmap:98 H264/90000\r\n");
            sdp.append("a=sendrecv\r\n");
        }
        return sdp.toString();
    }

    private void sendErrorMessage(SIPRequest request, Integer status, String message) {
        log.warn("[INVITE 错误] {}，返回: {}", message, status);
        try {
            responseAck(request, Response.TEMPORARILY_UNAVAILABLE, message);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[INVITE BAD_REQUEST 失败] {}", e.getMessage());
        }
    }

    private void sendErrorExceptionMessage(String message) throws SipException {
        log.error(message);
        throw new SipException(message);
    }
}
