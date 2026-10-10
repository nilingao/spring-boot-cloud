package cn.com.nla.web.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.web.config.properties.XcxProperties;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.*;
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

/**
 * 微信手机号获取协议与限长订阅器测试；不请求真实微信服务。
 *
 * @author TZY
 */
class WechatPhoneClientTest {

    private static GenericApplicationContext context;
    private static Object previousContext;
    private static Object previousFactory;
    private WechatPhoneClient client;
    private XcxProperties.App app;

    private static final String TOKEN_OK = "{\"access_token\":\"test-token\",\"expires_in\":7200}";
    private static final String PHONE_OK = "{\"errcode\":0,\"errmsg\":\"ok\",\"phone_info\":{\"phoneNumber\":\"+8613800001111\",\"purePhoneNumber\":\"13800001111\",\"countryCode\":\"86\",\"watermark\":{\"timestamp\":1630580242,\"appid\":\"wx0123456789abcdef\"}}}";

    @BeforeAll static void utilities() {
        previousContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        previousFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        context = new GenericApplicationContext();
        context.registerBean(SpringUtils.class, SpringUtils::new);
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder().build());
        context.refresh();
    }

    @AfterAll static void restore() {
        context.close();
        ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", previousContext);
        ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousFactory);
    }

    @BeforeEach void prepare() {
        client = spy(new WechatPhoneClient());
        app = new XcxProperties.App();
        app.setSecret("test-secret");
        doReturn(bytes(TOKEN_OK)).when(client).post(eq("/cgi-bin/stable_token"), anyMap());
        doReturn(bytes(PHONE_OK)).when(client).post(startsWith("/wxa/business/getuserphonenumber"), anyMap());
    }

    @Test void resolvesPurePhoneNumberFromValidReply() {
        String phone = client.resolvePhoneNumber("wx0123456789abcdef", app, "phone-code-123");
        assertEquals("13800001111", phone);
        verify(client).post("/cgi-bin/stable_token", Map.of(
            "grant_type", "client_credential", "appid", "wx0123456789abcdef",
            "secret", "test-secret", "force_refresh", false));
        verify(client).post("/wxa/business/getuserphonenumber?access_token=test-token",
            Map.of("code", "phone-code-123"));
    }

    @Test void tokenFailureDoesNotCallPhoneApi() {
        doReturn(bytes("{\"errcode\":40013,\"errmsg\":\"invalid appsecret\"}"))
            .when(client).post(eq("/cgi-bin/stable_token"), anyMap());
        var ex = assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
        assertEquals("手机号获取失败，请重新授权", ex.getMessage());
        assertNull(ex.getCause());
        verify(client, never()).post(startsWith("/wxa/business/getuserphonenumber"), anyMap());
    }

    @Test void phoneApiErrorThrowsSanitizedException() {
        doReturn(bytes("{\"errcode\":40001,\"errmsg\":\"invalid credential secret\"}"))
            .when(client).post(startsWith("/wxa/business/getuserphonenumber"), anyMap());
        var ex = assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
        assertFalse(ex.getMessage().contains("secret"));
        assertFalse(ex.getMessage().contains("credential"));
    }

    @Test void missingPhoneInfoFieldThrowsException() {
        doReturn(bytes("{\"errcode\":0,\"errmsg\":\"ok\"}"))
            .when(client).post(startsWith("/wxa/business/getuserphonenumber"), anyMap());
        assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
    }

    @Test void blankOrOversizedPhoneRejected() {
        doReturn(bytes("{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"\"}}"))
            .when(client).post(startsWith("/wxa/business/getuserphonenumber"), anyMap());
        assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
        doReturn(bytes("{\"errcode\":0,\"phone_info\":{\"purePhoneNumber\":\"" + "1".repeat(21) + "\"}}"))
            .when(client).post(startsWith("/wxa/business/getuserphonenumber"), anyMap());
        assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
    }

    @Test void networkExceptionDoesNotLeakSecret() {
        doThrow(new IllegalStateException("contains secret and token"))
            .when(client).post(anyString(), anyMap());
        var ex = assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
        assertFalse(ex.getMessage().contains("secret"));
        assertFalse(ex.getMessage().contains("token"));
        assertNull(ex.getCause());
    }

    @Test void invalidTokenJsonRejected() {
        doReturn(bytes("not-json"))
            .when(client).post(eq("/cgi-bin/stable_token"), anyMap());
        assertThrows(ServiceException.class,
            () -> client.resolvePhoneNumber("wx0123456789abcdef", app, "code"));
    }

    @Test void subscriberCancelsOversizedResponse() {
        var subscriber = new WechatPhoneClient.LimitedBodySubscriber();
        var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.allocate(64 * 1024 + 1)));
        verify(subscription).cancel();
        assertThrows(java.util.concurrent.CompletionException.class,
            () -> subscriber.getBody().toCompletableFuture().join());
    }

    @Test void subscriberAcceptsBoundedResponse() {
        var subscriber = new WechatPhoneClient.LimitedBodySubscriber();
        var subscription = mock(Flow.Subscription.class);
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2})));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{3})));
        subscriber.onComplete();
        assertArrayEquals(new byte[]{1, 2, 3}, subscriber.getBody().toCompletableFuture().join());
        verify(subscription, never()).cancel();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
