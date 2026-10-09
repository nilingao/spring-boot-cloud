package cn.com.nla.common.freeswitch.client.sip.listener.request.impl;

import cn.com.nla.common.freeswitch.client.sip.listener.request.AbstractSipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.RequestEvent;
import javax.sip.message.Request;

/**
 * SIP命令类型： ACK请求
 * 给上级平台进行推流
 */
@Slf4j
@Component
public class AckRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {


    @Override
    public String getMethod() {
        return Request.ACK;
    }
    /**
     * 处理  ACK请求
     * @param evt
     */
    @Override
    public void process(RequestEvent evt) {

    }

}
