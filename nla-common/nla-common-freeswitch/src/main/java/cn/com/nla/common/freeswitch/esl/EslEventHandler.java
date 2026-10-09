package cn.com.nla.common.freeswitch.esl;

import link.thingscloud.freeswitch.esl.transport.event.EslEvent;

/**
 * FreeSWITCH ESL 事件处理器。
 * <p>
 * 替代 freeswitch-esl-spring-boot-starter 的 {@code EslEventHandler}
 * (Boot2 {@code spring.factories} 机制在 Boot3+ 已失效)。
 * 实现类通过 {@link EslEventName} 声明关注的事件名, 由 {@link EslEventDispatcher} 统一分发。
 *
 * @author TZY
 */
public interface EslEventHandler {

    /**
     * 默认事件处理器标识: 未匹配到任何事件名时回退到该处理器。
     */
    String DEFAULT_ESL_EVENT_HANDLER = "DEFAULT_ESL_EVENT_HANDLER";

    /**
     * 子事件头名称 (如 CUSTOM 事件的 Event-Subclass)。
     */
    String SUB_EVENT_HEADER_KEY = "Event-Subclass";

    /**
     * 处理 ESL 事件。
     *
     * @param addr  事件来源 FreeSWITCH 服务地址
     * @param event ESL 事件
     */
    void handle(String addr, EslEvent event);
}
