package cn.com.nla.common.video.core.sip.listener.event.request.impl.message;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.AbstractSipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.model.RestResultEvent;
import cn.com.nla.common.video.core.utils.SipUtils;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.SerializationUtils;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 消息类型处理器
 */
@Slf4j
public class MessageRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {

    @Autowired
    private SipSubscribeHandle sipSubscribeHandle;

    public Map<String, MessageHandler> messageHandlerMap;

    public MessageRequestProcessor(ObjectProvider<MessageHandlerAbstract> messageHandler){
        if(!messageHandler.stream().findAny().isPresent()){
            throw new IllegalStateException(" [SipConfig error] : MessageHandler is null");
        }
        this.messageHandlerMap = messageHandler.stream().collect(Collectors.toMap(MessageHandlerAbstract::getMessageType, o -> o, (u, v) -> {
            throw new IllegalStateException(String.format("[SipConfig error] :Duplicate key %s", u));
        }, ConcurrentHashMap::new));
    }

    @Override
    public String getMethod() {
        return Request.MESSAGE;
    }

    @Override
    public void process(RequestEvent evt) {
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();

        SIPRequest sipRequest = (SIPRequest)evt.getRequest();
        String deviceId = SipUtils.getUserIdFromFromHeader(evt.getRequest());
        CallIdHeader callIdHeader = sipRequest.getCallIdHeader();
        // 先从会话内查找
        SsrcTransaction ssrcTransaction = ssrcTransactionManager.getParamOne(null, null, callIdHeader.getCallId(), null,null);
        // 兼容海康 媒体通知 消息from字段不是设备ID的问题
        if (ssrcTransaction != null) {
            deviceId = ssrcTransaction.getDeviceId();
        }
        SIPRequest request = (SIPRequest) evt.getRequest();
        // 查询设备是否存在
        DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
        // 查询上级平台是否存在
        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(deviceId);
        if(deviceVo != null && parentPlatformVo != null){
            String hostAddress = request.getRemoteAddress().getHostAddress();
            int remotePort = request.getRemotePort();
            if (deviceVo.getHostAddress().equals(hostAddress + ":" + remotePort)) {
                parentPlatformVo = null;
            }else {
                deviceVo = null;
            }
        }
        try {
            if(deviceVo == null && parentPlatformVo == null){
                // 不存在则回复404
                responseAck(request, Response.NOT_FOUND, "device "+ deviceId +" not found");
                log.warn("[设备未找到 ]deviceId: {}, callId: {}", deviceId, callIdHeader.getCallId());
                String key = String.format("%s:%s", SipSubscribeHandle.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, callIdHeader.getCallId());
                RedisUtils.publish(key, SerializationUtils.serialize(new EventResult(new RestResultEvent(RespCode.CODE_2.getValue(),"[设备未找到 ]deviceId: {}",deviceId))));
                return;
            }
            Element rootElement = getRootElement(evt);
            if (rootElement == null) {
                log.error("处理MESSAGE请求  未获取到消息体{}", evt.getRequest());
                responseAck(request, Response.BAD_REQUEST, "content is null");
                return;
            }
            String name = rootElement.getNodeName();
            MessageHandler messageHandler = messageHandlerMap.get(name);
            if (messageHandler != null) {
                if (deviceVo != null) {
                    messageHandler.handForDevice(evt, deviceVo, rootElement);
                }else { // 由于上面已经判断都为null则直接返回，所以这里device和parentPlatform必有一个不为null
                    messageHandler.handForPlatform(evt, parentPlatformVo, rootElement);
                }
            }else {
                // 不支持的message
                // 不存在则回复415
                responseAck(request, Response.UNSUPPORTED_MEDIA_TYPE, "Unsupported message type, must Control/Notify/Query/Response");
            }
        } catch (SipException e) {
            log.error("SIP 回复错误", e);
        } catch (InvalidArgumentException e) {
            log.error("参数无效", e);
        } catch (ParseException e) {
            log.error("SIP回复时解析异常", e);
        }catch (Exception e){
            log.error("消息处理异常:",e);
        }
    }
}
