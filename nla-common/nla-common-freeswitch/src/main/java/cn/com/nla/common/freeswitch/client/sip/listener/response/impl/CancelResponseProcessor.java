package cn.com.nla.common.freeswitch.client.sip.listener.response.impl;

import cn.com.nla.common.freeswitch.client.sip.listener.response.AbstractSipResponseEvent;
import cn.com.nla.common.json.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

/**
 * @description: CANCEL响应处理器
 */
@Slf4j
@Component
public class CancelResponseProcessor  extends AbstractSipResponseEvent {
    @Override
    public String getMethod() {
        return Request.CANCEL;
    }

    @Override
    public void process(ResponseEvent event) {
        // TODO Auto-generated method stub
        log.info("CANCEL响应处理器 值:{}", JsonUtils.toJsonString(event));
    }
}
