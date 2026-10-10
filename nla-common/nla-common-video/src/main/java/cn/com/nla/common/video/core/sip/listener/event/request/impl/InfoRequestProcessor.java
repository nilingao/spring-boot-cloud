package cn.com.nla.common.video.core.sip.listener.event.request.impl;

import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.redis.impl.InviteStreamManager;
import cn.com.nla.common.video.core.redis.impl.SendRtpManager;
import cn.com.nla.common.video.core.sip.listener.event.request.AbstractSipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.utils.SipUtils;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 回放控制
 */
@Slf4j
public class InfoRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {


    @Override
    public String getMethod() {return Request.INFO;}


    @Override
    public void process(RequestEvent evt) {
        log.debug("接收到消息：" + evt.getRequest());
        //业务开始 获取相关实现业务接口
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        if(deviceVoService == null){
            log.error(" DeviceService not implements ");
            return;
        }
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        if(parentPlatformVoService == null){
            log.error(" ParentPlatformService not implements ");
            return;
        }
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();

        SIPRequest request = (SIPRequest) evt.getRequest();
        String deviceId = SipUtils.getUserIdFromFromHeader(request); //可能是设备，也可能是上级平台
        CallIdHeader callIdHeader = request.getCallIdHeader(); //请求唯一标识
        // 先从会话内查找
        SsrcTransaction ssrcTransaction = ssrcTransactionManager.getParamOne(null, null, callIdHeader.getCallId(), null,null);
        // 兼容海康 媒体通知 消息from字段不是设备ID的问题
        if (ssrcTransaction != null) {
            deviceId = ssrcTransaction.getDeviceId();
        }
        // 查询设备是否存在
        DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
        // 查询上级平台是否存在
        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(deviceId);
        try {
            if (deviceVo != null && parentPlatformVo != null) {
                log.warn("[重复]平台与设备编号重复：{}", deviceId);
                String hostAddress = request.getRemoteAddress().getHostAddress();
                int remotePort = request.getRemotePort();
                if (deviceVo.getHostAddress().equals(hostAddress + ":" + remotePort)) {
                    parentPlatformVo = null;
                }else {
                    deviceVo = null;
                }
            }
            if (deviceVo == null && parentPlatformVo == null) {
                // 不存在则回复404
                responseAck(request, Response.NOT_FOUND, "device "+ deviceId +" not found");
                log.warn("[设备未找到 ]： {}", deviceId);
            }else {
                ContentTypeHeader header = (ContentTypeHeader)evt.getRequest().getHeader(ContentTypeHeader.NAME);
                String contentType = header.getContentType();
                String contentSubType = header.getContentSubType();
                if ("Application".equalsIgnoreCase(contentType) && "MANSRTSP".equalsIgnoreCase(contentSubType)) {
                    SendRtp sendRtp = sendRtpManager.querySendRTPServer(null, null, null, callIdHeader.getCallId());
                    String streamId = sendRtp.getStreamId();
                    InviteInfo inviteInfo= inviteStreamManager.getInviteInfoByStream(VideoStreamType.playback, streamId);
                    if (null == inviteInfo) {
                        responseAck(request, Response.NOT_FOUND, "stream " + streamId + " not found");
                        return;
                    }
                    if(inviteInfo.getStreamInfo() == null){
                        responseAck(request, Response.NOT_FOUND, "streamInfo " + streamId + " not found");
                        return;
                    }
                    DeviceVo deviceGbId = deviceVoService.findDeviceGbId(inviteInfo.getDeviceId());
                    sipCommander.playbackControlCmd(sipServer, deviceGbId,inviteInfo.getStreamInfo(),new String(evt.getRequest().getRawContent()), okEvent -> {
                        // 成功的回复
                        try {
                            responseAck(request, okEvent.getStatusCode(),okEvent.getMsg());
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[命令发送失败] 国标级联 录像控制: {}", e.getMessage());
                        }
                    }, errorEvent -> {
                        // 失败的回复
                        try {
                            responseAck(request, errorEvent.getStatusCode(), errorEvent.getMsg());
                        } catch (SipException | InvalidArgumentException | ParseException e) {
                            log.error("[命令发送失败] 国标级联 录像控制: {}", e.getMessage());
                        }
                    });
                }
            }
        } catch (SipException e) {
            log.warn("SIP 回复错误", e);
        } catch (InvalidArgumentException e) {
            log.warn("参数无效", e);
        } catch (ParseException e) {
            log.warn("SIP回复时解析异常", e);
        }
    }


}
