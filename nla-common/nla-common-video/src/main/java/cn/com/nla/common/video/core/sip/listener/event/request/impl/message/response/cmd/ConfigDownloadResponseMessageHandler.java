package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
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
 * 设备配置查询的回复
 */
@Slf4j
public class ConfigDownloadResponseMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    public ConfigDownloadResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.CONFIG_DOWNLOAD_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        String channelId = XmlUtils.getText(element, "DeviceID");
        String key = String.format("%s%s_%s",DeferredResultHolder.CALLBACK_CMD_CONFIGDOWNLOAD,deviceVo.getDeviceId(),channelId);
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 设备配置查询: {}", e.getMessage());
        }
        // 此处是对本平台发出DeviceControl指令的应答
        Map<String, Object> map = XmlUtil.xmlToMap(element);
        if (log.isDebugEnabled()) {
            log.debug("{}", map);
        }
        Map<String, Object> basicParam = BeanUtil.beanToMap("BasicParam");
        if(ObjectUtil.isNotEmpty(basicParam)){
            DeviceVo build = DeviceVo.builder()
                    .deviceId(deviceVo.getDeviceId())
                    .heartBeatInterval(MapUtil.getInt(basicParam, "heartBeatInterval", 60))
                    .heartBeatCount(MapUtil.getInt(basicParam, "heartBeatCount", 2))
                    .positionCapability(MapUtil.getInt(basicParam, "positionCapability", 2))
                    .build();
            VideoService.getDeviceService().save(build);
        }
        deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),null,map));
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        // 不会收到上级平台的设备配置查询
    }
}
