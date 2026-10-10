package cn.com.nla.common.video.core.sip.listener.event.request.impl;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.enums.InviteErrorCode;
import cn.com.nla.common.video.basic.enums.InviteStreamType;
import cn.com.nla.common.video.basic.enums.StreamType;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedHookVo;
import cn.com.nla.common.video.basic.vo.sip.DeviceRawContent;
import cn.com.nla.common.video.basic.vo.sip.SSRCInfo;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.*;
import cn.com.nla.common.video.core.demo.Gb28181Sdp;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SendRtpManager;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.redis.subscribe.media.HookKeyFactory;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.service.MediaService;
import cn.com.nla.common.video.core.service.PlayService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.*;
import cn.com.nla.common.video.core.sip.callback.InviteErrorCallback;
import cn.com.nla.common.video.core.sip.listener.event.request.AbstractSipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import gov.nist.javax.sdp.TimeDescriptionImpl;
import gov.nist.javax.sdp.fields.TimeField;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Vector;

/**
 * SIP命令类型： INVITE请求
 */
@Slf4j
public class InviteRequestProcessor  extends AbstractSipRequestEvent implements SipRequestEvent {

    @Autowired
    private PlayService playService;

    @Override
    public String getMethod() {
        return Request.INVITE;
    }

    /**
     * 处理invite请求
     * @param evt 请求消息
     */
    @Override
    public void process(RequestEvent evt) {
        //  Invite Request消息实现，此消息一般为级联消息，上级给下级发送请求视频指令
        SIPRequest request = (SIPRequest)evt.getRequest();
        CallIdHeader callIdHeader = (CallIdHeader) request.getHeader(CallIdHeader.NAME);
        String channelId = SipUtils.getChannelIdFromRequest(request);
        String requesterId = SipUtils.getUserIdFromFromHeader(request);
        if (requesterId == null || channelId == null) {
            log.info("无法从FromHeader的Address中获取到平台id，返回400");
            // 参数不全， 发400，请求错误
            try {
                responseAck(request, Response.BAD_REQUEST,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
            }
            return;
        }
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        // 查询请求是否来自上级平台
        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(requesterId);
        // 查询请求是否来自上级设备
        DeviceVo deviceVo = deviceVoService.findDeviceGbId(requesterId);
        try {
            if(parentPlatformVo != null){
                inviteParentPlatformHandle(request,callIdHeader, parentPlatformVo,requesterId,channelId);
            }else if(deviceVo != null){
                inviteDeviceHandle(request,callIdHeader,requesterId,deviceVo,channelId);
            }else {
                log.warn("来自无效设备/平台的请求");
                try {
                    responseAck(request, Response.BAD_REQUEST,null); // 不支持的格式，发415
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 来自无效设备/平台的请求， {}", e.getMessage());
                }
            }
        }catch (Exception e){
            log.error("[INVITE请求]，消息处理异常：", e );
        }
    }

    /**
     * 处理设备的invite请求
     */
    private void inviteDeviceHandle(SIPRequest request,CallIdHeader callIdHeader,String requesterId,DeviceVo deviceVo,String platformId){
        log.info("收到设备" + requesterId + "的语音广播Invite请求");
        try {
            responseAck(request, Response.TRYING,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] invite BAD_REQUEST: {}", e.getMessage());
        }
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        // TODO: 2023/8/14 获取不到通道编号,存储缓存先用此方案
        SsrcTransaction paramOne = ssrcTransactionManager.getParamOne(deviceVo.getDeviceId(), null, null, null, VideoStreamType.audio);
        if(paramOne == null){
            log.warn("[语音流]设备：{},未获取 推流信息",deviceVo.getDeviceId());
            try {
                responseAck(request, Response.GONE,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[语音流命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
            }
            return;
        }else {
            ssrcTransactionManager.remove(paramOne.getDeviceId(),paramOne.getChannelId(),paramOne.getStream(),VideoStreamType.audio);
            ssrcTransactionManager.put(paramOne.getDeviceId(),paramOne.getChannelId(),callIdHeader.getCallId(),paramOne.getApp(),paramOne.getStream(),paramOne.getSsrc(),paramOne.getMediaServerId(),request,VideoStreamType.audio);
            dynamicTask.stop(String.format("audio_push_stream:%s_%s",paramOne.getDeviceId(),paramOne.getChannelId()));
        }
        MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(paramOne.getMediaServerId());
        DeviceRawContent deviceRawContent = null;
        try {
            deviceRawContent = handleDeviceRawContent(mediaServerVo, request,"8");
        } catch (SdpException e) {
            log.error("[解析设备语音广播消息失败] handleDeviceRawContent is error! {}", e.getMessage());
            try {
                responseAck(request, Response.GONE,null);
            } catch (SipException | InvalidArgumentException | ParseException er) {
                log.error("[命令发送失败] invite GONE: {}", er.getMessage());
                return;
            }
            return;
        }
        String streamTypeStr = null;
        if (deviceRawContent.isMediaTransmissionTCP()) {
            if (deviceRawContent.isTcpActive()) {
                streamTypeStr = "TCP-ACTIVE";
            }else {
                streamTypeStr = "TCP-PASSIVE";
            }
        }else {
            streamTypeStr = "UDP";
        }
        log.info("[语音流]设备：{}， 通道：{}, 地址：{}:{}，收流方式：{}, ssrc：{}", deviceRawContent.getUsername(), null, deviceRawContent.getAddressStr(), deviceRawContent.getPort(),streamTypeStr, deviceRawContent.getSsrc());
        String streamId = paramOne.getStream();
        MediaRestResult result = MediaClient.getMediaList(mediaServerVo, null, null, "audio", streamId);
        if(result == null || result.getCode() !=RespCode.CODE_0.getValue() || ObjectUtils.isEmpty(result.getData())){
            log.warn("[语音流]设备：{}， 通道：{},未获取 语音流信息",deviceRawContent.getUsername(), paramOne.getChannelId());
            try {
                responseAck(request, Response.GONE,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[语音流命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
            }
            return;
        }
        SendRtp sendRtp = MediaClient.createSendRtp(mediaServerVo,deviceRawContent.getSessionName(), deviceRawContent.getStartTime(),deviceRawContent.getStopTime(),deviceRawContent.getAddressStr(), deviceRawContent.getPort(),deviceRawContent.getSsrc(),sipServer.getSipConfigProperties().getId(),platformId,deviceVo.getDeviceId(),paramOne.getChannelId(),"audio",streamId, deviceRawContent.isMediaTransmissionTCP(),deviceRawContent.isTcpActive(),videoProperties.getServerId(),callIdHeader.getCallId(),true,InviteStreamType.getInviteStreamType(deviceRawContent.getSessionName().toUpperCase()));
        if (sendRtp == null) {
            log.warn("服务器端口资源不足");
            try {
                responseAck(request, Response.BUSY_HERE,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[语音流命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
            }
            return;
        }
        sendRtp.setPt(8);//语音
        sendRtp.setUsePs(false);
        sendRtp.setOnlyAudio(true);
        sendRtpManager.put(sendRtp);
        dynamicTask.startDelay(callIdHeader.getCallId(),60,()->{
            log.error("ack等待超时，发送BYC");
            try {
                sipCommander.streamByeCmd(sipServer, deviceVo,paramOne.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId(),null,null,null);
            } catch (SipException | InvalidArgumentException | ParseException |SsrcTransactionNotFoundException e) {
                log.error("[命令发送失败] 语音流 发送BYE: {}", e.getMessage());
            }
        });
        //获取音频流
        String sdp = createSdp(8, mediaServerVo, request, sendRtp);
        try {
            //回复之后上级平台发送ACK
            responseSdpAck(request, sdp, deviceVo);
        } catch (InvalidArgumentException | SipException | ParseException  e) {
            log.error("[命令发送失败] 语音流 发送SdpAck: {}", e.getMessage());
        }
        // tcp主动模式，回复sdp后开启监听
        if (sendRtp.isTcpActive()) {
            MediaClient.startSendRtpStreamForPassive(mediaServerVo,sendRtp,sendRtp.getLocalPort());
        }
    }

    /**
     * 处理上级平台的invite请求
     */
    private void inviteParentPlatformHandle(SIPRequest request, CallIdHeader callIdHeader, ParentPlatformVo parentPlatformVo, String requesterId, String channelId){
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        PlatformCatalogVoService platformCatalogVoService = VideoService.getPlatformCatalogService();
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        StreamPushVoService streamPushVoService = VideoService.getStreamPushService();

        DeviceChannelVo deviceChannelVo = deviceChannelVoService.findPlatformIdChannelId(requesterId, channelId);
        GbStreamVo gbStreamVo = gbStreamVoService.findPlatformId(requesterId, channelId);
        PlatformCatalogVo platformCatalogVo = platformCatalogVoService.findChannelId(channelId);

        if(deviceChannelVo != null){
            // 通道存在，发100，TRYING
            try {
                responseAck(request, Response.TRYING,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite TRYING: {}", e.getMessage());
            }
            //处理设备 invite
            handleDeviceChannel(request,callIdHeader, parentPlatformVo, deviceChannelVo,requesterId,channelId);
        }else if(gbStreamVo != null){
            String mediaServerId = gbStreamVo.getMediaServerId();
            MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(mediaServerId);//流媒体
            //平级平台暂不实现
            if(mediaServerVo == null){
                log.info("[ app={}, stream={} ]找不到zlm {}，返回410", gbStreamVo.getApp(), gbStreamVo.getStream(), mediaServerId);
                try {
                    responseAck(request, Response.GONE,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite GONE: {}", e.getMessage());
                }
                return;
            }
            DeviceRawContent deviceRawContent =null;
            try {
                deviceRawContent = handleDeviceRawContent(mediaServerVo,request,"96");
            } catch (SdpException e) {
                log.error("[解析设备消息失败] handleDeviceRawContent is error! {}", e.getMessage());
                try {
                    responseAck(request, Response.GONE,null);
                } catch (SipException | InvalidArgumentException | ParseException er) {
                    log.error("[命令发送失败] invite GONE: {}", er.getMessage());
                }
                return;
            }
            StreamProxyVo streamProxyVo = streamProxyVoService.findAppStream(gbStreamVo.getApp(), gbStreamVo.getStream());//拉流信息
            StreamPushVo streamPushVo = streamPushVoService.findAppStream(gbStreamVo.getApp(), gbStreamVo.getStream());//推流信息
            if(StreamType.PROXY.getValue() == gbStreamVo.getStreamType() && streamProxyVo != null){
                //拉流
                if(streamProxyVo.getStatus()==ConstEnum.Flag.YES.getValue()){
                    pushStream(request, mediaServerVo, parentPlatformVo,deviceRawContent, gbStreamVo,callIdHeader,channelId);
                }else {
                    notifyPushStream(request, mediaServerVo, parentPlatformVo,deviceRawContent, gbStreamVo, streamProxyVo,callIdHeader,channelId);
                }
            }else if(StreamType.PULL.getValue() == gbStreamVo.getStreamType() && streamPushVo != null){
                //推流
                if(streamPushVo.getPushIng()==ConstEnum.Flag.YES.getValue()){
                    pushStream(request, mediaServerVo, parentPlatformVo,deviceRawContent, gbStreamVo,callIdHeader,channelId);
                }else {
                    notifyPushStream(request, mediaServerVo, parentPlatformVo,deviceRawContent, gbStreamVo, streamProxyVo,callIdHeader,channelId);
                }
            }else {
                log.info("[ app={}, stream={} ]找不到 zlm {}，返回410", gbStreamVo.getApp(), gbStreamVo.getStream(), mediaServerId);
                try {
                    responseAck(request, Response.GONE,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite GONE: {}", e.getMessage());
                }

            }
        }else if (platformCatalogVo != null) {
            try {
                // 目录不支持点播
                responseAck(request, Response.BAD_REQUEST, "catalog channel can not play");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite 目录不支持点播: {}", e.getMessage());
            }
        }else {
            log.info("通道不存在，返回404");
            try {
                // 通道不存在，发404，资源不存在
                responseAck(request, Response.NOT_FOUND,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite 通道不存在: {}", e.getMessage());
            }
        }
    }

    //处理设备通道
    private void handleDeviceChannel(SIPRequest request,  CallIdHeader callIdHeader, ParentPlatformVo parentPlatformVo, DeviceChannelVo deviceChannelVo, String requesterId, String channelId){
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();

        DeviceVo deviceVo = deviceVoService.findPlatformIdChannelId(requesterId, channelId);
        if(deviceVo == null){
            log.warn("点播平台{}的通道{}时未找到设备信息", requesterId, deviceChannelVo);
            try {
                responseAck(request, Response.SERVER_INTERNAL_ERROR,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite 未找到设备信息: {}", e.getMessage());
            }
            return;
        }
        MediaServerVo mediaServerVo = null;
        if(deviceVo.getMediaServerId() != null){
            mediaServerVo = mediaServerVoService.findOnLineMediaServerId(deviceVo.getMediaServerId());
        }else {
            mediaServerVo = mediaServerVoService.findMediaServerForMinimumLoad();
        }
        if (mediaServerVo == null) {
            log.warn("未找到可用的zlm");
            try {
                responseAck(request, Response.BUSY_HERE,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite BUSY_HERE: {}", e.getMessage());
            }
            return;
        }
        DeviceRawContent deviceRawContent =null;
        try {
            deviceRawContent = handleDeviceRawContent(mediaServerVo,request,"96");
        } catch (SdpException e) {
            log.error("[解析设备消息失败] handleDeviceRawContent is error! {}", e.getMessage());
            try {
                responseAck(request, Response.GONE,null);
            } catch (SipException | InvalidArgumentException | ParseException er) {
                log.error("[命令发送失败] invite GONE: {}", er.getMessage());
            }
            return;
        }
        String streamTypeStr = null;
        if (deviceRawContent.isMediaTransmissionTCP()) {
            if (deviceRawContent.isTcpActive()) {
                streamTypeStr = "TCP-ACTIVE";
            }else {
                streamTypeStr = "TCP-PASSIVE";
            }
        }else {
            streamTypeStr = "UDP";
        }
        log.info("[上级Invite]平台：{}， 通道：{}, 地址：{}:{}，收流方式：{}, ssrc：{}", deviceRawContent.getUsername(), channelId, deviceRawContent.getAddressStr(), deviceRawContent.getPort(),streamTypeStr, deviceRawContent.getSsrc());
        SendRtp sendRtp = MediaClient.createSendRtp(mediaServerVo,deviceRawContent.getSessionName(), deviceRawContent.getStartTime(),deviceRawContent.getStopTime(),deviceRawContent.getAddressStr(), deviceRawContent.getPort(),deviceRawContent.getSsrc(),sipServer.getSipConfigProperties().getId(),requesterId,deviceVo.getDeviceId(),deviceChannelVo.getChannelId(),"rtp",null, deviceRawContent.isMediaTransmissionTCP(),deviceRawContent.isTcpActive(),videoProperties.getServerId(),callIdHeader.getCallId(),ConstEnum.Flag.YES.getValue() == parentPlatformVo.getRtcp(),InviteStreamType.getInviteStreamType(deviceRawContent.getSessionName().toUpperCase()));
        if (sendRtp == null) {
            log.warn("服务器端口资源不足");
            try {
                responseAck(request, Response.BUSY_HERE,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
            }
            return;
        }
        //播放成功后回调
        DeviceRawContent finalDeviceRawContent = deviceRawContent;
        InviteErrorCallback<Object> hookEvent = (code, msg, data)->{
            StreamInfo vo = (StreamInfo)data;
            MediaServerVo media = mediaServerVoService.findOnLineMediaServerId(vo.getMediaServerId());
            log.info("[上级Invite]下级已经开始推流。 回复200OK(SDP)， {}/{}", vo.getApp(), vo.getStream());
            sendRtp.setStatus(1);
            dynamicTask.startDelay(callIdHeader.getCallId(),60,()->{
                log.error("ack等待超时，发送BYC");
                ssrcConfigManager.releaseSsrc(media.getId(),sendRtp.getSsrc());
                try {
                    sipCommanderForPlatform.streamByeCmd(sipServer, parentPlatformVo,sendRtp,null,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                }
            });
            String sdp = createSdp(96, media, request, sendRtp);
            try {
                //回复之后上级平台发送ACK
                responseSdpAck(request, sdp, parentPlatformVo);
            } catch (InvalidArgumentException | SipException | ParseException  e) {
                log.error("[命令发送失败] 国标级联 发送SdpAck: {}", e.getMessage());
            }
            // tcp主动模式，回复sdp后开启监听
            if (sendRtp.isTcpActive()) {
                MediaClient.startSendRtpStreamForPassive(media,sendRtp,sendRtp.getLocalPort());
            }
        };
        //发生错误回调
        InviteErrorCallback<Object> errorEvent = (statusCode, msg, data) ->{
            try {
                if (statusCode > 0) {
                    responseAck(request,statusCode,null);
                }
            } catch ( InvalidArgumentException|SipException | ParseException  e) {
                log.error("[命令发送失败] 错误回调 发送失败: ", e);
            }
        };
        if(sendRtp.getPlayType().equals(InviteStreamType.PLAYBACK)){
            //开始回放
            SSRCInfo ssrcInfo = playService.playBack(sipServer, mediaServerVo, deviceVo, channelId, sendRtp.getSsrc(), DateUtil.formatDateTime(new Date(finalDeviceRawContent.getStartTime() * 1000L)), DateUtil.formatDateTime(new Date(finalDeviceRawContent.getStopTime() * 1000L)), (code, msg, data) -> {
                if (code == InviteErrorCode.SUCCESS.getCode()) {
                    hookEvent.run(code, msg, data);
                } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode() || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                    log.info("[录像回放]超时,  通道：{}", channelId);
                    sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(), sendRtp.getChannelId(), null, sendRtp.getCallId());
                    errorEvent.run(code, msg, data);
                } else {
                    errorEvent.run(code, msg, data);
                }
            });
            sendRtp.setStreamId(ssrcInfo.getStream());
            sendRtp.setStatus(1);
        }else if(sendRtp.getPlayType().equals(InviteStreamType.DOWNLOAD)){
            //开始下载
            SSRCInfo ssrcInfo = playService.download(sipServer, mediaServerVo, deviceVo, channelId, sendRtp.getSsrc(), DateUtil.formatDateTime(new Date(finalDeviceRawContent.getStartTime() * 1000L)), DateUtil.formatDateTime(new Date(finalDeviceRawContent.getStopTime() * 1000L)), Integer.parseInt(finalDeviceRawContent.getDownloadSpeed()),
                    (code, msg, data) -> {
                        if (code == InviteErrorCode.SUCCESS.getCode()) {
                            hookEvent.run(code, msg, data);
                        } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode() || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                            log.info("[录像下载]超时， 通道：{}", channelId);
                            sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(), channelId, callIdHeader.getCallId(), null);
                            errorEvent.run(code, msg, data);
                        } else {
                            errorEvent.run(code, msg, data);
                        }
            });
            sendRtp.setStatus(1);
            sendRtp.setStreamId(ssrcInfo.getStream());
        }else {
            //开始点播
            SSRCInfo play = playService.play(sipServer, mediaServerVo, deviceVo.getDeviceId(), channelId, sendRtp.getSsrc(), (code, msg, data) -> {
                if (code == InviteErrorCode.SUCCESS.getCode()) {
                    hookEvent.run(code, msg, data);
                } else if (code == InviteErrorCode.ERROR_FOR_SIGNALLING_TIMEOUT.getCode() || code == InviteErrorCode.ERROR_FOR_STREAM_TIMEOUT.getCode()) {
                    log.error("[上级点播]超时, 用户：{}， 通道：{}", finalDeviceRawContent.getUsername(), channelId);
                    //超时后将发送流移除
                    sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(), sendRtp.getChannelId(), null, sendRtp.getCallId());
                    errorEvent.run(code, msg, data);
                } else {
                    errorEvent.run(code, msg, data);
                }
            });
            sendRtp.setStatus(1);
            sendRtp.setStreamId(play.getStream());
        }
        //将发送流存入redis
        sendRtpManager.put(sendRtp);
    }

    private DeviceRawContent handleDeviceRawContent(MediaServerVo mediaServerVo,SIPRequest request,String mediaFormat) throws SdpException {
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();

        // 解析sdp消息, 使用jainsip 自带的sdp解析方式
        String contentString = new String(request.getRawContent());
        Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
        SessionDescription sdp = gb28181Sdp.getBaseSdb();
        String sessionName = sdp.getSessionName().getValue();
        Long startTime = null;
        Long stopTime = null;
        Instant start = null;
        Instant end = null;
        if (sdp.getTimeDescriptions(false) != null && sdp.getTimeDescriptions(false).size() > 0) {
            TimeDescriptionImpl timeDescription = (TimeDescriptionImpl) (sdp.getTimeDescriptions(false).get(0));
            TimeField startTimeFiled = (TimeField) timeDescription.getTime();
            startTime = startTimeFiled.getStartTime();
            stopTime = startTimeFiled.getStopTime();

            start = Instant.ofEpochSecond(startTime);
            end = Instant.ofEpochSecond(stopTime);
        }
        //  获取支持的格式
        Vector mediaDescriptions = sdp.getMediaDescriptions(true);
        // 查看是否支持PS 负载96
        //String ip = null;
        int port = -1;
        String downloadSpeed = "1";
        boolean mediaTransmissionTCP = false;
        boolean tcpActive = false;
        for (Object description : mediaDescriptions) {
            MediaDescription mediaDescription = (MediaDescription) description;
            Media media = mediaDescription.getMedia();
            downloadSpeed = mediaDescription.getAttribute("downloadspeed");
            Vector mediaFormats = media.getMediaFormats(false);
            if (mediaFormats.contains(mediaFormat)) {
                port = media.getMediaPort();
                //String mediaType = media.getMediaType();
                String protocol = media.getProtocol();
                // 区分TCP发流还是udp， 当前默认udp
                if ("TCP/RTP/AVP".equalsIgnoreCase(protocol)) {
                    String setup = mediaDescription.getAttribute("setup");
                    if (setup != null) {
                        mediaTransmissionTCP = true;
                        if ("active".equalsIgnoreCase(setup)) {
                            tcpActive = true;
                        } else if ("passive".equalsIgnoreCase(setup)) {
                            tcpActive = false;
                        }
                    }
                }
                break;
            }
        }
        if (port == -1) {
            log.info("不支持的媒体格式，返回415");
            // 回复不支持的格式
            try {
                // 不支持的格式，发415
                responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite 不支持的格式: {}", e.getMessage());
            }
            return null;
        }
        String ssrc;
        if (videoProperties.getUseCustomSsrcForParentInvite() || gb28181Sdp.getSsrc() == null) {
            // 上级平台点播时不使用上级平台指定的ssrc，使用自定义的ssrc，参考国标文档-点播外域设备媒体流SSRC处理方式
            ssrc = "Play".equalsIgnoreCase(sessionName) ? ssrcConfigManager.getPlaySsrc(mediaServerVo.getId()) : ssrcConfigManager.getPlayBackSsrc(mediaServerVo.getId());
        }else {
            ssrc = gb28181Sdp.getSsrc();
        }
        String username = sdp.getOrigin().getUsername();
        String addressStr = sdp.getOrigin().getAddress();
        return DeviceRawContent.builder()
                .username(username)
                .addressStr(addressStr)
                .downloadSpeed(downloadSpeed)
                .port(port)
                .ssrc(ssrc)
                .mediaTransmissionTCP(mediaTransmissionTCP)
                .tcpActive(tcpActive)
                .sessionName(sessionName)
                .startTime(startTime)
                .stopTime(stopTime)
                .build();
    }

    /**
     * 流未上线处理
     */
    private void notifyPushStream(SIPRequest request, MediaServerVo mediaServerVo, ParentPlatformVo parentPlatformVo, DeviceRawContent deviceRawContent, GbStreamVo gbStreamVo, StreamProxyVo streamProxyVo, CallIdHeader callIdHeader, String channelId){
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        StreamProxyVoService streamProxyVoService = VideoService.getStreamProxyService();
        MediaHookSubscribe mediaHookSubscribe = MediaService.getMediaHookSubscribe();

        if(gbStreamVo.getStreamType() == StreamType.PROXY.getValue()){
            log.info("[ app={}, stream={} ]通道未推流，启用流后开始推流", gbStreamVo.getApp(), gbStreamVo.getStream());
            HookKey hookKey = HookKeyFactory.onStreamChanged(gbStreamVo.getApp(), gbStreamVo.getStream(), true, "rtsp", mediaServerVo.getId());
            //监听流上线
            mediaHookSubscribe.addSubscribe(hookKey,(media,response)->{
                OnStreamChangedHookVo vo = (OnStreamChangedHookVo) response;
                dynamicTask.stop(callIdHeader.getCallId());
                if(vo.isRegist()){
                    log.info("[上级点播]拉流代理已经就绪， {}/{}", vo.getApp(), vo.getStream());
                    pushStream(request, mediaServerVo, parentPlatformVo,deviceRawContent, gbStreamVo,callIdHeader,channelId);
                }else {
                    log.info("[上级点播]拉流代理已经注销， {}/{}", vo.getApp(), vo.getStream());
                }
                mediaHookSubscribe.removeSubscribe(hookKey);
            });
            //超时处理
            dynamicTask.startDelay(callIdHeader.getCallId(),videoProperties.getPlatformPlayTimeout(),()->{
                log.info("[ app={}, stream={} ] 等待拉流代理流超时", gbStreamVo.getApp(), gbStreamVo.getStream());
                mediaHookSubscribe.removeSubscribe(hookKey);
                try {
                    responseAck(request, Response.REQUEST_TIMEOUT,null); // 超时
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 超时任务发送失败: {}", e.getMessage());
                }
            });
            //开始拉流
            boolean start = streamProxyVoService.start(streamProxyVo);
            if(!start){
                try {
                    responseAck(request, Response.BUSY_HERE, "channel [" + gbStreamVo.getGbId() + "] offline");
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 通道未推流: {}", e.getMessage());
                }
                mediaHookSubscribe.removeSubscribe(hookKey);
                dynamicTask.stop(callIdHeader.getCallId());
            }
        }else if(gbStreamVo.getStreamType() == StreamType.PULL.getValue()){
            if (parentPlatformVo.getStartOfflinePush() == ConstEnum.Flag.NO.getValue()) {
                // 平台设置中关闭了拉起离线的推流则直接回复
                try {
                    log.info("[上级点播] 失败，推流设备未推流，channel: {}, app: {}, stream: {}", gbStreamVo.getGbId(), gbStreamVo.getApp(), gbStreamVo.getStream());
                    responseAck(request, Response.TEMPORARILY_UNAVAILABLE, "channel stream not pushing");
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 通道未推流: {}", e.getMessage());
                }
                return;
            }
            HookKey hookKey = HookKeyFactory.onStreamChanged(gbStreamVo.getApp(), gbStreamVo.getStream(), true, "rtsp", mediaServerVo.getId());
            //监听流上线
            mediaHookSubscribe.addSubscribe(hookKey,(media,response)->{
                //删除超时任务
                dynamicTask.stop(callIdHeader.getCallId());
                OnStreamChangedHookVo vo = (OnStreamChangedHookVo) response;
                if(vo.isRegist()){
                    log.info("[上级点播]推流代理已经注销， {}/{}", vo.getApp(), vo.getStream());
                    return;
                }
                log.info("[上级点播]推流代理已经就绪， {}/{}", vo.getApp(), vo.getStream());
                pushStream(request, mediaServerVo, parentPlatformVo,deviceRawContent, gbStreamVo,callIdHeader,channelId);
                mediaHookSubscribe.removeSubscribe(hookKey);
            });
            //超时处理
            dynamicTask.startDelay(callIdHeader.getCallId(),videoProperties.getPlatformPlayTimeout(),()->{
                log.info("[ app={}, stream={} ] 等待拉流代理流超时", gbStreamVo.getApp(), gbStreamVo.getStream());
                mediaHookSubscribe.removeSubscribe(hookKey);
                try {
                    responseAck(request, Response.REQUEST_TIMEOUT,null); // 超时
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 超时任务发送失败: {}", e.getMessage());
                }
            });
        }
    }

    /**
     * 安排推流
     */
    private void pushStream(SIPRequest request, MediaServerVo mediaServerVo, ParentPlatformVo parentPlatformVo, DeviceRawContent deviceRawContent, GbStreamVo gbStreamVo, CallIdHeader callIdHeader, String channelId){
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        MediaRestResult mediaList = MediaClient.getMediaList(mediaServerVo, "__defaultVhost__", null, gbStreamVo.getApp(), gbStreamVo.getStream());
        if(mediaList.getCode() == RespCode.CODE_0.getValue() && ObjectUtil.isNotEmpty(mediaList.getData())){
            SendRtp sendRtp = MediaClient.createSendRtp(mediaServerVo,deviceRawContent.getSessionName(), deviceRawContent.getStartTime(),deviceRawContent.getStopTime(),deviceRawContent.getAddressStr(), deviceRawContent.getPort(),deviceRawContent.getSsrc(),sipServer.getSipConfigProperties().getId(),parentPlatformVo.getServerGbId(),null,channelId,gbStreamVo.getApp(),gbStreamVo.getStream(), deviceRawContent.isMediaTransmissionTCP(),deviceRawContent.isTcpActive(),videoProperties.getServerId(),callIdHeader.getCallId(),ConstEnum.Flag.YES.getValue() == parentPlatformVo.getRtcp(),gbStreamVo.getStreamType()==StreamType.PROXY.getValue()?InviteStreamType.PROXY:InviteStreamType.PUSH);
            if (sendRtp == null) {
                log.warn("服务器端口资源不足");
                try {
                    responseAck(request, Response.BUSY_HERE,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] invite 服务器端口资源不足: {}", e.getMessage());
                }
                return;
            }
            sendRtp.setApp(gbStreamVo.getApp());
            sendRtp.setStreamId(gbStreamVo.getStream());
            sendRtp.setStatus(1);
            sendRtp.setFromTag(request.getFromTag());
            String sdp = createSdp(96, mediaServerVo, request, sendRtp);
            try {
                SIPResponse response =  responseSdpAck(request, sdp, parentPlatformVo);
                if (response != null) {
                    sendRtp.setToTag(response.getToTag());
                }
            } catch (InvalidArgumentException | SipException | ParseException e) {
                try {
                    responseAck(request, Response.GONE,"invite sdp消息发送失败");
                } catch (SipException | InvalidArgumentException | ParseException es) {
                    log.error("[命令发送失败] invite sdp消息发送失败: {}", es.getMessage());
                }
            }
            sendRtpManager.put(sendRtp);
        }
    }

    private String createSdp(int mediaFormat,MediaServerVo mediaServerVo, SIPRequest request, SendRtp sendRtp) {
        if(mediaFormat != 8 && mediaFormat != 96){
            try {
                responseAck(request, Response.GONE,"不支持此类消息发送");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] invite 不支持此类消息发送: {}", e.getMessage());
            }
            return null;
        }
        String videoType=mediaFormat==8?"audio":"video";
        String rtpmap = mediaFormat==8?"8 PCMA/8000":"96 PS/90000";
        StringBuffer content = new StringBuffer(200);
        content.append("v=0\r\n");
        content.append(String.format("o=%s  0 0 IN IP4 %s\r\n",sendRtp.getPlatformId(),mediaServerVo.getSdpIp()));
        content.append(String.format("s=%s\r\n",sendRtp.getSessionName()));
        content.append(String.format("c=IN IP4 %s\r\n",mediaServerVo.getSdpIp()));
        if ("Playback".equalsIgnoreCase(sendRtp.getSessionName())) {
            content.append(String.format("t=%s %s\r\n",sendRtp.getStartTime(),sendRtp.getStopTime()));
        } else {
            content.append("t=0 0\r\n");
        }
        content.append("t=0 0\r\n");
        if (sendRtp.isTcp()) {
            content.append(String.format("m=%s %s TCP/RTP/AVP %s\r\n",videoType,sendRtp.getLocalPort(),mediaFormat));
        }  else {
            content.append(String.format("m=%s %s RTP/AVP %s\r\n",videoType,sendRtp.getLocalPort(),mediaFormat));
        }
        content.append("a=sendonly\r\n");
        content.append(String.format("a=rtpmap:%s\r\n",rtpmap));
        if (sendRtp.isTcp()) {
            content.append("a=connection:new\r\n");
            if (!sendRtp.isTcpActive()) {
                content.append("a=setup:active\r\n");
            } else {
                content.append("a=setup:passive\r\n");
            }
        }
        content.append(String.format("y=%s\r\n",sendRtp.getSsrc()));
        content.append("f=\r\n");
        return content.toString();
    }
}
