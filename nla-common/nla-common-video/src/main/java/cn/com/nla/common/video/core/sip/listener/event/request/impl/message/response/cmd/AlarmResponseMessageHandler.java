package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.RequestEvent;
import java.util.Map;

/**
 * 报警回复信息
 */
@Slf4j
public class AlarmResponseMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private DeferredResultHolder deferredResultHolder;

    public AlarmResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.ALARM_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        String channelId = XmlUtils.getText(element,"DeviceID");
        String key = String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_ALARM,deviceVo.getDeviceId());
        Map<String, Object> map = XmlUtil.xmlToMap(element);
        if (log.isDebugEnabled()) {
            log.debug("{}", map);
        }
        deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),null,map));
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        //没有上级回复
    }
}
