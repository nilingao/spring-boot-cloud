package cn.com.nla.web.service;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.web.config.properties.XcxProperties;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 测试真实配置选择与响应处理，替换 JustAuth 外部请求边界。 */
class WechatMiniClientTest {
    private static final String APP = "wx0123456789abcdef";
    private static final String OTHER_APP = "wxfedcba9876543210";
    private XcxProperties properties;
    private XcxProperties.App app;
    private WechatMiniClient client;
    private AuthRequest request;
    private AuthResponse<AuthUser> response;
    private AuthUser authUser;
    private AuthToken token;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void prepareClient() {
        properties = new XcxProperties(); properties.setEnabled(true);
        app = new XcxProperties.App(); app.setSecret("test-only-secret"); app.setClientIds(List.of("web-client"));
        properties.getApps().put(APP, app);
        client = spy(new WechatMiniClient(properties));
        request = mock(AuthRequest.class);
        doReturn(request).when(client).createRequest(any(AuthConfig.class));
        response = mock(AuthResponse.class); authUser = mock(AuthUser.class); token = mock(AuthToken.class);
        when(response.ok()).thenReturn(true); when(response.getData()).thenReturn(authUser);
        when(authUser.getToken()).thenReturn(token); when(token.getOpenId()).thenReturn("verified-openid");
        when(request.login(any(AuthCallback.class))).thenReturn(response);
    }

    @Test
    void exchangesWithServerSecretAndAllowsMissingUnionId() {
        var identity = client.exchange(APP, "one-time-wx-code", "web-client");
        assertEquals(APP, identity.appid()); assertEquals("verified-openid", identity.openid());
        assertNull(identity.unionId());
        var config = ArgumentCaptor.forClass(AuthConfig.class);
        verify(client).createRequest(config.capture());
        assertEquals(APP, config.getValue().getClientId());
        assertEquals("test-only-secret", config.getValue().getClientSecret());
        var callback = ArgumentCaptor.forClass(AuthCallback.class);
        verify(request).login(callback.capture()); assertEquals("one-time-wx-code", callback.getValue().getCode());
        assertFalse(properties.toString().contains("test-only-secret"));
    }

    @Test
    void multipleAppsSelectOnlyMatchingSecretAndKeepSameOpenidIsolated() {
        var second = new XcxProperties.App(); second.setSecret("other-test-secret"); second.setClientIds(List.of("web-client"));
        properties.getApps().put(OTHER_APP, second);
        var identity = client.exchange(OTHER_APP, "wx-code", "web-client");
        var config = ArgumentCaptor.forClass(AuthConfig.class); verify(client).createRequest(config.capture());
        assertEquals(OTHER_APP, config.getValue().getClientId());
        assertEquals("other-test-secret", config.getValue().getClientSecret());
        assertEquals(OTHER_APP, identity.appid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "app-disabled", "unknown", "secret", "clients", "wrong-client", "null-apps"})
    void unavailableConfigurationNeverContactsProvider(String state) {
        String appid = APP; String clientId = "web-client";
        switch (state) {
            case "disabled" -> properties.setEnabled(false);
            case "app-disabled" -> app.setEnabled(false);
            case "unknown" -> appid = OTHER_APP;
            case "secret" -> app.setSecret(" ");
            case "clients" -> app.setClientIds(List.of());
            case "wrong-client" -> clientId = "other-client";
            case "null-apps" -> properties.setApps(null);
            default -> fail("Unknown scenario");
        }
        String selectedApp = appid; String selectedClient = clientId;
        assertThrows(ServiceException.class, () -> client.exchange(selectedApp, "wx-code", selectedClient));
        verifyNoInteractions(request);
        verify(client, never()).createRequest(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null-response", "failure", "no-user", "no-token", "exception"})
    void providerFailureDoesNotExposeCredentialsOrRawResponse(String state) {
        switch (state) {
            case "null-response" -> when(request.login(any(AuthCallback.class))).thenReturn(null);
            case "failure" -> { when(response.ok()).thenReturn(false); when(response.getMsg()).thenReturn("sensitive-provider-response"); }
            case "no-user" -> when(response.getData()).thenReturn(null);
            case "no-token" -> when(authUser.getToken()).thenReturn(null);
            case "exception" -> when(request.login(any(AuthCallback.class))).thenThrow(new IllegalStateException("test-only-secret"));
            default -> fail("Unknown scenario");
        }
        var failure = assertThrows(ServiceException.class, () -> client.exchange(APP, "wx-code", "web-client"));
        assertFalse(failure.getMessage().contains("sensitive-provider-response"));
        assertFalse(failure.getMessage().contains("test-only-secret"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "openid:another-app", "openid/invalid"})
    void invalidProviderIdentityIsRejected(String openid) {
        when(token.getOpenId()).thenReturn(openid);
        assertThrows(ServiceException.class, () -> client.exchange(APP, "wx-code", "web-client"));
    }

    @Test
    void unionIdIsOptionalMetadataAndNotAnAccountLookupKey() {
        when(token.getUnionId()).thenReturn("verified-union-id");
        assertEquals("verified-union-id", client.exchange(APP, "wx-code", "web-client").unionId());
    }

    @Test
    void bindsTypedConfigurationAndRemainsDisabledByDefault() {
        assertFalse(new XcxProperties().isEnabled());
        var source = new MapConfigurationPropertySource(Map.of(
            "xcx.enabled", "true", "xcx.apps[" + APP + "].secret", "test-secret",
            "xcx.apps[" + APP + "].client-ids[0]", "web-client"));
        var bound = new Binder(source).bind("xcx", Bindable.of(XcxProperties.class)).get();
        assertTrue(bound.isEnabled());
        assertEquals("test-secret", bound.getApps().get(APP).getSecret());
        assertEquals(List.of("web-client"), bound.getApps().get(APP).getClientIds());
    }
}
