package cn.com.nla.common.video.core.sip.cmd.impl;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.enums.*;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.basic.vo.media.HookVo;
import cn.com.nla.common.video.basic.vo.sip.SSRCInfo;
import cn.com.nla.common.video.basic.vo.video.DeviceAlarmVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.model.RestResultEvent;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.redis.subscribe.media.HookEvent;
import cn.com.nla.common.video.core.redis.subscribe.media.HookKeyFactory;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipMessageHandle;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.MediaServerVoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.sip.cmd.SipSendMessage;
import cn.com.nla.common.video.core.sip.cmd.build.SIPRequestProvider;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.message.Request;
import java.text.ParseException;

/**
 * @description:设备能力接口，用于定义设备的控制、查询能力
 * @author TZY
 * @date: 2020年5月3日 下午9:22:48
 */

@Slf4j
public class SIPCommanderImpl implements SIPCommander {
    @Autowired
    private MediaHookSubscribe mediaHookSubscribe;
    @Autowired
    private SipMessageHandle sipMessageHandle;

    /**
     * 云台缩放控制
     *
     * @param deviceVo    控制设备
     * @param channelId 预览通道
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     * @param zoomSpeed 镜头缩放速度
     */
    @Override
    public void ptzZoomCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, int inOut, int zoomSpeed, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        ptzCmd(sipServer, deviceVo, channelId, 0, 0, inOut, 0, zoomSpeed,okEvent,errorEvent);
    }

    /**
     * 云台指令码计算
     *
     * @param cmdCode      指令码
     * @param parameter1   数据1
     * @param parameter2   数据2
     * @param combineCode2 组合码2
     */
    public static String frontEndCmdString(int cmdCode, int parameter1, int parameter2, int combineCode2, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) {
        StringBuilder builder = new StringBuilder("A50F01");
        String strTmp;
        strTmp = String.format("%02X", cmdCode);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter1);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", parameter2);
        builder.append(strTmp, 0, 2);
        strTmp = String.format("%02X", combineCode2 << 4);
        builder.append(strTmp, 0, 2);
        //计算校验码
        int checkCode = (0XA5 + 0X0F + 0X01 + cmdCode + parameter1 + parameter2 + (combineCode2 << 4)) % 0X100;
        strTmp = String.format("%02X", checkCode);
        builder.append(strTmp, 0, 2);
        return builder.toString();
    }

    /**
     * 云台控制，支持方向与缩放控制
     *
     * @param deviceVo    控制设备
     * @param channelId 预览通道
     * @param leftRight 镜头左移右移 0:停止 1:左移 2:右移
     * @param upDown    镜头上移下移 0:停止 1:上移 2:下移
     * @param inOut     镜头放大缩小 0:停止 1:缩小 2:放大
     * @param moveSpeed 镜头移动速度
     * @param zoomSpeed 镜头缩放速度
     */
    @Override
    public void ptzCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, int leftRight, int upDown, int inOut, int moveSpeed,
                       int zoomSpeed, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String cmdStr = SipUtils.cmdString(leftRight, upDown, inOut, moveSpeed, zoomSpeed);
        StringBuilder ptzXml = new StringBuilder(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, ptzXml.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo,request,okEvent,errorEvent);
    }

    /**
     * 前端控制，包括PTZ指令、FI指令、预置位指令、巡航指令、扫描指令和辅助开关指令
     *
     * @param deviceVo       控制设备
     * @param channelId    预览通道
     * @param cmdCode      指令码
     * @param parameter1   数据1
     * @param parameter2   数据2
     * @param combineCode2 组合码2
     */
    @Override
    public void frontEndCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, int cmdCode, int parameter1, int parameter2, int combineCode2, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {

        String cmdStr = frontEndCmdString(cmdCode, parameter1, parameter2, combineCode2,okEvent,errorEvent);
        StringBuffer ptzXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdStr + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, ptzXml.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo,request,okEvent,errorEvent);

    }

    /**
     * 前端控制指令（用于转发上级指令）
     *
     * @param deviceVo    控制设备
     * @param channelId 预览通道
     * @param cmdString 前端控制指令串
     */
    @Override
    public void fronEndCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String cmdString, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer ptzXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        ptzXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        ptzXml.append("<Control>\r\n");
        ptzXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        ptzXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        ptzXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        ptzXml.append("<PTZCmd>" + cmdString + "</PTZCmd>\r\n");
        ptzXml.append("<Info>\r\n");
        ptzXml.append("<ControlPriority>5</ControlPriority>\r\n");
        ptzXml.append("</Info>\r\n");
        ptzXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, ptzXml.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("APPLICATION", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
       SipSendMessage.sendMessage(sipServer,deviceVo,request,okEvent,errorEvent);

    }

    /**
     * 请求预览视频流
     * @param deviceVo     视频设备
     * @param channelId  预览通道
     */
    @Override
    public void playStreamCmd(SipServer sipServer, MediaServerVo mediaServerVoItem, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, boolean isSeniorSdp, HookEvent hookEvent, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        String stream = ssrcInfo.getStream();
        if (deviceVo == null) {
            return;
        }
        log.info("{} 分配的ZLM为: {} [{}:{}]", stream, mediaServerVoItem.getId(), mediaServerVoItem.getSdpIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(deviceVo.getSdpIp())) {
            sdpIp = deviceVo.getSdpIp();
        }else {
            sdpIp = mediaServerVoItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Play\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=0 0\r\n");

        if (isSeniorSdp) {
            if ("TCP-PASSIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(StreamModeType.getName(deviceVo.getStreamMode()))) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }
        if("TP-LINK".equals(deviceVo.getManufacturer())){
            if (deviceVo.getSwitchPrimarySubStream() == ConstEnum.Flag.YES.getValue()){
                content.append("a=streamMode:sub\r\n");
            }else {
                content.append("a=streamMode:main\r\n");
            }
        }else {
            if (deviceVo.getSwitchPrimarySubStream() == ConstEnum.Flag.YES.getValue()){
                content.append("a=streamprofile:1\r\n");
            }else {
                content.append("a=streamprofile:0\r\n");
            }
        }

        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        // f字段:f= v/编码格式/分辨率/帧率/码率类型/码率大小a/编码格式/码率大小/采样率
        // content.append("f=v/2/5/25/1/4000a/1/8/1" + "\r\n"); // 未发现支持此特性的设备
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        SIPRequest request = (SIPRequest)SIPRequestProvider.builder(sipServer, null, Request.INVITE, content.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("APPLICATION", "SDP")
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .createSubjectHeader(channelId,ssrcInfo.getSsrc(), sipConfigProperties.getId())
                .createUserAgentHeader()
                .buildRequest();
        String callId = request.getCallId().getCallId();
        if(hookEvent != null){
            HookKey hookKey = HookKeyFactory.onStreamChanged("rtp", stream, true, "rtsp", mediaServerVoItem.getId());
            mediaHookSubscribe.addSubscribe(hookKey,(MediaServerVo mediaServerVo, HookVo response)->{
                response.setCallId(callId);
                hookEvent.response(mediaServerVo,response);
                mediaHookSubscribe.removeSubscribe(hookKey);
            });
        }
        SipSendMessage.sendMessage(sipServer,deviceVo, request,(ok)->{
            ResponseEvent event = (ResponseEvent) ok.getEvent();
            SIPResponse response = (SIPResponse) event.getResponse();
            // 这里为例避免一个通道的点播多次点播只有一个callID这个参数使用一个固定值
            ssrcTransactionManager.put(deviceVo.getDeviceId(),channelId,callId,"rtp",stream, ssrcInfo.getSsrc(), mediaServerVoItem.getId(),response, VideoStreamType.play);
            okEvent.response(ok);
        },(error)->{
            ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream(),callId,null);
            ssrcConfigManager.releaseSsrc(mediaServerVoItem.getId(),ssrcInfo.getSsrc());
            errorEvent.response(error);
        });
    }

    /**
     * 请求回放视频流
     *
     * @param deviceVo    视频设备
     * @param channelId 预览通道
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void playbackStreamCmd(SipServer sipServer, MediaServerVo mediaServerVoItem, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, String startTime, String endTime, boolean isSeniorSdp, HookEvent hookEvent, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        log.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerVoItem.getId(), mediaServerVoItem.getIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(deviceVo.getSdpIp())) {
            sdpIp = deviceVo.getSdpIp();
        }else {
            sdpIp = mediaServerVoItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Playback\r\n");
        content.append("u=" + channelId + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" + DateUtil.parse(startTime).getTime()/1000L + " "
                + DateUtil.parse(endTime).getTime()/1000L + "\r\n");

        String streamMode = StreamModeType.getName(deviceVo.getStreamMode());

        if (isSeniorSdp) {
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equalsIgnoreCase(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equalsIgnoreCase(streamMode)) {
                // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equalsIgnoreCase(streamMode)) {
                // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }

        //ssrc
        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");


        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        SIPRequest request = (SIPRequest)SIPRequestProvider.builder(sipServer, null, Request.INVITE, content.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("APPLICATION", "SDP")
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .createSubjectHeader(channelId,ssrcInfo.getSsrc(), sipConfigProperties.getId())
                .createUserAgentHeader()
                .buildRequest();
        //添加回放点播成功订阅
        String callId = request.getCallId().getCallId();
        if(hookEvent != null){
            HookKey hookKey = HookKeyFactory.onStreamChanged("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerVoItem.getId());
            mediaHookSubscribe.addSubscribe(hookKey,(MediaServerVo mediaServerVo, HookVo response)->{
                response.setCallId(callId);
                hookEvent.response(mediaServerVo,response);
                mediaHookSubscribe.removeSubscribe(hookKey);
            });
        }

        SipSendMessage.sendMessage(sipServer,deviceVo, request,(ok)->{
            ResponseEvent event = (ResponseEvent) ok.getEvent();
            SIPResponse response = (SIPResponse) event.getResponse();
            // 这里为例避免一个通道的点播只有一个callID这个参数使用一个固定值
            ssrcTransactionManager.put(deviceVo.getDeviceId(),channelId,callId,"rtp",ssrcInfo.getStream(), ssrcInfo.getSsrc(), mediaServerVoItem.getId(),response, VideoStreamType.playback);
            okEvent.response(ok);
        },(error)->{
            ssrcTransactionManager.remove(deviceVo.getDeviceId(),channelId,ssrcInfo.getStream(),callId,null);
            ssrcConfigManager.releaseSsrc(mediaServerVoItem.getId(),ssrcInfo.getSsrc());
            errorEvent.response(error);
        });
    }

    /**
     * 请求历史媒体下载
     *
     * @param deviceVo        视频设备
     * @param channelId     预览通道
     * @param startTime     开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime       结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param downloadSpeed 下载倍速参数
     */
    @Override
    public void downloadStreamCmd(SipServer sipServer, MediaServerVo mediaServerVoItem, SSRCInfo ssrcInfo, DeviceVo deviceVo, String channelId, String startTime, String endTime, int downloadSpeed, boolean isSeniorSdp, HookEvent hookEvent, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        log.info("{} 分配的ZLM为: {} [{}:{}]", ssrcInfo.getStream(), mediaServerVoItem.getId(), mediaServerVoItem.getIp(), ssrcInfo.getPort());
        String sdpIp;
        if (!ObjectUtils.isEmpty(deviceVo.getSdpIp())) {
            sdpIp = deviceVo.getSdpIp();
        }else {
            sdpIp = mediaServerVoItem.getSdpIp();
        }
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append("o=" + channelId + " 0 0 IN IP4 " + sdpIp + "\r\n");
        content.append("s=Download\r\n");
        content.append("u=" + channelId + ":0\r\n");
        content.append("c=IN IP4 " + sdpIp + "\r\n");
        content.append("t=" +  DateUtil.parse(startTime).getTime()/1000L + " "
                +  DateUtil.parse(endTime).getTime()/1000L + "\r\n");

        String streamMode = StreamModeType.getName(deviceVo.getStreamMode()).toUpperCase();

        if (isSeniorSdp) {
            if ("TCP-PASSIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 126 125 99 34 98 97\r\n");
            } else if ("UDP".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 126 125 99 34 98 97\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=fmtp:126 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:126 H264/90000\r\n");
            content.append("a=rtpmap:125 H264S/90000\r\n");
            content.append("a=fmtp:125 profile-level-id=42e01e\r\n");
            content.append("a=rtpmap:99 MP4V-ES/90000\r\n");
            content.append("a=fmtp:99 profile-level-id=3\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            if ("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        } else {
            if ("TCP-PASSIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " TCP/RTP/AVP 96 97 98 99\r\n");
            } else if ("UDP".equals(streamMode)) {
                content.append("m=video " + ssrcInfo.getPort() + " RTP/AVP 96 97 98 99\r\n");
            }
            content.append("a=recvonly\r\n");
            content.append("a=rtpmap:96 PS/90000\r\n");
            content.append("a=rtpmap:97 MPEG4/90000\r\n");
            content.append("a=rtpmap:98 H264/90000\r\n");
            content.append("a=rtpmap:99 H265/90000\r\n");
            if ("TCP-PASSIVE".equals(streamMode)) { // tcp被动模式
                content.append("a=setup:passive\r\n");
                content.append("a=connection:new\r\n");
            } else if ("TCP-ACTIVE".equals(streamMode)) { // tcp主动模式
                content.append("a=setup:active\r\n");
                content.append("a=connection:new\r\n");
            }
        }
        content.append("a=downloadspeed:" + downloadSpeed + "\r\n");
        content.append("y=" + ssrcInfo.getSsrc() + "\r\n");//ssrc
        log.debug("此时请求下载信令的ssrc===>{}",ssrcInfo.getSsrc());

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        SIPRequest request = (SIPRequest)SIPRequestProvider.builder(sipServer, null, Request.INVITE, content.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("APPLICATION", "SDP")
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .createSubjectHeader(channelId,ssrcInfo.getSsrc(), sipConfigProperties.getId())
                .createUserAgentHeader()
                .buildRequest();
        String callId = request.getCallId().getCallId();
        //添加下载开始订阅
        HookKey hookKey = HookKeyFactory.onStreamChanged("rtp", ssrcInfo.getStream(), true, "rtsp", mediaServerVoItem.getId());
        mediaHookSubscribe.addSubscribe(hookKey,(mediaServerVo,response)->{
            log.debug("sip 添加订阅===callId {}",callId);
            response.setCallId(callId);
            hookEvent.response(mediaServerVo,response);
            mediaHookSubscribe.removeSubscribe(hookKey);
            //添加下载结束订阅
            HookKey hookKey1 = HookKeyFactory.onStreamChanged("rtp", ssrcInfo.getStream(), false, "rtsp", mediaServerVoItem.getId());
            mediaHookSubscribe.addSubscribe(hookKey1,(vo,res)->{
                log.info("[录像]下载结束， 发送BYE");
                try {
                    streamByeCmd(sipServer,deviceVo, channelId, ssrcInfo.getStream(), callId,null,null,null);
                } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
                    log.error("[录像]下载结束， 发送BYE失败 {}", e.getMessage());
                }
            });
        });
        SipSendMessage.sendMessage(sipServer,deviceVo, request, ok->{
            ResponseEvent responseEvent = (ResponseEvent) ok.getEvent();
            SIPResponse response = (SIPResponse) responseEvent.getResponse();
            SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
            ssrcTransactionManager.put(deviceVo.getDeviceId(),channelId,response.getCallIdHeader().getCallId(),"rtp",ssrcInfo.getStream(),ssrcInfo.getSsrc(),mediaServerVoItem.getId(),response,VideoStreamType.download);
            okEvent.response(ok);
        },errorEvent);
    }

    /**
     * 视频流停止
     */
    @Override
    public void streamByeCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String stream, String callId,VideoStreamType type, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        MediaServerVoService mediaServerService = VideoService.getMediaServerService();
        SsrcTransaction ssrcTransaction = ssrcTransactionManager.getParamOne(deviceVo.getDeviceId(), channelId, callId, stream,type);
        if(ssrcTransaction == null){
            log.info("[视频流停止]未找到视频流信息，设备：{}, 流ID: {}", deviceVo.getDeviceId(), stream);
            if(errorEvent != null){
                errorEvent.response(new EventResult<RestResultEvent>(new RestResultEvent(RespCode.CODE_2.getValue(),"未找到视频流信息")));
            }
            return;
        }
        MediaServerVo mediaServerVo = mediaServerService.findOnLineMediaServerId(ssrcTransaction.getMediaServerId());
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        ssrcConfigManager.releaseSsrc(ssrcTransaction.getMediaServerId(),ssrcTransaction.getSsrc());
        ssrcTransactionManager.remove(deviceVo.getDeviceId(),ssrcTransaction.getChannelId(),ssrcTransaction.getStream());
        SipTransactionInfo sipTransactionInfo = ssrcTransaction.getSipTransactionInfo();
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.BYE,null)
                .createSipURI(ssrcTransaction.getChannelId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), false)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), sipTransactionInfo.getFromTag())
                .createToHeader(ssrcTransaction.getChannelId(), deviceVo.getHostAddress(), sipTransactionInfo.getToTag())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createCallIdHeader(null,null,sipTransactionInfo.getCallId())
                .createUserAgentHeader()
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,ok->{
            if(mediaServerVo != null){
                MediaClient.closeRtpServer(mediaServerVo,ssrcTransaction.getStream());
                MediaClient.closeStreams(mediaServerVo,"__defaultVhost__",ssrcTransaction.getApp(),ssrcTransaction.getStream());
            }
            if(okEvent!= null){
                okEvent.response(ok);
            }
        },error->{
            if(mediaServerVo != null){
                MediaClient.closeRtpServer(mediaServerVo,ssrcTransaction.getStream());
            }
            if(errorEvent!= null){
                errorEvent.response(error);
            }
        });
    }

    /**
     * 语音广播
     *
     * @param deviceVo 视频设备
     */

    @Override
    public void audioBroadcastCmd(SipServer sipServer, DeviceVo deviceVo, String channelId,SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        StringBuffer broadcastXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        broadcastXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        broadcastXml.append("<Notify>\r\n");
        broadcastXml.append("<CmdType>Broadcast</CmdType>\r\n");
        broadcastXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        broadcastXml.append("<SourceID>" + sipConfigProperties.getId() + "</SourceID>\r\n");
        broadcastXml.append("<TargetID>" + channelId + "</TargetID>\r\n");
        broadcastXml.append("</Notify>\r\n");
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, broadcastXml.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }


    /**
     * 音视频录像控制
     *
     * @param deviceVo       视频设备
     * @param channelId    预览通道
     * @param recordCmdStr 录像命令：Record / StopRecord
     */
    @Override
    public void recordCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String recordCmdStr, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId = StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("<RecordCmd>" + recordCmdStr + "</RecordCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 远程启动控制命令
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void teleBootCmd(SipServer sipServer, DeviceVo deviceVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<TeleBoot>Boot</TeleBoot>\r\n");
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 报警布防/撤防命令
     *
     * @param deviceVo      视频设备
     * @param guardCmdStr "SetGuard"/"ResetGuard"
     */
    @Override
    public void guardCmd(SipServer sipServer, DeviceVo deviceVo,String channelId, String guardCmdStr, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String charset = CharsetType.getName(deviceVo.getCharset());
        String deviceId = StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("<GuardCmd>" + guardCmdStr + "</GuardCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 报警复位命令
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void alarmCmd(SipServer sipServer, DeviceVo deviceVo, String alarmMethod, String alarmType, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("<AlarmCmd>ResetAlarm</AlarmCmd>\r\n");
        if (!ObjectUtils.isEmpty(alarmMethod) || !ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<Info>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod) || !ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("</Info>\r\n");
        }
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 强制关键帧命令,设备收到此命令应立刻发送一个IDR帧
     *
     * @param deviceVo    视频设备
     * @param channelId 预览通道
     */
    @Override
    public void iFrameCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId = ObjectUtils.isEmpty(channelId) ? deviceVo.getDeviceId() : channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("<IFameCmd>Send</IFameCmd>\r\n");
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 看守位控制命令
     *
     * @param deviceVo      视频设备
     * @param enabled     看守位使能：1 = 开启，0 = 关闭
     * @param resetTime   自动归位时间间隔，开启看守位时使用，单位:秒(s)
     * @param presetIndex 调用预置位编号，开启看守位时使用，取值范围0~255
     */
    @Override
    public void homePositionCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String enabled, String resetTime, String presetIndex, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId = ObjectUtils.isEmpty(channelId) ? deviceVo.getDeviceId() : channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceControl</CmdType>\r\n");

        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("<HomePosition>\r\n");
        if (NumberUtil.isInteger(enabled) && (!enabled.equals("0"))) {
            cmdXml.append("<Enabled>1</Enabled>\r\n");
            if (NumberUtil.isInteger(resetTime)) {
                cmdXml.append("<ResetTime>" + resetTime + "</ResetTime>\r\n");
            } else {
                cmdXml.append("<ResetTime>0</ResetTime>\r\n");
            }
            if (NumberUtil.isInteger(presetIndex)) {
                cmdXml.append("<PresetIndex>" + presetIndex + "</PresetIndex>\r\n");
            } else {
                cmdXml.append("<PresetIndex>0</PresetIndex>\r\n");
            }
        } else {
            cmdXml.append("<Enabled>0</Enabled>\r\n");
        }
        cmdXml.append("</HomePosition>\r\n");
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 设备配置命令：basicParam
     *
     * @param deviceVo            视频设备
     * @param channelId         通道编码（可选）
     * @param name              设备/通道名称（可选）
     * @param expiration        注册过期时间（可选）
     * @param heartBeatInterval 心跳间隔时间（可选）
     * @param heartBeatCount    心跳超时次数（可选）
     */
    @Override
    public void deviceBasicConfigCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String name, String expiration, String heartBeatInterval, String heartBeatCount, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId = ObjectUtils.isEmpty(channelId) ? deviceVo.getDeviceId() : channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Control>\r\n");
        cmdXml.append("<CmdType>DeviceConfig</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("<BasicParam>\r\n");
        if (!ObjectUtils.isEmpty(name)) {
            cmdXml.append("<Name>" + name + "</Name>\r\n");
        }
        if (NumberUtil.isInteger(expiration)) {
            if (Integer.valueOf(expiration) > 0) {
                cmdXml.append("<Expiration>" + expiration + "</Expiration>\r\n");
            }
        }
        if (NumberUtil.isInteger(heartBeatInterval)) {
            if (Integer.valueOf(heartBeatInterval) > 0) {
                cmdXml.append("<HeartBeatInterval>" + heartBeatInterval + "</HeartBeatInterval>\r\n");
            }
        }
        if (NumberUtil.isInteger(heartBeatCount)) {
            if (Integer.valueOf(heartBeatCount) > 0) {
                cmdXml.append("<HeartBeatCount>" + heartBeatCount + "</HeartBeatCount>\r\n");
            }
        }
        cmdXml.append("</BasicParam>\r\n");
        cmdXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询设备状态
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void deviceStatusQuery(SipServer sipServer, DeviceVo deviceVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        String charset = CharsetType.getName(deviceVo.getCharset());
        StringBuffer catalogXml = new StringBuffer(200);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>DeviceStatus</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, catalogXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询设备信息
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void deviceInfoQuery(SipServer sipServer, DeviceVo deviceVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer catalogXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");


        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, catalogXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询目录列表
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void catalogQuery(SipServer sipServer, DeviceVo deviceVo, int sn, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        StringBuffer catalogXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        catalogXml.append("<Query>\r\n");
        catalogXml.append("  <CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("  <SN>" + sn + "</SN>\r\n");
        catalogXml.append("  <DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        catalogXml.append("</Query>\r\n");
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, catalogXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询录像信息
     *
     * @param deviceVo    视频设备
     * @param startTime 开始时间,格式要求：yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间,格式要求：yyyy-MM-dd HH:mm:ss
     */
    @Override
    public void recordInfoQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, String startTime, String endTime, int sn, Integer secrecy, String type, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        if (secrecy == null) {
            secrecy = 0;
        }
        if (type == null) {
            type = "all";
        }

        StringBuffer recordInfoXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        recordInfoXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        recordInfoXml.append("<Query>\r\n");
        recordInfoXml.append("<CmdType>RecordInfo</CmdType>\r\n");
        recordInfoXml.append("<SN>" + sn + "</SN>\r\n");
        recordInfoXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        if (startTime != null) {
            recordInfoXml.append("<StartTime>" + DateUtil.format(DateUtil.parse(startTime), DatePattern.UTC_SIMPLE_PATTERN) + "</StartTime>\r\n");
        }
        if (endTime != null) {
            recordInfoXml.append("<EndTime>" + DateUtil.format(DateUtil.parse(endTime),DatePattern.UTC_SIMPLE_PATTERN) + "</EndTime>\r\n");
        }
        if (secrecy != null) {
            recordInfoXml.append("<Secrecy> " + secrecy + " </Secrecy>\r\n");
        }
        if (type != null) {
            // 大华NVR要求必须增加一个值为all的文本元素节点Type
            recordInfoXml.append("<Type>" + type + "</Type>\r\n");
        }
        recordInfoXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, recordInfoXml.toString())
                .createSipURI(channelId, deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(channelId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询报警信息
     *
     * @param deviceVo        视频设备
     * @param startPriority 报警起始级别（可选）
     * @param endPriority   报警终止级别（可选）
     * @param alarmMethod   报警方式条件（可选）
     * @param alarmType     报警类型
     * @param startTime     报警发生起始时间（可选）
     * @param endTime       报警发生终止时间（可选）
     * @return true = 命令发送成功
     */
    @Override
    public void alarmInfoQuery(SipServer sipServer, DeviceVo deviceVo, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>Alarm</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        if (!ObjectUtils.isEmpty(startPriority)) {
            cmdXml.append("<StartAlarmPriority>" + startPriority + "</StartAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(endPriority)) {
            cmdXml.append("<EndAlarmPriority>" + endPriority + "</EndAlarmPriority>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmMethod)) {
            cmdXml.append("<AlarmMethod>" + alarmMethod + "</AlarmMethod>\r\n");
        }
        if (!ObjectUtils.isEmpty(alarmType)) {
            cmdXml.append("<AlarmType>" + alarmType + "</AlarmType>\r\n");
        }
        if (!ObjectUtils.isEmpty(startTime)) {
            cmdXml.append("<StartAlarmTime>" + startTime + "</StartAlarmTime>\r\n");
        }
        if (!ObjectUtils.isEmpty(endTime)) {
            cmdXml.append("<EndAlarmTime>" + endTime + "</EndAlarmTime>\r\n");
        }
        cmdXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询设备配置
     *
     * @param deviceVo     视频设备
     * @param channelId  通道编码（可选）
     * @param configType 配置类型：
     */
    @Override
    public void deviceConfigQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, String configType, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId =StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>ConfigDownload</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("<ConfigType>" + configType + "</ConfigType>\r\n");
        cmdXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询设备预置位置
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void presetQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId =StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>PresetQuery</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    /**
     * 查询移动设备位置数据
     *
     * @param deviceVo 视频设备
     */
    @Override
    public void mobilePostitionQuery(SipServer sipServer, DeviceVo deviceVo, String channelId, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId =StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer mobilePostitionXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        mobilePostitionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        mobilePostitionXml.append("<Query>\r\n");
        mobilePostitionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        mobilePostitionXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        mobilePostitionXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        mobilePostitionXml.append("<Interval>5</Interval>\r\n");
        mobilePostitionXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, mobilePostitionXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);

    }

    /**
     * 订阅、取消订阅移动位置
     *
     * @param deviceVo 视频设备
     * @return true = 命令发送成功
     */
    @Override
    public SIPRequest mobilePositionSubscribe(SipServer sipServer, DeviceVo deviceVo,String channelId, SIPRequest requestOld, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId =StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer subscribePostitionXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        subscribePostitionXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        subscribePostitionXml.append("<Query>\r\n");
        subscribePostitionXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        subscribePostitionXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        subscribePostitionXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        if (deviceVo.getSubscribeCycleForMobilePosition() > 0) {
            subscribePostitionXml.append("<Interval>" + (deviceVo.getMobilePositionSubmissionInterval()>0?deviceVo.getMobilePositionSubmissionInterval():5) + "</Interval>\r\n");
        }
        subscribePostitionXml.append("</Query>\r\n");
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.SUBSCRIBE, subscribePostitionXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceId, deviceVo.getHostAddress(), null)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),requestOld == null?null:requestOld.getCallIdHeader().getCallId())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .createExpiresHeader(deviceVo.getSubscribeCycleForMobilePosition())
                .createEventHeader(String.valueOf((int) Math.floor(Math.random() * 10000)),CmdType.MOBILE_POSITION_RESPONSE.getValue())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
        return (SIPRequest) request;
    }


    @Override
    public SIPRequest catalogSubscribe(SipServer sipServer, DeviceVo deviceVo, SIPRequest requestOld, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {

        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<CmdType>Catalog</CmdType>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<DeviceID>" + deviceVo.getDeviceId() + "</DeviceID>\r\n");
        cmdXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.SUBSCRIBE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),requestOld == null?null:requestOld.getCallIdHeader().getCallId())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .createExpiresHeader(deviceVo.getSubscribeCycleForCatalog())
                .createEventHeader(String.valueOf((int) Math.floor(Math.random() * 10000)),CmdType.CATALOG_RESPONSE.getValue())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
        return (SIPRequest)  request;
    }

    @Override
    public SIPRequest alarmSubscribe(SipServer sipServer, DeviceVo deviceVo,String channelId, SIPRequest requestOld, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceId =StringUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer cmdXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        cmdXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        cmdXml.append("<Query>\r\n");
        cmdXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        cmdXml.append("<CmdType>Alarm</CmdType>\r\n");
//        cmdXml.append("<StartAlarmPriority>0</StartAlarmPriority>\r\n");
//        cmdXml.append("<EndAlarmPriority>0</EndAlarmPriority>\r\n");
//        cmdXml.append("<AlarmMethod>0</AlarmMethod>\r\n");
//        cmdXml.append("<AlarmType>0</AlarmType>\r\n");
        cmdXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        cmdXml.append("</Query>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.SUBSCRIBE, cmdXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceVo.getDeviceId(), deviceVo.getHostAddress(), null)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),requestOld == null?null:requestOld.getCallIdHeader().getCallId())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",localIp, sipConfigProperties.getPort()))
                .createExpiresHeader(deviceVo.getSubscribeCycleForAlarm())
                .createEventHeader(String.valueOf((int) Math.floor(Math.random() * 10000)),CmdType.ALARM_RESPONSE.getValue())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
        return (SIPRequest)  request;
    }

    @Override
    public void dragZoomCmd(SipServer sipServer, DeviceVo deviceVo, String channelId, String cmdString, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        String deviceID = ObjectUtils.isEmpty(channelId)?deviceVo.getDeviceId():channelId;
        StringBuffer dragXml = new StringBuffer(200);
        String charset = CharsetType.getName(deviceVo.getCharset());
        dragXml.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\r\n");
        dragXml.append("<Control>\r\n");
        dragXml.append("<CmdType>DeviceControl</CmdType>\r\n");
        dragXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        dragXml.append("<DeviceID>" + deviceID + "</DeviceID>\r\n");
        dragXml.append(cmdString);
        dragXml.append("</Control>\r\n");

        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, dragXml.toString())
                .createSipURI(deviceVo.getDeviceId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceID, deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo,request,okEvent,errorEvent);
    }

    /**
     * 回放暂停
     */
    @Override
    public void playPauseCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PAUSE RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("PauseTime: now\r\n");

        playbackControlCmd(sipServer, deviceVo, streamInfo, content.toString(),okEvent,errorEvent);
    }


    /**
     * 回放恢复
     */
    @Override
    public void playResumeCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo,  SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=now-\r\n");

        playbackControlCmd(sipServer, deviceVo, streamInfo, content.toString(),okEvent,errorEvent);
    }

    /**
     * 回放拖动播放
     */
    @Override
    public void playSeekCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, long seekTime, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Range: npt=" + Math.abs(seekTime) + "-\r\n");

        playbackControlCmd(sipServer, deviceVo, streamInfo, content.toString(),okEvent,errorEvent);
    }

    /**
     * 回放倍速播放
     */
    @Override
    public void playSpeedCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, Double speed,  SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        StringBuffer content = new StringBuffer(200);
        content.append("PLAY RTSP/1.0\r\n");
        content.append("CSeq: " + getInfoCseq() + "\r\n");
        content.append("Scale: " + String.format("%.6f", speed) + "\r\n");

        playbackControlCmd(sipServer, deviceVo, streamInfo, content.toString(),okEvent,errorEvent);
    }

    private int getInfoCseq() {
        return (int) ((Math.random() * 9 + 1) * Math.pow(10, 8));
    }

    @Override
    public void playbackControlCmd(SipServer sipServer, DeviceVo deviceVo, StreamInfo streamInfo, String content, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        SsrcTransaction ssrcTransaction = RedisService.getSsrcTransactionManager().getParamOne(deviceVo.getDeviceId(), streamInfo.getChannelId(), null, streamInfo.getStream(),null);
        if(ssrcTransaction == null){
            log.info("[回放控制]未找到视频流信息，设备：{}, 流ID: {}", deviceVo.getDeviceId(), streamInfo.getStream());
            return;
        }
        SipTransactionInfo sipTransactionInfo = ssrcTransaction.getSipTransactionInfo();
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.INFO, content)
                .createSipURI(streamInfo.getChannelId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), false)
                .createCallIdHeader(null,null,sipTransactionInfo.getCallId())
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), sipTransactionInfo.getFromTag())
                .createToHeader(streamInfo.getChannelId(), deviceVo.getHostAddress(), sipTransactionInfo.getToTag())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSRTSP")
                .buildRequest();
        if (request == null) {
            log.info("[回放控制]构建Request信息失败，设备：{}, 流ID: {}", deviceVo.getDeviceId(), streamInfo.getStream());
            return;
        }
        SipSendMessage.sendMessage(sipServer,deviceVo, request,okEvent,errorEvent);
    }

    @Override
    public void sendAlarmMessage(SipServer sipServer, DeviceVo deviceVo, DeviceAlarmVo deviceAlarmVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException {
        if (deviceVo == null) {
            return;
        }
        log.info("[发送 报警通知] {}/{}->{},{}", deviceVo.getDeviceId(), deviceAlarmVo.getChannelId(), deviceAlarmVo.getLongitude(), deviceAlarmVo.getLatitude());
        String characterSet = CharsetType.getName(deviceVo.getCharset());
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Notify>\r\n");
        deviceStatusXml.append("<CmdType>Alarm</CmdType>\r\n");
        deviceStatusXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + deviceAlarmVo.getChannelId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<AlarmPriority>" + deviceAlarmVo.getAlarmPriority() + "</AlarmPriority>\r\n");
        deviceStatusXml.append("<AlarmMethod>" + deviceAlarmVo.getAlarmMethod() + "</AlarmMethod>\r\n");
        deviceStatusXml.append("<AlarmTime>" + deviceAlarmVo.getAlarmTime() + "</AlarmTime>\r\n");
        deviceStatusXml.append("<AlarmDescription>" + deviceAlarmVo.getAlarmDescription() + "</AlarmDescription>\r\n");
        deviceStatusXml.append("<Longitude>" + deviceAlarmVo.getLongitude() + "</Longitude>\r\n");
        deviceStatusXml.append("<Latitude>" + deviceAlarmVo.getLatitude() + "</Latitude>\r\n");
        deviceStatusXml.append("<info>\r\n");
        deviceStatusXml.append("<AlarmType>" + deviceAlarmVo.getAlarmType() + "</AlarmType>\r\n");
        deviceStatusXml.append("</info>\r\n");
        deviceStatusXml.append("</Notify>\r\n");
        String localIp = sipServer.getLocalIp(deviceVo.getLocalIp());
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();;
        //构建器
        Request request = SIPRequestProvider.builder(sipServer, null, Request.MESSAGE, deviceStatusXml.toString())
                .createSipURI(deviceAlarmVo.getChannelId(), deviceVo.getHostAddress())
                .addViaHeader(localIp, sipConfigProperties.getPort(), TransportType.getName(deviceVo.getTransport()), true)
                .createCallIdHeader(localIp,TransportType.getName(deviceVo.getTransport()),null)
                .createFromHeader(sipConfigProperties.getId(), sipConfigProperties.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(deviceAlarmVo.getChannelId(), deviceVo.getHostAddress(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .createUserAgentHeader()
                .buildRequest();
        SipSendMessage.sendMessage(sipServer,deviceVo,request,okEvent,errorEvent);
    }


    @Override
    public void sendAckMessage(SipServer sipServer, SessionDescription sdp, ResponseEventExt event,SIPResponse response, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SdpParseException {
        SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
        Request reqAck = SIPRequestProvider.builder(sipServer, null, Request.ACK, null)
                .createSipURI(sdp.getOrigin().getUsername(), event.getRemoteIpAddress() + ":" + event.getRemotePort())
                .addViaHeader(response.getLocalAddress().getHostAddress(), sipConfigProperties.getPort(), response.getTopmostViaHeader().getTransport(), false)
                .createCallIdHeader(response.getCallIdHeader())
                .createFromHeader(response.getFromHeader())
                .createToHeader(response.getToHeader())
                .createCSeqHeader(response.getCSeq().getSeqNumber())
                .createContactHeader(sipConfigProperties.getId(),String.format("%s:%s",response.getLocalAddress().getHostAddress(), sipConfigProperties.getPort()))
                .createUserAgentHeader()
                .buildRequest();
        log.info("[回复ack] {}-> {}:{} ", sdp.getOrigin().getUsername(), event.getRemoteIpAddress(), event.getRemotePort());
        SipSendMessage.handleEvent(sipServer,response.getCallIdHeader().getCallId(),okEvent,errorEvent);
        sipMessageHandle.handleMessage(response.getLocalAddress().getHostAddress(),reqAck);
    }
}
