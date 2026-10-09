package cn.com.nla.common.freeswitch.client.sip.listener.request.impl;

import cn.com.nla.common.freeswitch.client.sip.listener.request.AbstractSipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.utils.SipUtils;
import cn.com.nla.common.freeswitch.utils.XmlUtils;
import cn.com.nla.common.freeswitch.vo.fs.AgentNotifyVo;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.EventHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.Map;

/**
 * SIP命令类型： NOTIFY请求,这是作为上级发送订阅请求后，设备才会响应的
 */
@Slf4j
@Component
public class NotifyRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {

    @Override
    public String getMethod() {
        return Request.NOTIFY;
    }



    @Override
    public void process(RequestEvent event) {
        SIPRequest request = (SIPRequest) event.getRequest();
        //回复订阅已接收
        try {
            responseAck(request, Response.OK, null);
        }catch (SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }
        EventHeader eventHeader = (EventHeader)request.getHeader(EventHeader.NAME);
        if (eventHeader == null) {
            log.error("处理NOTIFY消息时未获取到通知类型,{}", event.getRequest());
            return;
        }
        try {
           if(StringUtils.equals(AgentNotifyVo.TypeEnum.PRESENCE.getName(),eventHeader.getEventType())){
               processPresence(event);
           }else if(StringUtils.equals(AgentNotifyVo.TypeEnum.MESSAGE_SUMMARY.getName(),eventHeader.getEventType())){
                //该消息的目的是在话机登陆的时候，即通知话机目前有几个未读的消息
               processMessageSummary(event);
           }else {
               log.info("接收到消息：" + eventHeader.getEventType());
           }
        }catch (Exception e){
            log.error("[NOTIFY请求]，消息处理异常：", e );
        }
    }
    private void processPresence(RequestEvent event){
        SIPRequest request = (SIPRequest) event.getRequest();
        Element rootElement = getRootElement(event);
        if (rootElement == null) {
            log.error("处理 Presence 消息时未获取到消息体,{}", event.getRequest());
            return;
        }
        String agentCode = SipUtils.getUserIdFromHeader(request);
        Map<String, Object> stringObjectMap = XmlUtils.node2Json(rootElement);
        log.warn("[PresenceNotify请求 ] agentCode:{},data:{}",agentCode,stringObjectMap);
    }

    private void processMessageSummary(RequestEvent event) throws UnsupportedEncodingException {
        SIPRequest request = (SIPRequest) event.getRequest();
        String agentCode = SipUtils.getUserIdFromHeader(request);
        String content = request.getMessageContent();
        log.warn("[MessageSummaryNotify请求 ] agentCode:{},data:{}",agentCode,content);
    }
}
