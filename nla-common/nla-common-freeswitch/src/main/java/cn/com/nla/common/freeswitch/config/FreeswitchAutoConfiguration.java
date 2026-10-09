package cn.com.nla.common.freeswitch.config;

import cn.com.nla.common.freeswitch.esl.EslEventDispatcher;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import cn.com.nla.common.freeswitch.esl.FreeswitchEslProperties;
import link.thingscloud.freeswitch.esl.IEslEventListener;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.ServerConnectionListener;
import link.thingscloud.freeswitch.esl.inbound.option.InboundClientOption;
import link.thingscloud.freeswitch.esl.inbound.option.ServerOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * FreeSWITCH 呼叫中心自动配置。
 * <p>
 * 自建封装替代 freeswitch-esl-spring-boot-starter 的 Boot2 {@code spring.factories} 机制
 * (Boot3+ 已移除 {@code spring.factories} 的 EnableAutoConfiguration 支持),
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册。
 * <p>
 * 提供 ESL 入站客户端 {@link InboundClient}({@code initMethod=start}/{@code destroyMethod=shutdown})
 * 与事件分发器 {@link IEslEventListener} 两个核心 Bean; 事件处理器 {@link EslEventHandler}
 * 由消费方组件扫描注册, 分发器按 {@code @EslEventName} 路由。
 *
 * @author TZY
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(InboundClient.class)
@ConditionalOnProperty(value = "freeswitch.enabled", havingValue = "true")
@EnableConfigurationProperties(FreeswitchEslProperties.class)
public class FreeswitchAutoConfiguration {

    /**
     * ESL 事件分发器: 收集容器内所有 {@link EslEventHandler}, 按 {@code @EslEventName} 事件名路由,
     * 初始化后注册为 {@link InboundClient} 的事件监听器。
     */
    @Bean
    @ConditionalOnMissingBean(IEslEventListener.class)
    public IEslEventListener eslEventListener(InboundClient inboundClient, List<EslEventHandler> eslEventHandlers) {
        return new EslEventDispatcher(inboundClient, eslEventHandlers);
    }

    /**
     * FreeSWITCH ESL 入站客户端。
     * <p>
     * 替代 freeswitch-esl-spring-boot-starter 的 {@code FreeswitchEslAutoConfiguration#inboundClient}:
     * 由 {@link FreeswitchEslProperties} 构建 {@link InboundClientOption}(默认值与 starter 一致),
     * 通过 {@code initMethod=start} 建立连接, {@code destroyMethod=shutdown} 释放资源。
     */
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(InboundClient.class)
    public InboundClient inboundClient(FreeswitchEslProperties properties, ServerConnectionListener serverConnectionListener) {
        InboundClientOption option = new InboundClientOption()
            .sndBufSize(properties.getSndBufSize())
            .rcvBufSize(properties.getRcvBufSize())
            .workerGroupThread(properties.getWorkerGroupThread())
            .publicExecutorThread(properties.getPublicExecutorThread())
            .privateExecutorThread(properties.getPrivateExecutorThread())
            .callbackExecutorThread(properties.getCallbackExecutorThread())
            .defaultTimeoutSeconds(properties.getDefaultTimeoutSeconds())
            .readTimeoutSeconds(properties.getReadTimeoutSeconds())
            .readerIdleTimeSeconds(properties.getReaderIdleTimeSeconds())
            .defaultPassword(properties.getDefaultPassword())
            .disablePublicExecutor(properties.isDisablePublicExecutor())
            .performance(properties.isPerformance())
            .performanceCostTime(properties.getPerformanceCostTime())
            .eventPerformance(properties.isEventPerformance())
            .eventPerformanceCostTime(properties.getEventPerformanceCostTime());
        properties.getServers().forEach(server -> {
            if (server.getHost() != null && !server.getHost().isBlank() && server.getPort() > 1) {
                option.addServerOption(new ServerOption(server.getHost(), server.getPort())
                    .timeoutSeconds(server.getTimeoutSeconds())
                    .password(server.getPassword()));
            }
        });
        properties.getEvents().forEach(event -> {
            if (event != null && !event.isBlank()) {
                option.addEvents(event);
            }
        });
        option.serverConnectionListener(serverConnectionListener);
        log.info("inboundClient option : [{}]", option);
        return InboundClient.newInstance(option);
    }
}
