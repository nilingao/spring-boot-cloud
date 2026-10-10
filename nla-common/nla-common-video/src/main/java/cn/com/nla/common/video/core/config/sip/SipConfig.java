package cn.com.nla.common.video.core.config.sip;

import cn.com.nla.common.video.core.config.runner.SipDeviceRunner;
import cn.com.nla.common.video.core.config.runner.SipPlatformRunner;
import cn.com.nla.common.video.core.config.runner.SipServerRunner;
import cn.com.nla.common.video.core.config.runner.ZLMRunner;
import cn.com.nla.common.video.core.media.client.ZlmService;
import cn.com.nla.common.video.core.media.hook.MediaHookServer;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.impl.*;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.redis.subscribe.notify.DeviceNotifyHandle;
import cn.com.nla.common.video.core.redis.subscribe.record.RecordEndSubscribeHandle;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipMessageHandle;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.redis.subscribe.sip.register.SipRegisterHandle;
import cn.com.nla.common.video.core.service.PlayService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.impl.SIPCommanderFroPlatformImpl;
import cn.com.nla.common.video.core.sip.cmd.impl.SIPCommanderImpl;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.*;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageRequestProcessor;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.control.ControlMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.control.cmd.DeviceControlQueryMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.NotifyMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd.AlarmNotifyMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd.KeepaliveNotifyMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd.MediaStatusNotifyMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd.MobilePositionNotifyMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.QueryMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.cmd.*;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd.*;
import cn.com.nla.common.video.core.sip.listener.event.response.impl.ByeResponseProcessor;
import cn.com.nla.common.video.core.sip.listener.event.response.impl.CancelResponseProcessor;
import cn.com.nla.common.video.core.sip.listener.event.response.impl.InviteResponseProcessor;
import cn.com.nla.common.video.core.sip.listener.event.response.impl.RegisterResponseProcessor;
import cn.com.nla.common.video.core.sip.listener.event.timeout.impl.SipTimeoutEventImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import cn.com.nla.common.video.core.service.authentication.CurrentUserProvider;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * SIP注册中心
 */
@Import({
        SipServer.class, DeferredResultHolder.class,
        ZlmService.class, MediaHookServer.class, MediaHookSubscribe.class,
        PlatformRegisterManager.class, SendRtpManager.class, SipTransactionManager.class, SsrcConfigManager.class, SsrcTransactionManager.class, MediaServerManager.class, InviteStreamManager.class, CseqManager.class,RegisterServerManager.class, PlatformNotifySubscribeManager.class, CatalogDataManager.class,StreamChangedManager.class,RecordMp4Manager.class,
        SipMessageHandle.class, SipRegisterHandle.class,
        MessageRequestProcessor.class,AckRequestProcessor.class, ByeRequestProcessor.class, CancelRequestProcessor.class, InfoRequestProcessor.class, InviteRequestProcessor.class, NotifyRequestProcessor.class, RegisterRequestProcessor.class, SubscribeRequestProcessor.class,
        ControlMessageHandler.class, DeviceControlQueryMessageHandler.class,
        NotifyMessageHandler.class, AlarmNotifyMessageHandler.class, KeepaliveNotifyMessageHandler.class, MediaStatusNotifyMessageHandler.class, MobilePositionNotifyMessageHandler.class,
        QueryMessageHandler.class, AlarmQueryMessageHandler.class, CatalogQueryMessageHandler.class, DeviceInfoQueryMessageHandler.class, DeviceStatusQueryMessageHandler.class, RecordInfoQueryMessageHandler.class,
        ResponseMessageHandler.class, AlarmResponseMessageHandler.class, BroadcastResponseMessageHandler.class, CatalogResponseMessageHandler.class, DeviceConfigResponseMessageHandler.class, ConfigDownloadResponseMessageHandler.class, DeviceControlResponseMessageHandler.class, DeviceInfoResponseMessageHandler.class, DeviceStatusResponseMessageHandler.class, MobilePositionResponseMessageHandler.class, PresetQueryResponseMessageHandler.class, RecordInfoResponseMessageHandler.class,
        ByeResponseProcessor.class, CancelResponseProcessor.class, InviteResponseProcessor.class, RegisterResponseProcessor.class,
        SipServerRunner.class, ZLMRunner.class,SipPlatformRunner.class, SipDeviceRunner.class,
        SipTimeoutEventImpl.class,
        SipSubscribeHandle.class, RecordEndSubscribeHandle.class, DeviceNotifyHandle.class, DeviceNotifySubscribeManager.class,
        DynamicTask.class, PlayService.class,
        SIPCommanderImpl.class, SIPCommanderFroPlatformImpl.class,
})
@AutoConfiguration
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@EnableConfigurationProperties({VideoProperties.class, SipConfigProperties.class})
@RequiredArgsConstructor
public class SipConfig {
    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    CurrentUserProvider videoCurrentUserProvider() { return () -> null; }

}
