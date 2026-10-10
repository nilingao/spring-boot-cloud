package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.cmd;

import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.QueryMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 报警查询
 */
@Slf4j
public class AlarmQueryMessageHandler extends SipResponseEvent implements MessageHandler {

    public AlarmQueryMessageHandler(QueryMessageHandler handler){
        handler.setMessageHandler(CmdType.ALARM_QUERY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        log.info("不支持alarm查询");
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.NOT_FOUND, "not support alarm query");
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 alarm查询回复200OK: {}", e.getMessage());
        }
    }
}
