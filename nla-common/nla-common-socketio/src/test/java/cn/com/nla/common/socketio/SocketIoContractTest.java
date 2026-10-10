package cn.com.nla.common.socketio;

import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import cn.com.nla.common.socketio.core.config.*;
import cn.com.nla.common.socketio.core.properties.SocketIoProperties;
import cn.com.nla.common.socketio.core.store.*;
import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.protocol.*;
import com.corundumstudio.socketio.store.Store;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.*;
import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.client.codec.Codec;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SocketIoContractTest {
    private final ApplicationContextRunner contexts = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SocketIoAutoConfiguration.class));
    private ApplicationContextRunner enabled() {
        return contexts.withPropertyValues("socket-io.enabled=true", "socket-io.auto-start=false");
    }
    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.com.nla.common.socketio.core")
    static class WideScan { }

    @Test void disabledEvenWhenConsumerScansAndSpecifiesRedisStore() {
        contexts.withUserConfiguration(WideScan.class).withPropertyValues("socket-io.store=redisson")
            .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(SocketIOServer.class)
                .doesNotHaveBean(StoreFactory.class).doesNotHaveBean(SocketIoLifecycle.class));
    }
    @Test void bindsPropertiesAndRegistersStandaloneAndEventOnlyNamespaces() {
        TestNamespace solo = new TestNamespace("/solo");
        TestNamespace chat = new TestNamespace("/chat");
        enabled().withBean("solo", NamespaceListener.class, () -> solo)
            .withBean("echo", EventListener.class, () -> new Echo(chat))
            .withPropertyValues("socket-io.host=127.0.0.1", "socket-io.port=9012", "socket-io.work-count=2",
                "socket-io.context=/realtime", "socket-io.ping-interval=9000")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(SocketIOServer.class);
                SocketIOServer server = context.getBean(SocketIOServer.class);
                assertNotNull(server.getNamespace("/solo"));
                assertNotNull(server.getNamespace("/chat"));
                assertSame(server, solo.getSocketIOServer());
                assertSame(server, chat.getSocketIOServer());
                assertEquals(9012, server.getConfiguration().getPort());
                assertEquals("/realtime", server.getConfiguration().getContext());
                assertEquals(9000, server.getConfiguration().getPingInterval());
                assertFalse(context.getBean(SocketIoLifecycle.class).isRunning());
            });
    }
    @Test void duplicateEventsAndConflictingNamespacesFailStartup() {
        TestNamespace chat = new TestNamespace("/chat");
        enabled().withBean("first", EventListener.class, () -> new Echo(chat))
            .withBean("second", EventListener.class, () -> new Echo(chat))
            .run(context -> assertThat(context).hasFailed());
        enabled().withBean("first", NamespaceListener.class, () -> new TestNamespace("/chat"))
            .withBean("second", NamespaceListener.class, () -> new TestNamespace("/chat"))
            .run(context -> assertThat(context).hasFailed());
    }
    @Test void invalidConfigurationAndMissingRedisClientFail() {
        enabled().withPropertyValues("socket-io.port=0").run(context -> assertThat(context).hasFailed());
        enabled().withPropertyValues("socket-io.store=redisson").run(context -> assertThat(context).hasFailed());
    }
    @Test void memoryModeWorksWithoutOptionalRedissonDependency() {
        enabled().withClassLoader(new FilteredClassLoader("org.redisson"))
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(SocketIOServer.class));
    }
    @Test void redisModeUsesConsumerClientWithoutTakingOwnership() {
        RedissonClient client = mock(RedissonClient.class);
        enabled().withPropertyValues("socket-io.store=redisson", "socket-io.redis-prefix=isolated:app")
            .withBean(RedissonClient.class, () -> client).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(SocketIoRedissonStoreFactory.class);
                assertSame(context.getBean(StoreFactory.class), context.getBean(SocketIOServer.class).getConfiguration().getStoreFactory());
            });
        verify(client, never()).shutdown();
    }
    @Test void foreignDispatchRoutesThroughFactoryHubAndIgnoresMissingNamespaces() {
        RedissonClient client = mock(RedissonClient.class);
        Map<String, RTopic> topics = new HashMap<>();
        Map<String, MessageListener<?>> callbacks = new HashMap<>();
        when(client.getTopic(anyString(), any(Codec.class))).thenAnswer(call -> {
            String name = call.getArgument(0);
            return topics.computeIfAbsent(name, key -> {
                RTopic topic = mock(RTopic.class);
                when(topic.addListener(any(Class.class), any(MessageListener.class))).thenAnswer(registration -> {
                    callbacks.put(key, registration.getArgument(1)); return 1;
                });
                return topic;
            });
        });
        var hub = mock(com.corundumstudio.socketio.namespace.NamespacesHub.class);
        var namespace = mock(com.corundumstudio.socketio.namespace.Namespace.class);
        when(hub.get("/chat")).thenReturn(namespace);
        SocketIoRedissonStoreFactory factory = new SocketIoRedissonStoreFactory(client, "foreign:app");
        factory.init(hub, mock(com.corundumstudio.socketio.handler.AuthorizeHandler.class), null);
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        DispatchMessage routed = new DispatchMessage("room", packet, "/chat");
        var callback = (MessageListener<DispatchMessage>) callbacks.get("foreign:app:topic:dispatch");
        callback.onMessage("channel", routed);
        verify(namespace).dispatch("room", packet);
        callback.onMessage("channel", new DispatchMessage("room", packet, "/absent"));
        factory.shutdown();
        assertEquals(7, topics.size());
        topics.values().forEach(topic -> verify(topic).removeListener(1));
        verify(client, never()).shutdown();
    }
    @Test void consumerServerStoreAuthorizationAndCustomizerOverridesWork() {
        SocketIOServer server = mock(SocketIOServer.class);
        StoreFactory store = mock(StoreFactory.class);
        enabled().withBean(SocketIOServer.class, () -> server).withBean(StoreFactory.class, () -> store)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(SocketIOServer.class).hasSingleBean(StoreFactory.class);
                assertSame(server, context.getBean(SocketIOServer.class));
            });
        AuthorizationListener auth = data -> new AuthorizationResult(false);
        enabled().withBean(AuthorizationListener.class, () -> auth)
            .withBean(SocketIoConfigurationCustomizer.class, () -> config -> config.setOrigin("https://example.test"))
            .run(context -> {
                assertThat(context).hasNotFailed();
                var config = context.getBean(SocketIOServer.class).getConfiguration();
                assertSame(auth, config.getAuthorizationListener());
                assertEquals("https://example.test", config.getOrigin());
            });
    }
    @Test void lifecycleIsIdempotentAndCleansFailedStart() {
        SocketIOServer server = mock(SocketIOServer.class);
        SocketIoLifecycle lifecycle = new SocketIoLifecycle(server, true);
        lifecycle.start(); lifecycle.start(); lifecycle.stop(); lifecycle.stop();
        verify(server, times(1)).start(); verify(server, times(1)).stop();
        assertFalse(lifecycle.isRunning());
        SocketIOServer failure = mock(SocketIOServer.class);
        doThrow(new IllegalStateException("bind failed")).when(failure).start();
        assertThrows(IllegalStateException.class, () -> new SocketIoLifecycle(failure, true).start());
        verify(failure).stop();
    }
    @Test void redisUsesLiveMapsAndDeletesOnlyOwnedSession() {
        RedissonClient client = mock(RedissonClient.class);
        RMap<String, Object> map = mock(RMap.class);
        Map<String, Object> values = new HashMap<>();
        when(client.getMap(anyString(), any(Codec.class))).thenReturn((RMap) map);
        when(map.put(anyString(), any())).thenAnswer(call -> values.put(call.getArgument(0), call.getArgument(1)));
        when(map.get(any())).thenAnswer(call -> values.get(call.getArgument(0)));
        when(map.containsKey(any())).thenAnswer(call -> values.containsKey(call.getArgument(0)));
        when(map.remove(any())).thenAnswer(call -> values.remove(call.getArgument(0)));
        SocketIoRedissonStoreFactory factory = new SocketIoRedissonStoreFactory(client, "test:service");
        UUID id = UUID.randomUUID();
        Store first = factory.createStore(id), second = factory.createStore(id);
        first.set("user", "张三"); assertEquals("张三", second.get("user"));
        assertTrue(second.has("user")); second.del("user"); assertFalse(first.has("user"));
        var head = mock(com.corundumstudio.socketio.handler.ClientHead.class);
        when(head.getSessionId()).thenReturn(id); factory.onDisconnect(head);
        verify(map).delete();
        factory.shutdown(); verify(client, never()).shutdown();
        verify(client, atLeastOnce()).getMap(eq("test:service:session:" + id), any(Codec.class));
    }
    @Test void pubsubFiltersOwnNodeAndRemovesOnlyOwnedListenerIds() {
        RedissonClient client = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(client.getTopic(anyString(), any(Codec.class))).thenReturn(topic);
        ArgumentCaptor<MessageListener<ConnectMessage>> listener = ArgumentCaptor.forClass(MessageListener.class);
        when(topic.addListener(eq(ConnectMessage.class), any(MessageListener.class))).thenReturn(71);
        var pubsub = new SocketIoRedissonPubSubStore(client, "test:service", new SocketIoRedisCodec(), 12L);
        AtomicInteger count = new AtomicInteger();
        pubsub.subscribe(PubSubType.CONNECT, message -> count.incrementAndGet(), ConnectMessage.class);
        verify(topic).addListener(eq(ConnectMessage.class), listener.capture());
        ConnectMessage own = new ConnectMessage(UUID.randomUUID()); own.setNodeId(12L);
        listener.getValue().onMessage("channel", own); assertEquals(0, count.get());
        own.setNodeId(13L); listener.getValue().onMessage("channel", own); assertEquals(1, count.get());
        pubsub.publish(PubSubType.CONNECT, own); assertEquals(12L, own.getNodeId());
        pubsub.unsubscribe(PubSubType.LEAVE); pubsub.shutdown(); pubsub.shutdown();
        verify(topic).removeListener(71); verify(topic, never()).removeAllListeners();
        verify(client, never()).shutdown();
        verify(client, atLeastOnce()).getTopic(eq("test:service:topic:connect"), any(Codec.class));
        assertThrows(IllegalStateException.class, () -> pubsub.publish(PubSubType.CONNECT, own));
    }
    @Test void redisCodecRoundTripsDispatchJoinAndChineseDtoWithTimeAndBytes() throws Exception {
        SocketIoRedisCodec codec = new SocketIoRedisCodec();
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setSubType(PacketType.EVENT); packet.setName("通知"); packet.setNsp("/chat");
        var dto = new Payload(); dto.text = "你好"; dto.time = LocalDateTime.of(2026, 10, 10, 12, 0);
        packet.setData(new ArrayList<>(List.of(dto, new byte[]{1, 2, 3})));
        DispatchMessage dispatch = new DispatchMessage("room", packet, "/chat"); dispatch.setNodeId(11L);
        DispatchMessage decoded = (DispatchMessage) roundTrip(codec, dispatch);
        assertEquals("/chat", decoded.getNamespace()); assertEquals(11L, decoded.getNodeId());
        assertEquals("通知", decoded.getPacket().getName());
        List<?> data = decoded.getPacket().getData();
        assertEquals(dto.time, ((Payload) data.get(0)).time);
        assertEquals("你好", ((Payload) data.get(0)).text);
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) data.get(1));
        UUID id = UUID.randomUUID();
        var join = (JoinLeaveMessage) roundTrip(codec, new JoinLeaveMessage(id, "room", "/chat"));
        assertEquals(id, join.getSessionId());
        assertEquals(id, roundTrip(codec, id));
        assertEquals(dto.time, roundTrip(codec, dto.time));
        assertEquals(new FinalPayload("终值"), roundTrip(codec, new FinalPayload("终值")));
    }
    private static Object roundTrip(Codec codec, Object object) throws Exception {
        ByteBuf bytes = codec.getValueEncoder().encode(object);
        try { return codec.getValueDecoder().decode(bytes, null); } finally { bytes.release(); }
    }
    public static class Payload { public String text; public LocalDateTime time; }
    public record FinalPayload(String text) { }
    public static class TestNamespace implements NamespaceListener {
        private final String name;
        private SocketIOServer server;
        public TestNamespace(String name) { this.name = name; }
        public String getNamespaceName() { return name; }
        public SocketIOServer getSocketIOServer() { return server; }
        public void setSocketIOServer(SocketIOServer server) { this.server = server; }
        public void onConnect(SocketIOClient client) { client.joinRoom("room"); }
        public void onDisconnect(SocketIOClient client) { }
    }
    public static class Echo implements EventListener<Payload> {
        private final NamespaceListener namespace;
        public Echo(NamespaceListener namespace) { this.namespace = namespace; }
        public Class<Payload> getEventClass() { return Payload.class; }
        public String getEventName() { return "echo"; }
        public NamespaceListener getNamespace() { return namespace; }
        public void onData(SocketIOClient client, Payload data, AckRequest ack) { ack.sendAckData(data.text); }
    }
}
