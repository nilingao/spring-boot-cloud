package cn.com.nla.common.freeswitch.client.sip.listener.response.impl;

import cn.com.nla.common.freeswitch.client.sip.listener.response.AbstractSipResponseEvent;
import cn.com.nla.common.json.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

/**
 * @description: BYE请求响应器
 */
@Slf4j
@Component
public class ByeResponseProcessor extends AbstractSipResponseEvent {

    @Override
    public String getMethod() {
        return Request.BYE;
    }

    @Override
    public void process(ResponseEvent event) {
        // TODO Auto-generated method stub
        log.info("BYE请求响应器 值:{}", JsonUtils.toJsonString(event));
        //回复200
    }
}
