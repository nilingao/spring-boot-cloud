package cn.com.nla.common.video.core.sip.listener.event.response.impl;

import cn.com.nla.common.video.core.sip.listener.event.response.AbstractSipResponseEvent;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sip.ResponseEvent;
import javax.sip.message.Request;

/**
 * @description: CANCEL响应处理器
 */
@Slf4j
public class CancelResponseProcessor  extends AbstractSipResponseEvent {
    @Override
    public String getMethod() {
        return Request.CANCEL;
    }

    @Override
    public void process(ResponseEvent event) {
        // TODO Auto-generated method stub
        log.info("CANCEL响应处理器 值:{}", JSONUtil.toJsonPrettyStr(event));
    }
}
