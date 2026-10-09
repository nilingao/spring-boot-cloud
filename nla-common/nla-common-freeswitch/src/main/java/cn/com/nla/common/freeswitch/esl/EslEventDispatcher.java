package cn.com.nla.common.freeswitch.esl;

import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ESL 事件分发器。
 * <p>
 * 替代 freeswitch-esl-spring-boot-starter 的 {@code IEslEventListenerTemplate}:
 * 收集容器内所有 {@link EslEventHandler}, 按其 {@link EslEventName} 声明的事件名建立路由表;
 * 收到事件时按 {@link EslEvent#getEventName()} 分发到对应处理器, 未匹配时回退到默认处理器
 * ({@link EslEventHandler#DEFAULT_ESL_EVENT_HANDLER})。初始化完成后将自身注册为
 * {@link InboundClient} 的事件监听器。
 *
 * @author TZY
 */
@Slf4j
public class EslEventDispatcher implements IEslEventListener, InitializingBean {

    private final InboundClient inboundClient;
    private final List<EslEventHandler> eslEventHandlers;
    private final Map<String, List<EslEventHandler>> handlerTable = new HashMap<>(16);
    /**
     * 默认兜底处理器: 未匹配到事件名时使用。若容器内存在
     * {@code @EslEventName(DEFAULT_ESL_EVENT_HANDLER)} 的处理器, 初始化时会被覆盖。
     */
    private EslEventHandler defaultEventHandler;

    public EslEventDispatcher(InboundClient inboundClient, List<EslEventHandler> eslEventHandlers) {
        this.inboundClient = inboundClient;
        this.eslEventHandlers = eslEventHandlers == null ? Collections.emptyList() : eslEventHandlers;
        this.defaultEventHandler = (addr, event) ->
            log.warn("当前事件未实现: addr:{}, EventName:{}", addr, event.getEventName());
    }

    @Override
    public void eventReceived(String addr, EslEvent event) {
        handleEslEvent(addr, event);
    }

    @Override
    public void backgroundJobResultReceived(String addr, EslEvent event) {
        handleEslEvent(addr, event);
    }

    private void handleEslEvent(String addr, EslEvent event) {
        String eventName = event.getEventName();
        List<EslEventHandler> handlers = handlerTable.get(eventName);
        if (!CollectionUtils.isEmpty(handlers)) {
            handlers.forEach(handler -> handler.handle(addr, event));
        } else {
            defaultEventHandler.handle(addr, event);
        }
    }

    @Override
    public void afterPropertiesSet() {
        log.info("EslEventDispatcher init ...");
        for (EslEventHandler handler : eslEventHandlers) {
            EslEventName annotation = handler.getClass().getAnnotation(EslEventName.class);
            if (annotation == null) {
                annotation = handler.getClass().getSuperclass().getAnnotation(EslEventName.class);
            }
            if (annotation == null || annotation.value().length == 0) {
                continue;
            }
            for (String eventName : annotation.value()) {
                if (eventName == null || eventName.isBlank()) {
                    continue;
                }
                log.info("EslEventDispatcher add EventName[{}], EventHandler[{}] ...", eventName, handler.getClass());
                if (EslEventHandler.DEFAULT_ESL_EVENT_HANDLER.equals(eventName)) {
                    defaultEventHandler = handler;
                } else {
                    handlerTable.computeIfAbsent(eventName, key -> new ArrayList<>(4)).add(handler);
                }
            }
        }
        inboundClient.option().addListener(this);
    }
}
