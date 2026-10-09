package cn.com.nla.common.freeswitch.client.fs.handler.event;

import link.thingscloud.freeswitch.esl.constant.EventNames;
import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 按键收号
 */
@Slf4j
@Component
@EslEventName(EventNames.DTMF)
public class DtmfEventHandler implements EslEventHandler {
    @Override
    public void handle(String addr, EslEvent event) {
        log.info("进入事件  [按键收号]   DTMF");
        log.info("按键收号 暂未实现:{}", event.getEventHeaders());
    }
}
