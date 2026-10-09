package cn.com.nla.common.freeswitch.client.sip.listener.request.impl;


import cn.com.nla.common.freeswitch.client.sip.listener.request.AbstractSipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.sip.RequestEvent;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

/**
 * 订阅请求事件
 */
@Slf4j
@Component
public class SubscribeRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {
    @Override
    public String getMethod() {
        return Request.SUBSCRIBE;
    }

    @Override
    public void process(RequestEvent event) {
        SIPRequest request = (SIPRequest) event.getRequest();
        Element rootElement = getRootElement(event);
        if (rootElement == null) {
            log.error("处理SUBSCRIBE请求  未获取到消息体{}", event.getRequest());
            return;
        }
        //接收到订阅消息，暂无此订阅消息 因为是请求上级 我们对上级发送订阅 不是上级对我们发送
        try {
            log.info("接收到订阅消息消息：" + rootElement.toString());
            Response response = sipServer.getSipFactory().createMessageFactory().createResponse(200, request);
            if(response != null){
                ExpiresHeader expiresHeader = sipServer.getSipFactory().createHeaderFactory().createExpiresHeader(30);
                response.setExpires(expiresHeader);
            }
            log.info("response : " + response);
            sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(),response);
        }catch (Exception e){
            log.error("[订阅请求事件发生错误]，消息处理异常：", e );
        }
    }


}
