package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd;

import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.redis.subscribe.media.HookKeyFactory;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.redis.impl.InviteStreamManager;
import cn.com.nla.common.video.core.redis.impl.SendRtpManager;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.NotifyMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 媒体通知
 */
@Slf4j
public class MediaStatusNotifyMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private MediaHookSubscribe mediaHookSubscribe;

    public MediaStatusNotifyMessageHandler(NotifyMessageHandler handler){
        handler.setMessageHandler(CmdType.MEDIA_STATUS_NOTIFY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 录像流推送完毕，回复200OK: {}", e.getMessage());
        }
        CallIdHeader callIdHeader = (CallIdHeader)evt.getRequest().getHeader(CallIdHeader.NAME);
        String NotifyType = XmlUtils.getText(element, "NotifyType");
        if ("121".equals(NotifyType)){
            SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
            SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
            InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
            ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
            log.info("[录像流]推送完毕，收到关流通知");


            SsrcTransaction ssrcTransaction = ssrcTransactionManager.getParamOne(null, null, callIdHeader.getCallId(), null,null);
            if (ssrcTransaction != null) { // 兼容海康 媒体通知 消息from字段不是设备ID的问题
                log.info("[录像流]推送完毕，关流通知， device: {}, channelId: {}", ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId());
                InviteInfo inviteInfo = inviteStreamManager.getInviteInfo(VideoStreamType.download, ssrcTransaction.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getStream(),null);
                if (inviteInfo.getStreamInfo() != null) {
                    inviteInfo.getStreamInfo().setProgress(1);
                    inviteStreamManager.updateInviteInfo(inviteInfo);
                }
                try {
                    sipCommander.streamByeCmd(sipServer, deviceVo, ssrcTransaction.getChannelId(), ssrcTransaction.getStream(), callIdHeader.getCallId(),null,null,null);
                } catch (InvalidArgumentException | ParseException | SsrcTransactionNotFoundException | SipException e) {
                    log.error("[录像流]推送完毕，收到关流通知， 发送BYE失败 {}", e.getMessage());
                }
                // 去除监听流注销自动停止下载的监听
                HookKey hookSubscribe = HookKeyFactory.onStreamChanged("rtp", ssrcTransaction.getStream(), false, "rtsp", ssrcTransaction.getMediaServerId());
                mediaHookSubscribe.removeSubscribe(hookSubscribe);
                // 如果级联播放，需要给上级发送此通知 TODO 多个上级同时观看一个下级 可能存在停错的问题，需要将点播CallId进行上下级绑定
                SendRtp sendRtpItem =  sendRtpManager.querySendRTPServer(null, ssrcTransaction.getChannelId(), null, null);
                if (sendRtpItem != null) {
                    ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(sendRtpItem.getPlatform());
                    if (parentPlatformVo == null) {
                        log.warn("[级联消息发送]：发送MediaStatus发现上级平台{}不存在", sendRtpItem.getPlatform());
                        return;
                    }
                    try {
                        sipCommanderForPlatform.sendMediaStatusNotify(sipServer, parentPlatformVo, sendRtpItem,null,null);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 国标级联 录像播放完毕: {}", e.getMessage());
                    }
                }
            }else {
                log.info("[录像流]推送完毕，关流通知， 但是未找到对应的下载信息");
            }
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        // 不会收到上级平台的媒体通知
    }
}
