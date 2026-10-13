package cn.com.nla.common.freeswitch.config;

import cn.com.nla.common.freeswitch.client.fs.handler.EventNextHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ChannelAnswerEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ChannelBridgeEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ChannelCustomeventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ChannelExecuteCompleteEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ChannelHangupCompleteEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ChannelParkEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.DefaultEslEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.DtmfEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.PlaybackStopEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.RecordStartEventHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.event.ServerConnectionListenerImpl;
import cn.com.nla.common.freeswitch.client.fs.handler.process.ProcessNextHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.strategy.CallStrategyHandler;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.impl.SIPCommanderFroPlatformImpl;
import cn.com.nla.common.freeswitch.client.sip.cmd.impl.SIPCommanderImpl;
import cn.com.nla.common.freeswitch.client.sip.listener.SipListenerImpl;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.AckRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.ByeRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.CancelRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.InviteRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.NotifyRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.OptionsRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.RegisterRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.request.impl.SubscribeRequestProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.response.impl.ByeResponseProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.response.impl.CancelResponseProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.response.impl.InviteResponseProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.response.impl.RegisterResponseProcessor;
import cn.com.nla.common.freeswitch.client.sip.listener.timeout.impl.SipTimeoutEventImpl;
import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import cn.com.nla.common.freeswitch.client.sip.properties.VideoProperties;
import cn.com.nla.common.freeswitch.config.fs.GroupProcessRunner;
import cn.com.nla.common.freeswitch.config.sip.SipServerRunner;
import cn.com.nla.common.freeswitch.esl.EslEventDispatcher;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import cn.com.nla.common.freeswitch.esl.FreeswitchEslProperties;
import cn.com.nla.common.freeswitch.redis.impl.fs.AgentInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.AgentNotifySubscribeManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.CallInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.CompanyConferenceInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.CompanyInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.DeviceInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.GroupInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.PlaybackInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.fs.VdnPhoneManager;
import cn.com.nla.common.freeswitch.redis.impl.result.DeferredResultHolder;
import cn.com.nla.common.freeswitch.redis.impl.sip.CseqManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.PlatformRegisterManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.RegisterServerManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.SipTransactionManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.SsrcTransactionManager;
import cn.com.nla.common.freeswitch.redis.subscribe.notify.AgentLoginSubscribe;
import cn.com.nla.common.freeswitch.redis.subscribe.notify.AgentNotifyHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.AgentSubscribeHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipMessageHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.register.SipRegisterHandle;
import cn.com.nla.common.freeswitch.utils.DynamicTask;
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
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * FreeSWITCH 呼叫中心自动配置。
 * <p>
 * 自建封装替代 freeswitch-esl-spring-boot-starter 的 Boot2 {@code spring.factories} 机制
 * (Boot3+ 已移除 {@code spring.factories} 的 EnableAutoConfiguration 支持),
 * 通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册。
 * <p>
 * 收编模式对齐 {@code nla-common-video} 的 {@code SipConfig}: 协议组件一律不标 {@code @Component},
 * 由本类的 {@link Import} 显式清单注册, 配合 {@code @ConditionalOnProperty(freeswitch.enabled=true)}
 * 实现整体门控——消费方(如 nla-admin 的全包组件扫描)在未启用呼叫中心时不会误注册依赖
 * {@link InboundClient} 的组件, 避免 5.1.1 记录过的启动期 Bean 缺失崩溃。
 * <p>
 * 提供 ESL 入站客户端 {@link InboundClient}({@code initMethod=start}/{@code destroyMethod=shutdown})、
 * 事件分发器 {@link IEslEventListener} 与流程编排 {@link ProcessNextHandler} 三个核心 Bean;
 * ESL 事件处理器与 SIP 处理器同样经 {@link Import} 收编, 分发器按 {@code @EslEventName} 路由。
 *
 * @author TZY
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(InboundClient.class)
@ConditionalOnProperty(value = "freeswitch.enabled", havingValue = "true")
@EnableConfigurationProperties({FreeswitchEslProperties.class, SipConfigProperties.class, VideoProperties.class})
@Import({
    // 公共任务调度
    DynamicTask.class,
    // ESL 事件处理链
    EventNextHandler.class,
    ChannelAnswerEventHandler.class,
    ChannelBridgeEventHandler.class,
    ChannelCustomeventHandler.class,
    ChannelExecuteCompleteEventHandler.class,
    ChannelHangupCompleteEventHandler.class,
    ChannelParkEventHandler.class,
    DefaultEslEventHandler.class,
    DtmfEventHandler.class,
    PlaybackStopEventHandler.class,
    RecordStartEventHandler.class,
    ServerConnectionListenerImpl.class,
    // SIP 服务端栈
    SipServer.class,
    SIPCommanderFroPlatformImpl.class,
    SIPCommanderImpl.class,
    SipListenerImpl.class,
    AckRequestProcessor.class,
    ByeRequestProcessor.class,
    CancelRequestProcessor.class,
    InviteRequestProcessor.class,
    NotifyRequestProcessor.class,
    OptionsRequestProcessor.class,
    RegisterRequestProcessor.class,
    SubscribeRequestProcessor.class,
    ByeResponseProcessor.class,
    CancelResponseProcessor.class,
    InviteResponseProcessor.class,
    RegisterResponseProcessor.class,
    SipTimeoutEventImpl.class,
    // 启动 Runner
    GroupProcessRunner.class,
    SipServerRunner.class,
    // Redis 缓存管理器
    AgentInfoManager.class,
    AgentNotifySubscribeManager.class,
    CallInfoManager.class,
    CompanyConferenceInfoManager.class,
    CompanyInfoManager.class,
    DeviceInfoManager.class,
    GroupInfoManager.class,
    PlaybackInfoManager.class,
    VdnPhoneManager.class,
    DeferredResultHolder.class,
    CseqManager.class,
    PlatformRegisterManager.class,
    RegisterServerManager.class,
    SipTransactionManager.class,
    SsrcTransactionManager.class,
    // Redis 订阅处理器
    AgentLoginSubscribe.class,
    AgentNotifyHandle.class,
    AgentSubscribeHandle.class,
    SipMessageHandle.class,
    SipSubscribeHandle.class,
    SipRegisterHandle.class
})
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

    /**
     * 呼叫流程编排器: 按技能组策略将排队呼叫分配给坐席。
     * <p>
     * 自旧工程 {@code FreeswitchConfig#processNextHandler} 迁移——该配置类删除后由本类承接,
     * 策略处理器 {@link CallStrategyHandler} 非 Bean, 在此以构造参数方式组装。
     */
    @Bean
    @ConditionalOnMissingBean(ProcessNextHandler.class)
    public ProcessNextHandler processNextHandler(InboundClient inboundClient) {
        return new ProcessNextHandler(new CallStrategyHandler(inboundClient));
    }
}
