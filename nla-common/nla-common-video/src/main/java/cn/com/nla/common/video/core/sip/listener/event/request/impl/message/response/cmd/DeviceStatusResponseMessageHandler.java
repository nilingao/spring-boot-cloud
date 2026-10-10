package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Map;

/**
 * 设备状态的回复
 */
@Slf4j
public class DeviceStatusResponseMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    public DeviceStatusResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.DEVICE_STATUS_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        log.info("接收到DeviceStatus应答消息");
        // 检查设备是否存在， 不存在则不回复
        if (deviceVo == null) {
            return;
        }
        DeviceVoService deviceVoService = VideoService.getDeviceService();

        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 设备状态应答回复200OK: {}", e.getMessage());
        }
        String channelId = XmlUtils.getText(element,"DeviceID");
        Map<String, Object> map = XmlUtil.xmlToMap(element);
        if (log.isDebugEnabled()) {
            log.debug("{}", map);
        }
        String text = XmlUtils.getText(element,"Online");
        if ("ONLINE".equalsIgnoreCase(text.trim())) {
            deviceVoService.online(deviceVo,sipServer,sipCommander,videoProperties,null,"设备状态的回复");
        }else {
            log.info("设备状态查询结果：" + text.trim());
            deviceVoService.offline(deviceVo.getDeviceId(),"设备状态的回复");
        }
        String key = String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_DEVICESTATUS,deviceVo.getDeviceId());
        deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),null,map));
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {

    }
}
