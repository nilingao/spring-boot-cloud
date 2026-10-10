package cn.com.nla.web.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.web.config.properties.XcxProperties;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 真实供应商协议与限长订阅器；不请求真实微信服务。 */
class WechatQrClientTest {
    private static GenericApplicationContext context;
    private static Object previousContext;
    private static Object previousFactory;
    private WechatQrClient client;
    private XcxProperties.App app;
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};

    @BeforeAll static void utilities() {
        previousContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        previousFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        context = new GenericApplicationContext(); context.registerBean(SpringUtils.class, SpringUtils::new);
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build()); context.refresh();
    }
    @AfterAll static void restore() {
        context.close(); ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", previousContext);
        ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousFactory);
    }
    @BeforeEach void prepare() {
        client = spy(new WechatQrClient()); app = new XcxProperties.App();
        app.setSecret("private-test-secret"); app.setQrPage("pages/login/index");
        doReturn(bytes("{\"access_token\":\"server-token\",\"expires_in\":7200}"))
            .when(client).post(eq("/cgi-bin/stable_token"), anyMap());
        doReturn(PNG).when(client).post(eq("/wxa/getwxacodeunlimit?access_token=server-token"), anyMap());
    }

    @Test void generatesCodeForServerScenePageAndEnvironmentWithoutBrowserSecret() {
        String image = client.generate("wx0123456789abcdef", app, "0123456789abcdef0123456789abcdef");
        assertTrue(image.startsWith("data:image/png;base64,")); assertFalse(image.contains("server-token"));
        verify(client).post("/cgi-bin/stable_token", Map.of("appid", "wx0123456789abcdef", "secret", "private-test-secret",
            "grant_type", "client_credential", "force_refresh", false));
        verify(client).post("/wxa/getwxacodeunlimit?access_token=server-token", Map.of("scene", "0123456789abcdef0123456789abcdef",
            "page", "pages/login/index", "check_path", true, "env_version", "release", "width", 430));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"errcode\":40013,\"errmsg\":\"private-test-secret\"}",
        "{\"access_token\":\"token&other=unsafe\"}", "{\"access_token\":123}", "not-json",
        "{\"access_token\":\"server-token\",\"errcode\":40013}"})
    void invalidTokenReplyDoesNotRequestImageOrExposeRawProviderError(String reply) {
        doReturn(bytes(reply)).when(client).post(eq("/cgi-bin/stable_token"), anyMap());
        var failure = assertThrows(ServiceException.class, () -> client.generate("wx0123456789abcdef", app, "scene"));
        assertFalse(failure.getMessage().contains("private-test-secret")); assertNull(failure.getCause());
        verify(client, never()).post(startsWith("/wxa/"), anyMap());
    }

    @Test void imageJsonFailureAndNetworkExceptionAreSanitized() {
        doReturn(bytes("{\"errcode\":45009,\"errmsg\":\"private-test-secret\"}"))
            .when(client).post(startsWith("/wxa/"), anyMap());
        assertThrows(ServiceException.class, () -> client.generate("wx0123456789abcdef", app, "scene"));
        doThrow(new IllegalStateException("private-test-secret server-token")).when(client).post(anyString(), anyMap());
        var error = assertThrows(ServiceException.class, () -> client.generate("wx0123456789abcdef", app, "scene"));
        assertFalse(error.getMessage().contains("server-token")); assertNull(error.getCause());
    }

    @Test void supportsJpegAndRejectsEmptyImage() {
        doReturn(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0}).when(client).post(startsWith("/wxa/"), anyMap());
        assertTrue(client.generate("wx0123456789abcdef", app, "scene").startsWith("data:image/jpeg;base64,"));
        doReturn(new byte[0]).when(client).post(startsWith("/wxa/"), anyMap());
        assertThrows(ServiceException.class, () -> client.generate("wx0123456789abcdef", app, "scene"));
    }

    @Test void subscriberCancelsOversizedResponseBeforeBuffering() {
        var subscriber = new WechatQrClient.LimitedBodySubscriber(); var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.allocate(1024 * 1024), ByteBuffer.allocate(1)));
        verify(subscription).cancel();
        assertThrows(java.util.concurrent.CompletionException.class, () -> subscriber.getBody().toCompletableFuture().join());
    }

    @Test void subscriberAcceptsBoundedResponseAcrossChunks() {
        var subscriber = new WechatQrClient.LimitedBodySubscriber(); var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription); subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2})));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{3}))); subscriber.onComplete();
        assertArrayEquals(new byte[]{1, 2, 3}, subscriber.getBody().toCompletableFuture().join());
        verify(subscription, never()).cancel();
    }

    @Test void qrConfigurationBindsIndependentlyFromMiniLoginAndDefaultsClosed() {
        assertFalse(new XcxProperties().isQrEnabled());
        var source = new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(Map.of(
            "xcx.enabled", "true", "xcx.qr-enabled", "true", "xcx.apps[wx0123456789abcdef].qr-client-ids[0]", "web-client",
            "xcx.apps[wx0123456789abcdef].client-ids[0]", "mini-client",
            "xcx.apps[wx0123456789abcdef].qr-page", "pages/login/index"));
        var bound = new org.springframework.boot.context.properties.bind.Binder(source)
            .bind("xcx", org.springframework.boot.context.properties.bind.Bindable.of(XcxProperties.class)).get();
        assertTrue(bound.isQrEnabled()); var config = bound.getApps().get("wx0123456789abcdef");
        assertEquals(List.of("web-client"), config.getQrClientIds()); assertEquals(List.of("mini-client"), config.getClientIds());
        assertEquals("pages/login/index", config.getQrPage()); assertEquals("release", config.getQrEnvVersion());
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
