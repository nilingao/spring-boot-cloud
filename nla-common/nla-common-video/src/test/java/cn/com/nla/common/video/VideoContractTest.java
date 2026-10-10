package cn.com.nla.common.video;

import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.HookType;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.basic.vo.media.HookVo;
import cn.com.nla.common.video.basic.vo.sip.RecordInfo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.config.sip.SipConfig;
import cn.com.nla.common.video.core.demo.MediaHookVo;
import cn.com.nla.common.video.core.demo.VideoRestResult;
import cn.com.nla.common.video.core.media.client.MediaUtils;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.model.RestResultEvent;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.video.core.redis.impl.RegisterServerManager;
import cn.com.nla.common.video.core.redis.subscribe.media.MediaHookSubscribe;
import cn.com.nla.common.video.core.redis.subscribe.record.RecordEndSubscribeHandle;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.service.authentication.CurrentUserProvider;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HttpUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.redisson.api.*;
import org.redisson.api.listener.MessageListener;
import org.redisson.codec.TypedJsonJackson3Codec;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.util.SerializationUtils;

import javax.sip.RequestEvent;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Migration contracts verified without external SIP, ZLM or Redis endpoints.
 * @author TZY
 */
class VideoContractTest {
    private static final RedissonClient REDIS = mock(RedissonClient.class);
    private static StaticApplicationContext infrastructure;
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();

    @BeforeAll
    static void infrastructure() {
        infrastructure = new StaticApplicationContext();
        infrastructure.getBeanFactory().registerSingleton("redissonClient", REDIS);
        infrastructure.refresh();
        bindContext(infrastructure);
    }

    private static void bindContext(org.springframework.context.ConfigurableApplicationContext context) {
        SpringUtil spring = new SpringUtil();
        spring.postProcessBeanFactory(context.getBeanFactory());
        spring.setApplicationContext(context);
    }

    @BeforeEach
    void resetInfrastructure() {
        reset(REDIS);
        bindContext(infrastructure);
        when(REDIS.getTopic(anyString())).thenAnswer(call -> topic(call.getArgument(0)).mock);
    }

    @AfterAll
    static void closeInfrastructure() { infrastructure.close(); }

    private Topic topic(String name) { return topics.computeIfAbsent(name, Topic::new); }

    /** Exercises the actual RedisUtils topic delegation against an in-memory message bus. */
    private static class Topic {
        final RTopic mock = mock(RTopic.class);
        final Map<Integer, MessageListener<byte[]>> listeners = new ConcurrentHashMap<>();
        final AtomicInteger ids = new AtomicInteger();
        Topic(String name) {
            when(mock.addListener(eq(byte[].class), any())).thenAnswer(call -> {
                int id = ids.incrementAndGet();
                listeners.put(id, call.getArgument(1));
                return id;
            });
            doAnswer(call -> { listeners.remove(call.<Integer>getArgument(0)); return null; })
                .when(mock).removeListener(anyInt());
            when(mock.publish(any())).thenAnswer(call -> {
                byte[] payload = call.getArgument(0);
                new ArrayList<>(listeners.values()).forEach(listener -> listener.onMessage(name, payload));
                return (long) listeners.size();
            });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.com.nla.common.video")
    static class WideScan { }

    @Test
    void disabledByDefaultEvenWhenConsumerScansVideoPackage() {
        new ApplicationContextRunner().withUserConfiguration(WideScan.class)
            .withConfiguration(AutoConfigurations.of(SipConfig.class)).run(context -> {
                assertThat(context).hasNotFailed().doesNotHaveBean(SipServer.class)
                    .doesNotHaveBean(DynamicTask.class).doesNotHaveBean(DeferredResultHolder.class);
                verifyNoInteractions(REDIS);
            });
    }

    @Test
    void enabledConfigurationBindsPropertiesAndClosesEverySubscriptionAndScheduler() {
        AtomicReference<DynamicTask> tasks = new AtomicReference<>();
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SipConfig.class))
            .withBean(DeviceChannelVoService.class, () -> mock(DeviceChannelVoService.class))
            .withBean("sipServer", Object.class, Object::new)
            .withBean("dynamicTask", Object.class, Object::new)
            .withBean("sipCommander", Object.class, Object::new)
            .withBean("videoProperties", Object.class, Object::new)
            .withPropertyValues("video.enabled=true", "video.sip.id=34020000002000000001",
                "video.sip.domain=3402000000", "video.sip.port=5062", "video.sip.bind-ip=0.0.0.0",
                "video.sip.advertised-ip=192.0.2.10", "video.settings.play-timeout=9")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(SipServer.class);
                assertEquals("0.0.0.0", context.getBean(SipConfigProperties.class).getBindIp());
                assertEquals("192.0.2.10", context.getBean(SipServer.class).getAdvertisedIp(null));
                assertEquals(9, context.getBean(VideoProperties.class).getPlayTimeout());
                assertNull(context.getBean(CurrentUserProvider.class).getUserId());
                tasks.set(context.getBean(DynamicTask.class));
                assertEquals(4, topics.values().stream().mapToInt(topic -> topic.listeners.size()).sum());
            });
        assertTrue(tasks.get().getExecutor().isShutdown());
        assertEquals(0, topics.values().stream().mapToInt(topic -> topic.listeners.size()).sum());
    }

    @Test
    void currentUserProviderCanBeSuppliedByTheConsumer() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(SipConfig.class))
            .withBean(DeviceChannelVoService.class, () -> mock(DeviceChannelVoService.class))
            .withPropertyValues("video.enabled=true")
            .withBean(CurrentUserProvider.class, () -> () -> 42L)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(CurrentUserProvider.class);
                assertEquals(42L, context.getBean(CurrentUserProvider.class).getUserId());
            });
    }

    @Test
    void callbacksAreIsolatedByKeyAndIdAndUseOneReleasedTopicListener() {
        try (DynamicTask tasks = new DynamicTask(); DeferredResultHolder holder = new DeferredResultHolder(tasks)) {
            holder.subscribe();
            var first = new VideoRestResult<ProtocolResult<String>>();
            var second = new VideoRestResult<ProtocolResult<String>>();
            var otherKey = new VideoRestResult<ProtocolResult<String>>();
            holder.put("play:a", "one", first);
            holder.put("play:a", "two", second);
            holder.put("play:b", "one", otherKey);
            holder.invokeResult("play:a", "one", ProtocolResult.result(0, "成功", "流"));
            assertEquals("流", first.join().getData());
            assertFalse(second.isDone()); assertFalse(otherKey.isDone());
            holder.invokeAllResult("play:a", ProtocolResult.result(0, "all", "next"));
            assertEquals("next", second.join().getData());
            assertFalse(otherKey.isDone());
            assertFalse(holder.exist("play:a", null));
            assertTrue(holder.exist("play:b", "one"));
            assertEquals(1, topic(VideoConstant.VIDEO_DEFERRED_RESULT_HOLDER).listeners.size());
        }
        assertTrue(topic(VideoConstant.VIDEO_DEFERRED_RESULT_HOLDER).listeners.isEmpty());
    }

    @Test
    void timeoutCompletesAndRemovesThePendingRequest() throws Exception {
        try (DynamicTask tasks = new DynamicTask(); DeferredResultHolder holder = new DeferredResultHolder(tasks)) {
            var result = new VideoRestResult<>(20L, () -> "timeout");
            holder.put("play", "one", result);
            assertEquals("timeout", result.get(2, TimeUnit.SECONDS));
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(2))
                .until(() -> !holder.exist("play", "one"));
        }
    }

    @Test
    void completedResultNeverEvaluatesItsTimeoutSupplier() throws Exception {
        CountDownLatch barrier = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        var result = new VideoRestResult<>(20L, () -> { calls.incrementAndGet(); return "late"; });
        result.complete("early");
        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(barrier::countDown);
        assertTrue(barrier.await(2, TimeUnit.SECONDS));
        assertEquals("early", result.join()); assertEquals(0, calls.get());
    }

    @Test
    void timeoutSupplierFailureCompletesExceptionally() {
        var result = new VideoRestResult<>(10L, () -> { throw new IllegalStateException("fallback failure"); });
        assertThrows(ExecutionException.class, () -> result.get(2, TimeUnit.SECONDS));
    }

    @Test
    void hookFieldsFilterSharedEventsAndExpiryRefreshDoesNotChangeIdentity() {
        try (DynamicTask tasks = new DynamicTask(); MediaHookSubscribe hooks = new MediaHookSubscribe(tasks)) {
            HookKey wildcard = new HookKey(HookType.on_server_keepalive, Map.of());
            HookKey first = new HookKey(HookType.on_server_keepalive, Map.of("mediaServerId", "a"));
            HookKey second = new HookKey(HookType.on_server_keepalive, Map.of("mediaServerId", "b"));
            AtomicInteger all = new AtomicInteger(), a = new AtomicInteger(), b = new AtomicInteger();
            hooks.addSubscribe(wildcard, (server, event) -> all.incrementAndGet());
            hooks.addSubscribe(first, (server, event) -> a.incrementAndGet());
            hooks.addSubscribe(second, (server, event) -> b.incrementAndGet());
            first.updateExpires(new Date(System.currentTimeMillis() + 600000));
            HookVo event = new HookVo(); event.setMediaServerId("a");
            hooks.sendNotify(MediaHookVo.builder().type(HookType.on_server_keepalive).hookVo(event).build());
            assertEquals(1, all.get()); assertEquals(1, a.get()); assertEquals(0, b.get());
            hooks.removeSubscribe(first);
            assertNull(hooks.getHookKey(first));
            assertSame(wildcard, hooks.getHookKey(wildcard));
            hooks.sendNotify(MediaHookVo.builder().type(HookType.on_server_keepalive).hookVo(event).build());
            assertEquals(2, all.get()); assertEquals(1, a.get());
            hooks.removeSubscribe(wildcard);
            assertSame(second, hooks.getHookKey(second));
            hooks.removeSubscribe(second);
            assertEquals(0, topics.values().stream().mapToInt(topic -> topic.listeners.size()).sum());
        }
    }

    @Test
    void sipSubscriptionsAcceptResponseAndPrewrappedErrorEventsAndReleaseBothTopics() {
        try (DynamicTask tasks = new DynamicTask()) {
            SipSubscribeHandle sip = new SipSubscribeHandle(tasks);
            AtomicReference<EventResult> received = new AtomicReference<>();
            sip.addOkSubscribe("call", received::set);
            sip.addErrorSubscribe("call", received::set);
            RedisUtils.publish(SipSubscribeHandle.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER + "call",
                SerializationUtils.serialize(new EventResult(new RestResultEvent(2, "error", "call"))));
            assertEquals(2, received.get().getStatusCode());
            assertEquals(0, sip.getOkSubscribesSize()); assertEquals(0, sip.getErrorSubscribesSize());
            sip.addOkSubscribe("next", received::set);
            RedisUtils.publish(SipSubscribeHandle.VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER + "next",
                SerializationUtils.serialize(new RestResultEvent(0, "ok", "next")));
            assertEquals(0, received.get().getStatusCode());
            sip.close();
            assertEquals(0, topics.values().stream().mapToInt(topic -> topic.listeners.size()).sum());
        }
    }

    @Test
    void recordCompletionPublishesAcrossNodesAndReplacementReleasesOldListener() {
        try (DynamicTask tasks = new DynamicTask()) {
            RecordEndSubscribeHandle source = new RecordEndSubscribeHandle(tasks);
            RecordEndSubscribeHandle remote = new RecordEndSubscribeHandle(tasks);
            AtomicInteger old = new AtomicInteger(), current = new AtomicInteger();
            remote.addEndEventHandler("device", "channel", event -> old.incrementAndGet());
            remote.addEndEventHandler("device", "channel", event -> current.incrementAndGet());
            assertEquals(1, topic(VideoConstant.VIDEO_RECORD_END_SUBSCRIBE_MANAGER + "device:channel").listeners.size());
            source.handlerEvent(RecordInfo.builder().deviceId("device").channelId("channel").count(1).sumNum(1).build());
            assertEquals(0, old.get()); assertEquals(1, current.get());
            assertEquals(0, topics.values().stream().mapToInt(topic -> topic.listeners.size()).sum());
            source.close(); remote.close();
        }
    }

    @Test
    void cacheKeepsSecondsAndPersistentTtlAndAtomicCounterSemantics() {
        RBucket<Object> bucket = mock(RBucket.class);
        RAtomicLong counter = mock(RAtomicLong.class);
        when(REDIS.getBucket("key")).thenReturn(bucket);
        when(REDIS.getAtomicLong("counter")).thenReturn(counter);
        when(counter.addAndGet(3)).thenReturn(7L);
        when(bucket.remainTimeToLive()).thenReturn(1201L, -1L);
        VideoCache.set("key", "value", 12);
        VideoCache.set("key", "persistent", -1);
        assertEquals(2, VideoCache.ttlSeconds("key"));
        assertEquals(-1, VideoCache.ttlSeconds("key"));
        assertEquals(7, VideoCache.increment("counter", 3, 9));
        verify(bucket).set("value", Duration.ofSeconds(12));
        verify(bucket).set("persistent");
        verify(counter).expire(Duration.ofSeconds(9));
    }

    @Test
    void redissonJacksonCodecRoundTripsChineseProtocolDtoAndLocalDateTime() throws Exception {
        var codec = new TypedJsonJackson3Codec(DeviceVo.class);
        DeviceVo device = new DeviceVo();
        device.setDeviceId("34020000001320000001"); device.setName("视频设备");
        device.setKeepaliveTime(LocalDateTime.of(2026, 10, 10, 10, 20, 30));
        var buffer = codec.getValueEncoder().encode(device);
        try {
            DeviceVo decoded = (DeviceVo) codec.getValueDecoder().decode(buffer, null);
            assertEquals(device.getName(), decoded.getName());
            assertEquals(device.getKeepaliveTime(), decoded.getKeepaliveTime());
        } finally { buffer.release(); }
    }

    @Test
    void zlmGetPreservesPrefixQueryEncodingAndProtocolErrorCodes() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/video/index/api/test", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"code\":0,\"msg\":\"成功\",\"data\":{\"stream\":\"camera\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var response = exchange.getResponseBody()) { response.write(body); }
        });
        server.createContext("/video/index/api/error", exchange -> {
            exchange.sendResponseHeaders(503, -1); exchange.close();
        });
        server.start();
        try {
            MediaServerVo media = new MediaServerVo(); media.setIp("127.0.0.1");
            media.setHttpPort(server.getAddress().getPort()); media.setSslStatus(0);
            media.setVideoHttpPrefix("/video/"); media.setSecret("secret+&");
            var result = MediaUtils.request(media, "/index/api/test", Map.of("stream", "视频&stream"));
            assertEquals(0, result.getCode());
            var fields = HttpUtil.decodeParamMap(query.get(), StandardCharsets.UTF_8);
            assertEquals("secret+&", fields.get("secret")); assertEquals("视频&stream", fields.get("stream"));
            assertEquals(2, MediaUtils.request(media, "/index/api/error").getCode());
            assertEquals(2, MediaUtils.requestStreamChanged(media, "/index/api/error").getCode());
        } finally { server.stop(0); }
    }

    @Test
    void closingVideoDoesNotStopAnExistingSipFactoryStackOnTheSameIp() throws Exception {
        var legacyProperties = new java.util.Properties();
        legacyProperties.setProperty("javax.sip.STACK_NAME", "GB28181_SIP");
        legacyProperties.setProperty("javax.sip.IP_ADDRESS", "127.0.0.1");
        var legacy = javax.sip.SipFactory.getInstance().createSipStack(legacyProperties);
        RegisterServerManager registry = mock(RegisterServerManager.class);
        infrastructure.getBeanFactory().registerSingleton("videoRegistry", registry);
        int legacyPort;
        try (ServerSocket tcp = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            legacyPort = tcp.getLocalPort();
        }
        try {
            legacy.createSipProvider(legacy.createListeningPoint("127.0.0.1", legacyPort, "TCP"));
            legacy.createSipProvider(legacy.createListeningPoint("127.0.0.1", legacyPort, "UDP"));
            legacy.start();
            SipConfigProperties properties = new SipConfigProperties();
            properties.setId("34020000002000000002"); properties.setDomain("3402000000");
            properties.setBindIp("127.0.0.1");
            try (ServerSocket tcp = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                properties.setPort(tcp.getLocalPort());
            }
            try (SipServer video = new SipServer(properties, mock(SipSubscribeHandle.class), new VideoProperties())) {
                video.initSipServer(null, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
                assertNotSame(legacy, video.getUdpSipProvider().getSipStack());
            }
            assertThrows(BindException.class, () -> {
                try (DatagramSocket ignored = new DatagramSocket(new InetSocketAddress("127.0.0.1", legacyPort))) { }
            });
            assertThrows(BindException.class, () -> {
                try (ServerSocket ignored = new ServerSocket(legacyPort, 1, InetAddress.getByName("127.0.0.1"))) { }
            });
        } finally {
            legacy.stop();
            infrastructure.getDefaultListableBeanFactory().destroySingleton("videoRegistry");
        }
    }

    @Test
    void jdk21SipUdpReceivesARealLoopbackMessageAndReleasesTransportPorts() throws Exception {
        int port;
        try (ServerSocket tcp = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = tcp.getLocalPort();
        }
        SipConfigProperties properties = new SipConfigProperties();
        properties.setId("34020000002000000001"); properties.setDomain("3402000000");
        properties.setPort(port); properties.setBindIp("127.0.0.1"); properties.setAdvertisedIp("192.0.2.10");
        RegisterServerManager registry = mock(RegisterServerManager.class);
        infrastructure.getBeanFactory().registerSingleton("videoRegistry", registry);
        CountDownLatch received = new CountDownLatch(1);
        SipRequestEvent handler = new SipRequestEvent() {
            public String getMethod() { return "OPTIONS"; }
            public void process(RequestEvent event) { received.countDown(); }
        };
        try (SipServer sip = new SipServer(properties, mock(SipSubscribeHandle.class), new VideoProperties())) {
            var requests = new ConcurrentHashMap<String, SipRequestEvent>(); requests.put("OPTIONS", handler);
            sip.initSipServer(null, requests, new ConcurrentHashMap<>());
            assertNotNull(sip.getTcpSipProvider()); assertNotNull(sip.getUdpSipProvider());
            assertEquals("192.0.2.10:5060", sip.getAdvertisedAddress("127.0.0.1:5060"));
            try (DatagramSocket sender = new DatagramSocket()) {
                String message = "OPTIONS sip:34020000002000000001@127.0.0.1:" + port + " SIP/2.0\r\n" +
                    "Via: SIP/2.0/UDP 127.0.0.1:" + sender.getLocalPort() + ";branch=z9hG4bK-test\r\n" +
                    "From: <sip:device@127.0.0.1>;tag=test\r\nTo: <sip:server@127.0.0.1>\r\n" +
                    "Call-ID: loopback@127.0.0.1\r\nCSeq: 1 OPTIONS\r\nMax-Forwards: 70\r\nContent-Length: 0\r\n\r\n";
                byte[] bytes = message.getBytes(StandardCharsets.US_ASCII);
                sender.send(new DatagramPacket(bytes, bytes.length, InetAddress.getByName("127.0.0.1"), port));
                assertTrue(received.await(5, TimeUnit.SECONDS));
            }
        } finally { infrastructure.getDefaultListableBeanFactory().destroySingleton("videoRegistry"); }
        verify(registry).delSip(properties.getId());
        try (ServerSocket tcp = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
             DatagramSocket udp = new DatagramSocket(new InetSocketAddress("127.0.0.1", port))) {
            assertTrue(tcp.isBound()); assertTrue(udp.isBound());
        }
    }
}
