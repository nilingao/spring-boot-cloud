package cn.com.nla.common.freeswitch.esl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 ESL 事件处理器关注的事件名。
 * <p>
 * 替代 freeswitch-esl-spring-boot-starter 的 {@code @EslEventName}。可绑定多个事件名,
 * {@link EslEventDispatcher} 收到事件时按事件名路由到对应处理器。
 *
 * @author TZY
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EslEventName {

    /**
     * 关注的事件名, 取值参考 {@code link.thingscloud.freeswitch.esl.constant.EventNames},
     * 或 {@link EslEventHandler#DEFAULT_ESL_EVENT_HANDLER} 表示默认兜底处理器。
     */
    String[] value();
}
