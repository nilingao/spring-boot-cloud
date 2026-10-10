package cn.com.nla.common.video.core.sip.cmd;

import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.MessageTypeVo;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.sip.SipServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;

import javax.sip.header.CallIdHeader;
import javax.sip.message.Message;
import java.util.Objects;

/**
 * 消息发送类
 */
@Slf4j
public class SipSendMessage {

    /**
     * 处理上级发送消息事件
     */
    public static void sendMessage(SipServer sipServer, DeviceVo vo, Message message, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent){
        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        //处理回调事件
        handleEvent(sipServer,callIdHeader.getCallId(),okEvent,errorEvent);
        RedisUtils.publish(VideoConstant.VIDEO_SEND_SIP_MESSAGE, Objects.requireNonNull(SerializationUtils.serialize(MessageTypeVo.builder().type(MessageTypeVo.TypeEnum.DEVICE.getValue()).gbId(vo.getDeviceId()).message(message).build())));
    }
    /**
     * 处理上级发送消息事件
     */
    public static void sendMessage(SipServer sipServer, ParentPlatformVo vo, Message message, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent){
        CallIdHeader callIdHeader = (CallIdHeader) message.getHeader(CallIdHeader.NAME);
        handleEvent(sipServer,callIdHeader.getCallId(),okEvent,errorEvent);
        RedisUtils.publish(VideoConstant.VIDEO_SEND_SIP_MESSAGE, Objects.requireNonNull(SerializationUtils.serialize(MessageTypeVo.builder().type(MessageTypeVo.TypeEnum.PLATFORM.getValue()).gbId(vo.getServerGbId()).message(message).build())));
    }

    public static void handleEvent(SipServer sipServer,String callId,SipSubscribeEvent okEvent,SipSubscribeEvent errorEvent){
        SipSubscribeHandle sipSubscribeHandle = sipServer.getSubscribeManager();
        // 添加成功订阅
        if (okEvent != null) {
            sipSubscribeHandle.addOkSubscribe(callId, eventResult -> {
                okEvent.response(eventResult);
                sipSubscribeHandle.removeAllSubscribe(eventResult.getCallId());
            });
        }
        // 添加错误订阅
        if (errorEvent != null) {
            sipSubscribeHandle.addErrorSubscribe(callId, (eventResult -> {
                errorEvent.response(eventResult);
                sipSubscribeHandle.removeAllSubscribe(eventResult.getCallId());
            }));
        }
    }
}

