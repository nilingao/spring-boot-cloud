package cn.com.nla.common.freeswitch.client.fs.handler.event;

import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@EslEventName(EslEventHandler.DEFAULT_ESL_EVENT_HANDLER)
@Component
public class DefaultEslEventHandler implements EslEventHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(String addr, EslEvent event) {
        //默认未实现的实现处理类
        log.warn("当前事件未实现： 事件：{} addr：{},EventName:{}","DEFAULT_ESL_EVENT_HANDLER",addr, EslEventUtil.getEventName(event));
    }
}