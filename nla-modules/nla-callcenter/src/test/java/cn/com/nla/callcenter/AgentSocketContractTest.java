package cn.com.nla.callcenter;

import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.callcenter.socket.AgentNamespace;
import cn.com.nla.callcenter.socket.event.AgentInCallPhoneEvent;
import cn.com.nla.callcenter.socket.event.AgentInHangUpPhoneEvent;
import cn.com.nla.callcenter.socket.event.AgentInPhoneNotificationEvent;
import cn.com.nla.callcenter.socket.event.AgentInStatusEvent;
import cn.com.nla.callcenter.socket.message.AgentInCallPhoneData;
import cn.com.nla.callcenter.socket.message.AgentInHangUpPhoneData;
import cn.com.nla.callcenter.socket.message.AgentInPhoneNotificationData;
import cn.com.nla.callcenter.socket.message.AgentInStatusData;
import cn.com.nla.callcenter.spi.AgentService;
import cn.com.nla.callcenter.spi.SendAgentMessageImpl;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.freeswitch.common.interfaces.ResultEvent;
import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.fs.AgentInfoManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.SsrcTransactionManager;
import cn.com.nla.common.freeswitch.redis.subscribe.notify.AgentLoginSubscribe;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.AgentSubscribeHandle;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 6.6 第二轮坐席 Socket.IO 契约测试。
 *
 * <p>不启动 Spring 容器，纯 Mockito 验证三个契约面：
 * 1) SendAgentMessageImpl 的房间推送（{@code namespace:agentKey} 房间键 + ObjectProvider 降级）；
 * 2) AgentNamespace 的 Sa-Token 握手鉴权、登录占位锁与延迟登录；
 * 3) 四个入站事件的回执协议（错误码与文案对齐旧工程）与消息数据类的字段/序列化契约。</p>
 *
 * @author TZY
 */
class AgentSocketContractTest {

    /**
     * RedisUtils 类加载即向 SpringUtils 索取 RedissonClient，先垫一个最小容器
     * （含 mock RedissonClient）再触发其初始化，后续 mockStatic 才能正常插裁。
     */
    @BeforeAll
    static void prepareStaticContainerForRedisUtils() {
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("redissonClient", mock(RedissonClient.class));
        context.refresh();
        new SpringUtil().setApplicationContext(context);
    }

    // ------------------------------------------------------------------
    // SendAgentMessageImpl：出站房间推送契约
    // ------------------------------------------------------------------

    @Test
    void sendMessagePushesResultToLegacyAgentRoom() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SocketIOServer> provider = mock(ObjectProvider.class);
        SocketIOServer server = mock(SocketIOServer.class);
        SocketIONamespace namespace = mock(SocketIONamespace.class);
        BroadcastOperations room = mock(BroadcastOperations.class);
        when(provider.getIfAvailable()).thenReturn(server);
        when(server.getNamespace(AgentCommon.SOCKET_AGENT)).thenReturn(namespace);
        when(namespace.getRoomOperations("/socket_agent:10001")).thenReturn(room);

        SendAgentMessageImpl impl = new SendAgentMessageImpl(provider);
        RestResultEvent payload = new RestResultEvent(RespCode.CODE_0.getValue(), "ok", null);
        impl.sendMessage(AgentCommon.SOCKET_AGENT, AgentCommon.AGENT_OUT_CALL_NOTIFICATION, "10001", payload);

        verify(server).getNamespace(AgentCommon.SOCKET_AGENT);
        verify(namespace).getRoomOperations("/socket_agent:10001");
        verify(room).sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION, payload);
    }

    @Test
    void sendMessageDegradesSilentlyWhenSocketIoServerAbsent() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SocketIOServer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        SendAgentMessageImpl impl = new SendAgentMessageImpl(provider);
        assertDoesNotThrow(() -> impl.sendMessage(AgentCommon.SOCKET_AGENT, AgentCommon.AGENT_OUT_LOGIN,
            "10001", new RestResultEvent(RespCode.CODE_0.getValue(), "ok")));
    }

    @Test
    void sendMessageSkipsWhenNamespaceNotRegistered() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SocketIOServer> provider = mock(ObjectProvider.class);
        SocketIOServer server = mock(SocketIOServer.class);
        when(provider.getIfAvailable()).thenReturn(server);
        when(server.getNamespace(AgentCommon.SOCKET_AGENT)).thenReturn(null);

        SendAgentMessageImpl impl = new SendAgentMessageImpl(provider);
        assertDoesNotThrow(() -> impl.sendMessage(AgentCommon.SOCKET_AGENT, AgentCommon.AGENT_OUT_LOGIN,
            "10001", new RestResultEvent(RespCode.CODE_0.getValue(), "ok")));
        verify(server).getNamespace(AgentCommon.SOCKET_AGENT);
    }

    // ------------------------------------------------------------------
    // AgentNamespace：命名空间、房间键与握手鉴权
    // ------------------------------------------------------------------

    @Test
    void namespaceNameAndRoomKeyKeepLegacyProtocol() {
        AgentNamespace namespace = new AgentNamespace();
        assertEquals("/socket_agent", namespace.getNamespaceName());
        assertEquals("/socket_agent:10001", namespace.getSocketAgentKey("10001"));
    }

    @Test
    void onConnectRejectsClientWithoutAuthorizationToken() {
        AgentNamespace namespace = new AgentNamespace();
        AgentService agentService = mock(AgentService.class);
        ReflectionTestUtils.setField(namespace, "agentService", agentService);
        SocketIOClient client = clientWithParams(Map.of());

        namespace.onConnect(client);

        verify(client, never()).joinRoom(anyString());
        verify(agentService, never()).findUserId(any());
    }

    @Test
    void onConnectRejectsUnparsableLoginId() {
        AgentNamespace namespace = new AgentNamespace();
        AgentService agentService = mock(AgentService.class);
        ReflectionTestUtils.setField(namespace, "agentService", agentService);
        SocketIOClient client = clientWithParams(Map.of("Authorization", List.of("bad-token")));

        try (var stp = mockStatic(StpUtil.class)) {
            // loginId 缺少 userType-userId 分隔结构，无法解析用户编号
            stp.when(() -> StpUtil.getLoginIdByToken("bad-token")).thenReturn("sys_user");
            namespace.onConnect(client);
        }

        verify(agentService, never()).findUserId(any());
        verify(client, never()).joinRoom(anyString());
    }

    @Test
    void onConnectAuthenticatesTokenAndLogsInWhenLockAcquired() {
        AgentNamespace namespace = new AgentNamespace();
        AgentService agentService = mock(AgentService.class);
        ReflectionTestUtils.setField(namespace, "agentService", agentService);
        SocketIOClient client = clientWithParams(Map.of("Authorization", List.of("Bearer tok-99")));

        Agent agent = new Agent();
        agent.setAgentKey("10001");
        when(agentService.findUserId(99L)).thenReturn(agent);
        AgentInfoManager infoManager = mock(AgentInfoManager.class);

        try (var stp = mockStatic(StpUtil.class);
             var redis = mockStatic(RedisService.class);
             var utils = mockStatic(RedisUtils.class)) {
            stp.when(() -> StpUtil.getLoginIdByToken("tok-99")).thenReturn("sys_user-99");
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            utils.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(true);

            namespace.onConnect(client);

            verify(client).joinRoom("/socket_agent:10001");
            verify(infoManager).putAgentKey(client.getSessionId().toString(), "10001");
            ArgumentCaptor<ResultEvent> loginCallback = ArgumentCaptor.forClass(ResultEvent.class);
            verify(agentService).login(eq("10001"), loginCallback.capture());

            // 登录结果回调：向坐席回发 AGENT:OUT:LOGIN 并释放占位锁
            loginCallback.getValue().result(new RestResultEvent(RespCode.CODE_0.getValue(), "登陆成功"));
            verify(client).sendEvent(eq(AgentCommon.AGENT_OUT_LOGIN), any(RestResultEvent.class));
            utils.verify(() -> RedisUtils.deleteObject(anyString()));
        }
    }

    @Test
    void onConnectDefersLoginWhenLockHeldByAnotherSession() {
        AgentNamespace namespace = new AgentNamespace();
        AgentService agentService = mock(AgentService.class);
        ReflectionTestUtils.setField(namespace, "agentService", agentService);
        SocketIOClient client = clientWithParams(Map.of("Authorization", List.of("tok-99")));

        Agent agent = new Agent();
        agent.setAgentKey("10001");
        when(agentService.findUserId(99L)).thenReturn(agent);
        AgentInfoManager infoManager = mock(AgentInfoManager.class);
        AgentLoginSubscribe loginSubscribe = mock(AgentLoginSubscribe.class);

        try (var stp = mockStatic(StpUtil.class);
             var redis = mockStatic(RedisService.class);
             var utils = mockStatic(RedisUtils.class)) {
            stp.when(() -> StpUtil.getLoginIdByToken("tok-99")).thenReturn("sys_user-99");
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            redis.when(RedisService::getAgentLoginSubscribe).thenReturn(loginSubscribe);
            // 占位锁被占用（false）时挂延迟登录
            utils.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(false);

            namespace.onConnect(client);

            verify(agentService, never()).login(anyString(), any());
            verify(loginSubscribe).addLoginSubscribe(eq("10001"), any());
        }
    }

    // ------------------------------------------------------------------
    // AgentInStatusEvent：坐席状态机回执契约
    // ------------------------------------------------------------------

    @Test
    void statusEventTogglesLoginToReadyAndReadyToBusyOther() {
        AgentInStatusEvent event = new AgentInStatusEvent(mock(AgentNamespace.class));
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);

        try (var redis = mockStatic(RedisService.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");

            // LOGIN -> READY
            AgentVoInfo login = agentVo(AgentStateEnum.LOGIN);
            when(infoManager.get("10001")).thenReturn(login);
            event.onData(client, AgentInStatusData.builder().updateStatus(1).build(), null);
            assertEquals(AgentStateEnum.READY, login.getAgentState());
            verify(infoManager).put(login);
            ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
            verify(client, times(1)).sendEvent(eq(AgentCommon.AGENT_OUT_STATUS), reply.capture());
            assertEquals(RespCode.CODE_0.getValue(), reply.getValue().getCode());
            assertEquals("客服状态更新成功", reply.getValue().getMessage());
            assertSame(login, reply.getValue().getData());

            // READY -> BUSY_OTHER
            AgentVoInfo ready = agentVo(AgentStateEnum.READY);
            when(infoManager.get("10001")).thenReturn(ready);
            event.onData(client, AgentInStatusData.builder().updateStatus(1).build(), null);
            assertEquals(AgentStateEnum.BUSY_OTHER, ready.getAgentState());
            verify(client, times(2)).sendEvent(eq(AgentCommon.AGENT_OUT_STATUS), any(RestResultEvent.class));
        }
    }

    @Test
    void statusEventReturnsInfoWhenUpdateFlagAbsent() {
        AgentInStatusEvent event = new AgentInStatusEvent(mock(AgentNamespace.class));
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);
        AgentVoInfo vo = agentVo(AgentStateEnum.READY);

        try (var redis = mockStatic(RedisService.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");
            when(infoManager.get("10001")).thenReturn(vo);

            event.onData(client, AgentInStatusData.builder().build(), null);

            ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
            verify(client).sendEvent(eq(AgentCommon.AGENT_OUT_STATUS), reply.capture());
            assertEquals(RespCode.CODE_0.getValue(), reply.getValue().getCode());
            assertEquals("客服信息获取成功", reply.getValue().getMessage());
            assertSame(vo, reply.getValue().getData());
            verify(infoManager, never()).put(any());
        }
    }

    @Test
    void statusEventRejectsUnloggedClient() {
        AgentInStatusEvent event = new AgentInStatusEvent(mock(AgentNamespace.class));
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);

        try (var redis = mockStatic(RedisService.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");
            when(infoManager.get("10001")).thenReturn(null);

            event.onData(client, AgentInStatusData.builder().updateStatus(1).build(), null);

            ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
            verify(client).sendEvent(eq(AgentCommon.AGENT_OUT_STATUS), reply.capture());
            assertEquals(RespCode.CODE_2.getValue(), reply.getValue().getCode());
            assertTrue(reply.getValue().getMessage().contains("未登录"));
        }
    }

    @Test
    void statusEventRejectsStateOutsideToggleWindow() {
        AgentInStatusEvent event = new AgentInStatusEvent(mock(AgentNamespace.class));
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);
        AgentVoInfo vo = agentVo(AgentStateEnum.TALKING);

        try (var redis = mockStatic(RedisService.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");
            when(infoManager.get("10001")).thenReturn(vo);

            event.onData(client, AgentInStatusData.builder().updateStatus(1).build(), null);

            ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
            verify(client).sendEvent(eq(AgentCommon.AGENT_OUT_STATUS), reply.capture());
            assertEquals(RespCode.CODE_2.getValue(), reply.getValue().getCode());
            assertEquals("客服当前无法更新状态", reply.getValue().getMessage());
            verify(infoManager, never()).put(any());
        }
    }

    // ------------------------------------------------------------------
    // AgentInCallPhoneEvent / AgentInHangUpPhoneEvent：未登录与空通话回执
    // ------------------------------------------------------------------

    @Test
    void callPhoneEventRejectsUnloggedClient() {
        AgentInCallPhoneEvent event = new AgentInCallPhoneEvent(mock(AgentNamespace.class));
        AgentService agentService = mock(AgentService.class);
        ReflectionTestUtils.setField(event, "agentService", agentService);
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);

        try (var redis = mockStatic(RedisService.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");
            when(infoManager.get("10001")).thenReturn(null);

            event.onData(client, AgentInCallPhoneData.builder().caller("10086").build(), null);

            ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
            verify(client).sendEvent(eq(AgentCommon.AGENT_OUT_CALL_PHONE), reply.capture());
            assertEquals(RespCode.CODE_2.getValue(), reply.getValue().getCode());
            assertTrue(reply.getValue().getMessage().contains("未登录"));
            verify(agentService, never()).callPhone(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void hangUpEventReportsNoActiveCall() throws Exception {
        AgentInHangUpPhoneEvent event = new AgentInHangUpPhoneEvent(mock(AgentNamespace.class));
        SipServer sipServer = mock(SipServer.class);
        SIPCommanderForPlatform sipCommander = mock(SIPCommanderForPlatform.class);
        ReflectionTestUtils.setField(event, "sipServer", sipServer);
        ReflectionTestUtils.setField(event, "sipCommanderForPlatform", sipCommander);
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);
        SsrcTransactionManager transactionManager = mock(SsrcTransactionManager.class);

        try (var redis = mockStatic(RedisService.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            redis.when(RedisService::getSsrcTransactionManager).thenReturn(transactionManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");
            when(infoManager.get("10001")).thenReturn(agentVo(AgentStateEnum.TALKING));
            // getParamAll 未打桩，默认返回 null：视为无活跃通话

            event.onData(client, AgentInHangUpPhoneData.builder().build(), null);

            ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
            verify(client).sendEvent(eq(AgentCommon.AGENT_OUT_HANG_UP_PHONE), reply.capture());
            assertEquals(RespCode.CODE_2.getValue(), reply.getValue().getCode());
            assertTrue(reply.getValue().getMessage().contains("无活跃通话"));
            verify(sipCommander, never()).streamByeCmd(any(), any(), any(), any(), any(),
                any(), any(), any());
        }
    }

    // ------------------------------------------------------------------
    // AgentInPhoneNotificationEvent：入参校验与挂断信号
    // ------------------------------------------------------------------

    @Test
    void notificationEventValidatesPayloadBeforeBusiness() {
        AgentInPhoneNotificationEvent event = new AgentInPhoneNotificationEvent(mock(AgentNamespace.class));
        SocketIOClient client = mock(SocketIOClient.class);

        // 依次缺 type、callId、onVideo，均应在进入业务前按旧 validateFast 文案回执
        event.onData(client, AgentInPhoneNotificationData.builder().build(), null);
        event.onData(client, AgentInPhoneNotificationData.builder().type(1).build(), null);
        event.onData(client, AgentInPhoneNotificationData.builder().type(1).callId("C1").build(), null);

        ArgumentCaptor<RestResultEvent> reply = ArgumentCaptor.forClass(RestResultEvent.class);
        verify(client, times(3)).sendEvent(eq(AgentCommon.AGENT_OUT_CALL_NOTIFICATION), reply.capture());
        assertEquals(List.of("未获取操作类型", "未获取CallId编号", "未获取接听方式"),
            reply.getAllValues().stream().map(RestResultEvent::getMessage).toList());
        assertEquals(RespCode.CODE_2.getValue(), reply.getAllValues().get(0).getCode());
    }

    @Test
    void notificationEventPublishesAbandonSignalOnReject() {
        AgentInPhoneNotificationEvent event = new AgentInPhoneNotificationEvent(mock(AgentNamespace.class));
        AgentService agentService = mock(AgentService.class);
        ReflectionTestUtils.setField(event, "agentService", agentService);
        SocketIOClient client = mock(SocketIOClient.class);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        AgentInfoManager infoManager = mock(AgentInfoManager.class);

        try (var redis = mockStatic(RedisService.class);
             var utils = mockStatic(RedisUtils.class)) {
            redis.when(RedisService::getAgentInfoManager).thenReturn(infoManager);
            when(infoManager.getAgentKey(anyString())).thenReturn("10001");
            when(infoManager.get("10001")).thenReturn(agentVo(AgentStateEnum.IN_CALL_RING));

            event.onData(client, AgentInPhoneNotificationData.builder()
                .type(1).callId("C1").onVideo(0).build(), null);

            String expectedChannel = AgentSubscribeHandle.VIDEO_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER + "C1";
            utils.verify(() -> RedisUtils.publish(eq(expectedChannel), any()));
            verify(agentService, never()).callPhone(any(), any(), any(), any(), any(), any(), any());
        }
    }

    // ------------------------------------------------------------------
    // 消息数据类：字段契约与 Java 序列化
    // ------------------------------------------------------------------

    @Test
    void messagePayloadsKeepLegacyFieldContract() throws Exception {
        AgentInCallPhoneData callPhone = AgentInCallPhoneData.builder()
            .caller("10086").type("CALL_AUDIO_PHONE").build();
        assertEquals("10086", callPhone.getCaller());
        assertEquals("CALL_AUDIO_PHONE", callPhone.getType());

        AgentInPhoneNotificationData notification = AgentInPhoneNotificationData.builder()
            .type(2).callId("C1").onVideo(0).code(0).message("m").build();
        assertEquals(2, notification.getType());
        assertEquals("C1", notification.getCallId());
        assertEquals(0, notification.getOnVideo());
        assertEquals(0, notification.getCode());
        assertEquals("m", notification.getMessage());

        AgentInStatusData status = AgentInStatusData.builder().updateStatus(1).build();
        assertEquals(1, status.getUpdateStatus());

        AgentInHangUpPhoneData hangUp = AgentInHangUpPhoneData.builder().agentKey("10001").build();
        assertEquals("10001", hangUp.getAgentKey());

        // Java 序列化契约：入站载荷需可序列化并保持相等（前端经 Socket.IO 传输）
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(notification);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            assertEquals(notification, in.readObject());
        }
    }

    // ------------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------------

    private SocketIOClient clientWithParams(Map<String, List<String>> urlParams) {
        SocketIOClient client = mock(SocketIOClient.class);
        HandshakeData handshake = mock(HandshakeData.class);
        when(client.getHandshakeData()).thenReturn(handshake);
        when(handshake.getUrlParams()).thenReturn(urlParams);
        when(client.getSessionId()).thenReturn(UUID.randomUUID());
        return client;
    }

    private AgentVoInfo agentVo(AgentStateEnum state) {
        return AgentVoInfo.builder().agentKey("10001").agentState(state).build();
    }
}
