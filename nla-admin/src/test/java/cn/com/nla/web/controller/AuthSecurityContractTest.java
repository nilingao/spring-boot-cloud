package cn.com.nla.web.controller;

import cn.com.nla.web.service.WechatMiniClient;
import cn.com.nla.system.service.SysXcxBindingService;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.satoken.core.service.SaPermissionImpl;
import cn.com.nla.common.satoken.handler.SaTokenExceptionHandler;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.security.config.SecurityConfig;
import cn.com.nla.common.security.config.properties.SecurityProperties;
import cn.com.nla.common.security.handler.AllUrlHandler;
import cn.com.nla.common.social.config.properties.SocialProperties;
import cn.com.nla.system.api.MessageService;
import cn.com.nla.system.api.model.LoginUser;
import cn.com.nla.system.domain.vo.SysClientVo;
import cn.com.nla.system.service.ISysClientService;
import cn.com.nla.system.service.ISysConfigService;
import cn.com.nla.system.service.ISysSocialService;
import cn.com.nla.web.domain.vo.LoginVo;
import cn.com.nla.web.service.IAuthStrategy;
import cn.com.nla.web.service.SysLoginService;
import cn.com.nla.web.service.SysRegisterService;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.fun.strategy.SaCreateSaRequestFunction;
import cn.dev33.satoken.fun.strategy.SaCreateSaResponseFunction;
import cn.dev33.satoken.fun.strategy.SaCreateSaStorageFunction;
import cn.dev33.satoken.fun.strategy.SaRouteMatchFunction;
import cn.dev33.satoken.json.SaJsonTemplate;
import cn.dev33.satoken.json.SaJsonTemplateForJackson3;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.spring.SaTokenContextRegister;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 验证真实 MVC 拦截器、JWT 会话和认证控制器；外部用户/客户端服务采用测试替身。 */
class AuthSecurityContractTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private SysClientVo client;
    private ISysClientService clients;
    private IAuthStrategy strategy;
    private ScheduledExecutorService scheduler;
    private SaTokenConfig previousConfig;
    private SaTokenDao previousDao;
    private SaTokenContext previousContext;
    private StpInterface previousPermission;
    private SaJsonTemplate previousJson;
    private StpLogic previousLogic;
    private SaRouteMatchFunction previousRouteMatcher;
    private SaCreateSaRequestFunction previousRequestFactory;
    private SaCreateSaResponseFunction previousResponseFactory;
    private SaCreateSaStorageFunction previousStorageFactory;
    private SaTokenDaoDefaultImpl memoryDao;

    @BeforeEach
    void prepareMvcAndIsolatedTokenStore() {
        previousConfig = SaManager.getConfig(); previousDao = SaManager.getSaTokenDao();
        previousContext = SaManager.getSaTokenContext(); previousPermission = SaManager.getStpInterface();
        previousJson = SaManager.getSaJsonTemplate(); previousLogic = StpUtil.getStpLogic();
        previousRouteMatcher = SaStrategy.instance.routeMatcher;
        previousRequestFactory = SaStrategy.instance.createSaRequest;
        previousResponseFactory = SaStrategy.instance.createSaResponse;
        previousStorageFactory = SaStrategy.instance.createSaStorage;
        new SaTokenContextRegister();
        SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
            .setJwtSecretKey("contract-test-key-with-no-production-use").setIsReadCookie(false)
            .setIsShare(false).setIsConcurrent(true).setDynamicActiveTimeout(true).setIsPrint(false).setIsLog(false));
        memoryDao = new SaTokenDaoDefaultImpl(); SaManager.setSaTokenDao(memoryDao);
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        SaManager.setSaJsonTemplate(new SaJsonTemplateForJackson3());
        SaManager.setStpInterface(new SaPermissionImpl()); StpUtil.setStpLogic(new StpLogicJwtForSimple());
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(MvcConfig.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        clients = context.getBean(ISysClientService.class); strategy = context.getBean(IAuthStrategy.class);
        scheduler = context.getBean(ScheduledExecutorService.class);
        client = new SysClientVo(); client.setClientId("web-client"); client.setClientKey("web");
        client.setDeviceType("pc"); client.setStatus("0"); client.setGrantType("password,email");
        client.setTimeout(1800L); client.setActiveTimeout(600L);
        when(clients.queryByClientId("web-client")).thenReturn(client);
        when(strategy.login(anyString(), same(client))).thenAnswer(invocation -> {
            String token = issueToken(IAuthStrategy.buildLoginParameter(client), Set.of("contract:read"), Set.of("operator"));
            LoginVo view = new LoginVo(); view.setAccessToken(token); view.setClientId(client.getClientId());
            view.setExpireIn(StpUtil.getTokenTimeout()); return view;
        });
    }

    @AfterEach
    void restoreGlobalTokenState() {
        RequestContextHolder.resetRequestAttributes();
        if (context != null) { context.close(); }
        StpUtil.setStpLogic(previousLogic); SaManager.setConfig(previousConfig); SaManager.setSaTokenDao(previousDao);
        SaManager.setSaTokenContext(previousContext); SaManager.setStpInterface(previousPermission);
        SaManager.setSaJsonTemplate(previousJson); memoryDao.destroy();
        SaStrategy.instance.routeMatcher = previousRouteMatcher;
        SaStrategy.instance.createSaRequest = previousRequestFactory;
        SaStrategy.instance.createSaResponse = previousResponseFactory;
        SaStrategy.instance.createSaStorage = previousStorageFactory;
    }

    @Test
    void publicLoginDispatchesAuthorizedStrategyAndReturnedJwtAccessesProtectedMvc() throws Exception {
        var result = mvc.perform(post("/auth/login").contentType("application/json").content(loginBody("password")))
            .andReturn();
        var json = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
        assertEquals(200, json.get("code").asInt());
        assertEquals("web-client", json.get("data").get("client_id").asString());
        assertTrue(json.get("data").get("expire_in").asLong() > 0);
        String token = json.get("data").get("access_token").asString();
        assertEquals(200, responseCode(authenticated(get("/contract/read"), token)));
        verify(strategy).login(loginBody("password"), client);
        verify(scheduler).schedule(any(Runnable.class), eq(5L), eq(TimeUnit.SECONDS));
    }

    @ParameterizedTest
    @ValueSource(strings = {"word", "pass", "PASSWORD", "password,email", "emailAuthStrategy"})
    void unauthorizedGrantNamesNeverDispatch(String grant) throws Exception {
        assertEquals(500, responseCode(post("/auth/login").contentType("application/json").content(loginBody(grant))));
        verifyNoInteractions(strategy, scheduler);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "word", "passwords,email"})
    void unconfiguredOrPartialAllowedGrantNeverDispatches(String configuredGrants) throws Exception {
        client.setGrantType(configuredGrants);
        assertEquals(500, responseCode(post("/auth/login").contentType("application/json").content(loginBody("password"))));
        verifyNoInteractions(strategy, scheduler);
    }

    @Test
    void configuredGrantWhitespaceIsTrimmed() throws Exception {
        client.setGrantType(" email, password , ");
        assertEquals(200, responseCode(post("/auth/login").contentType("application/json").content(loginBody("password"))));
        verify(strategy).login(loginBody("password"), client);
    }

    @Test
    void unknownAndDisabledClientsNeverDispatch() throws Exception {
        when(clients.queryByClientId("web-client")).thenReturn(null);
        assertEquals(500, responseCode(post("/auth/login").contentType("application/json").content(loginBody("password"))));
        when(clients.queryByClientId("web-client")).thenReturn(client); client.setStatus("1");
        assertEquals(500, responseCode(post("/auth/login").contentType("application/json").content(loginBody("password"))));
        verifyNoInteractions(strategy, scheduler);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/contract/plain", "/contract/read", "/contract/role", "/files/one"})
    void registeredProtectedRoutesRequireLogin(String path) throws Exception {
        assertEquals(401, responseCode(get(path)));
    }

    @Test
    void methodIgnoreAndConfiguredExcludeRemainPublic() throws Exception {
        assertEquals(200, responseCode(get("/public")));
        assertEquals(200, responseCode(get("/assets/open")));
        assertEquals(200, responseCode(post("/auth/logout")));
        assertEquals(500, responseCode(post("/auth/register").contentType("application/json")
            .content("{\"clientId\":\"web-client\",\"grantType\":\"password\",\"username\":\"contract-user\",\"password\":\"test-only-password\"}")));
    }

    static Stream<Arguments> clientIds() {
        return Stream.of(
            Arguments.of(null, null, 401),
            Arguments.of("web-client", null, 200),
            Arguments.of(null, "web-client", 200),
            Arguments.of("web-client", "web-client", 200),
            Arguments.of("other", null, 401),
            Arguments.of("web-client", "other", 401),
            Arguments.of("other", "web-client", 401),
            Arguments.of("web-client", "", 401),
            Arguments.of("", "web-client", 401));
    }

    @ParameterizedTest
    @MethodSource("clientIds")
    void everyProvidedClientIdMustMatchToken(String header, String parameter, int expected) throws Exception {
        String token = token(Set.of());
        var request = get("/contract/plain").header("Authorization", "Bearer " + token);
        if (header != null) { request.header("clientid", header); }
        if (parameter != null) { request.param("clientid", parameter); }
        assertEquals(expected, responseCode(request));
    }

    @Test
    void repeatedClientValuesCannotHideConflicts() throws Exception {
        String token = token(Set.of());
        assertEquals(401, responseCode(authenticated(get("/contract/plain"), token).param("clientid", "web-client", "other")));
        assertEquals(401, responseCode(authenticated(get("/contract/plain"), token).header("clientid", "other")));
        assertEquals(200, responseCode(authenticated(get("/contract/plain"), token).param("clientid", "web-client", "web-client")));
    }

    @Test
    void missingClientTokenExtraIs401InsteadOfNullPointerError() throws Exception {
        String token = issueInBoundRequest(new SaLoginParameter(), Set.of(), Set.of());
        assertEquals(401, responseCode(authenticated(get("/contract/plain"), token)));
    }

    @Test
    void permissionsAndRolesComeFromActualLoginSession() throws Exception {
        String denied = token(Set.of());
        assertEquals(403, responseCode(authenticated(get("/contract/read"), denied)));
        assertEquals(403, responseCode(authenticated(get("/contract/role"), denied)));
        String allowed = issueInBoundRequest(IAuthStrategy.buildLoginParameter(client), Set.of("contract:read"), Set.of("operator"));
        assertEquals(200, responseCode(authenticated(get("/contract/read"), allowed)));
        assertEquals(200, responseCode(authenticated(get("/contract/role"), allowed)));
    }

    @Test
    void cookieAloneCannotAuthenticateAndMalformedJwtIsRejected() throws Exception {
        String token = token(Set.of());
        assertEquals(401, responseCode(get("/contract/plain").cookie(new Cookie("Authorization", token)).header("clientid", "web-client")));
        assertEquals(401, responseCode(authenticated(get("/contract/plain"), "invalid.jwt.token")));
    }

    @Test
    void revokedJwtCannotAccessProtectedMvc() throws Exception {
        String token = token(Set.of());
        StpUtil.logoutByTokenValue(token);
        assertEquals(401, responseCode(authenticated(get("/contract/plain"), token)));
    }

    @Test
    void jwtKeepsSnowflakeIdentityDepartmentAndClientTimeouts() throws Exception {
        String token = token(Set.of("contract:read"));
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        try {
            assertEquals(9_007_199_254_740_993L, LoginHelper.getUserId());
            assertEquals(9_007_199_254_740_999L, LoginHelper.getDeptId());
            assertEquals("contract-user", LoginHelper.getUsername());
            assertEquals("sys_user-9007199254740993", StpUtil.getLoginIdAsString());
            assertEquals("web-client", StpUtil.getExtra(LoginHelper.CLIENT_KEY));
            assertEquals("pc", LoginHelper.getLoginUser().getDeviceType());
            assertTrue(StpUtil.getTokenTimeout() > 0 && StpUtil.getTokenTimeout() <= 1800);
            assertTrue(StpUtil.getTokenActiveTimeout() > 0 && StpUtil.getTokenActiveTimeout() <= 600);
        } finally { RequestContextHolder.resetRequestAttributes(); }
    }

    @Test
    void clientPathAndIpRestrictionsReachRealInterceptor() throws Exception {
        client.setAccessPath("/contract/**"); client.setIpWhitelist("10.0.0.0/24;127.0.0.1");
        String token = token(Set.of());
        assertEquals(200, responseCode(authenticated(get("/contract/plain"), token).with(request -> { request.setRemoteAddr("10.0.0.8"); return request; })));
        assertEquals(403, responseCode(authenticated(get("/contract/plain"), token).with(request -> { request.setRemoteAddr("192.168.1.8"); return request; })));
        assertEquals(403, responseCode(authenticated(get("/files/one"), token)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/unlock/9", "/auth/social/callback"})
    void socialMutationRoutesRequireFullClientPolicy(String path) throws Exception {
        var request = path.contains("unlock") ? delete(path) : post(path).contentType("application/json").content("{}");
        assertEquals(401, responseCode(request));
        var mismatch = path.contains("unlock") ? delete(path) : post(path).contentType("application/json").content("{}");
        assertEquals(401, responseCode(mismatch.header("Authorization", "Bearer " + token(Set.of())).header("clientid", "other")));
        verifyNoInteractions(context.getBean(ISysSocialService.class));
    }

    @Test
    void socialUnlinkPassesSessionOwnerAndDoesNotUseUnscopedDelete() throws Exception {
        var social = context.getBean(ISysSocialService.class);
        when(social.deleteByIdAndUserId(9L, 9_007_199_254_740_993L)).thenReturn(true);
        String token = token(Set.of());
        assertEquals(200, responseCode(authenticated(delete("/auth/unlock/9"), token)));
        verify(social).deleteByIdAndUserId(9L, 9_007_199_254_740_993L);
        verify(social, never()).deleteWithValidById(anyLong());
    }

    @Test
    void xcxLoginUsesExistingAuthorizedGrantDispatch() throws Exception {
        client.setGrantType("password,xcx");
        assertEquals(200, responseCode(post("/auth/login").contentType("application/json").content(loginBody("xcx"))));
        verify(strategy).login(loginBody("xcx"), client);
    }

    @Test
    void xcxBindingRequiresLoginAndMatchingClientPolicy() throws Exception {
        var request = post("/auth/xcx/bind").contentType("application/json")
            .content("{\"appid\":\"wx0123456789abcdef\",\"xcxCode\":\"wx-code\"}");
        assertEquals(401, responseCode(request));
        assertEquals(401, responseCode(request.header("Authorization", "Bearer " + token(Set.of()))
            .header("clientid", "other-client")));
        verifyNoInteractions(context.getBean(WechatMiniClient.class), context.getBean(SysXcxBindingService.class));
    }

    @Test
    void xcxBindingRetainsClientPathAndIpRestrictions() throws Exception {
        client.setAccessPath("/contract/**");
        assertEquals(403, responseCode(authenticated(post("/auth/xcx/bind"), token(Set.of()))
            .contentType("application/json").content("{\"appid\":\"wx0123456789abcdef\",\"xcxCode\":\"wx-code\"}")));
        client.setAccessPath(null); client.setIpWhitelist("10.0.0.0/24");
        assertEquals(403, responseCode(authenticated(post("/auth/xcx/bind"), token(Set.of()))
            .contentType("application/json").content("{\"appid\":\"wx0123456789abcdef\",\"xcxCode\":\"wx-code\"}")
            .with(request -> { request.setRemoteAddr("192.168.1.1"); return request; })));
        verifyNoInteractions(context.getBean(WechatMiniClient.class));
    }

    @Test
    void xcxBindingUsesSessionOwnerAndVerifiedProviderIdentity() throws Exception {
        var provider = context.getBean(WechatMiniClient.class);
        var bindings = context.getBean(SysXcxBindingService.class);
        when(provider.exchange("wx0123456789abcdef", "wx-code", "web-client"))
            .thenReturn(new WechatMiniClient.Identity("wx0123456789abcdef", "verified-openid", null));
        assertEquals(200, responseCode(authenticated(post("/auth/xcx/bind"), token(Set.of()))
            .contentType("application/json").content("{\"appid\":\"wx0123456789abcdef\",\"xcxCode\":\"wx-code\",\"userId\":1,\"openid\":\"spoofed\"}")));
        verify(bindings).bind(9_007_199_254_740_993L, "wx0123456789abcdef", "verified-openid", null);
    }

    @Test
    void qrPublicCreationAndPossessionPollingAreMethodScopedAndNeverReturnJwt() throws Exception {
        var qr = context.getBean(cn.com.nla.web.service.QrLoginService.class);
        String scene = "0123456789abcdef0123456789abcdef";
        String secret = "a".repeat(43);
        when(qr.create("wx0123456789abcdef", "web-client"))
            .thenReturn(new cn.com.nla.web.domain.model.QrLoginModels.Created(scene, secret, "data:image/png;base64,test", 180));
        var created = mvc.perform(post("/auth/qr/create").contentType("application/json")
            .content("{\"appid\":\"wx0123456789abcdef\",\"clientId\":\"web-client\"}")).andReturn().getResponse();
        assertEquals("no-store", created.getHeader("Cache-Control"));
        var json = JsonMapper.builder().build().readTree(created.getContentAsString());
        assertEquals(200, json.get("code").asInt()); assertEquals(secret, json.get("data").get("browserToken").asString());
        assertFalse(created.getContentAsString().contains("access_token"));
        when(qr.status(scene, secret, "web-client"))
            .thenReturn(new cn.com.nla.web.domain.model.QrLoginModels.Status("CONFIRMED"));
        var status = mvc.perform(post("/auth/qr/status").contentType("application/json")
            .content("{\"scene\":\"" + scene + "\",\"browserToken\":\"" + secret + "\",\"clientId\":\"web-client\"}"))
            .andReturn().getResponse();
        assertEquals("no-store", status.getHeader("Cache-Control"));
        assertEquals("CONFIRMED", JsonMapper.builder().build().readTree(status.getContentAsString()).get("data").get("status").asString());
        assertFalse(status.getContentAsString().contains(secret)); assertFalse(status.getContentAsString().contains("access_token"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/qr/scan", "/auth/qr/confirm"})
    void qrScannerRoutesRequireLoginMatchingClientAndPathAndIpPolicy(String path) throws Exception {
        String body = "{\"scene\":\"0123456789abcdef0123456789abcdef\",\"confirmed\":true}";
        assertEquals(401, responseCode(post(path).contentType("application/json").content(body)));
        assertEquals(401, responseCode(post(path).contentType("application/json").content(body)
            .header("Authorization", "Bearer " + token(Set.of())).header("clientid", "other")));
        client.setAccessPath("/contract/**");
        assertEquals(403, responseCode(authenticated(post(path), token(Set.of())).contentType("application/json").content(body)));
        client.setAccessPath(null); client.setIpWhitelist("10.0.0.0/24");
        assertEquals(403, responseCode(authenticated(post(path), token(Set.of())).contentType("application/json").content(body)
            .with(request -> { request.setRemoteAddr("192.168.1.1"); return request; })));
        verifyNoInteractions(context.getBean(cn.com.nla.web.service.QrLoginService.class));
    }

    @Test
    void qrConfirmationRequiresExplicitDecisionAndRejectsMalformedScene() throws Exception {
        String token = token(Set.of());
        assertEquals(400, mvc.perform(authenticated(post("/auth/qr/confirm"), token).contentType("application/json")
            .content("{\"scene\":\"0123456789abcdef0123456789abcdef\"}")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(authenticated(post("/auth/qr/scan"), token).contentType("application/json")
            .content("{\"scene\":\"invalid\"}")).andReturn().getResponse().getStatus());
        assertEquals(400, mvc.perform(post("/auth/qr/status").contentType("application/json")
            .content("{\"scene\":\"0123456789abcdef0123456789abcdef\",\"clientId\":\"web-client\"}"))
            .andReturn().getResponse().getStatus());
        verifyNoInteractions(context.getBean(cn.com.nla.web.service.QrLoginService.class));
    }

    @Test
    void qrGrantUsesExistingLoginDispatchAndCannotMatchPartialGrant() throws Exception {
        client.setGrantType("password,qrcode");
        assertEquals(500, responseCode(post("/auth/login").contentType("application/json").content(loginBody("qr"))));
        verifyNoInteractions(strategy);
        client.setGrantType("password, qr ");
        assertEquals(200, responseCode(post("/auth/login").contentType("application/json").content(loginBody("qr"))));
        verify(strategy).login(loginBody("qr"), client);
    }

    private String token(Set<String> permissions) {
        return issueInBoundRequest(IAuthStrategy.buildLoginParameter(client), permissions, Set.of());
    }

    private String issueInBoundRequest(SaLoginParameter parameters, Set<String> permissions, Set<String> roles) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest(), new MockHttpServletResponse()));
        try { return issueToken(parameters, permissions, roles); }
        finally { RequestContextHolder.resetRequestAttributes(); }
    }

    private String issueToken(SaLoginParameter parameters, Set<String> permissions, Set<String> roles) {
        LoginUser user = new LoginUser(); user.setUserType("sys_user"); user.setUserId(9_007_199_254_740_993L);
        user.setUsername("contract-user"); user.setDeptId(9_007_199_254_740_999L);
        user.setMenuPermission(permissions); user.setRolePermission(roles);
        user.setIpaddr("127.0.0.1"); user.setLoginLocation("test"); user.setBrowser("test"); user.setOs("test");
        LoginHelper.login(user, parameters); return StpUtil.getTokenValue();
    }

    private int responseCode(MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(request).andReturn().getResponse();
        assertEquals(200, response.getStatus(), response.getContentAsString());
        return JsonMapper.builder().build().readTree(response.getContentAsString()).get("code").asInt();
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token).header("clientid", "web-client");
    }

    private static String loginBody(String grant) {
        return "{\"clientId\":\"web-client\",\"grantType\":\"" + grant + "\"}";
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class MvcConfig implements WebMvcConfigurer {
        @Override public void addInterceptors(InterceptorRegistry registry) {
            SecurityProperties properties = new SecurityProperties(); properties.setExcludes(new String[]{"/assets/**"});
            new SecurityConfig(properties).addInterceptors(registry);
        }
        @Bean SpringUtils springUtils() { return new SpringUtils(); }
        @Bean AllUrlHandler allUrlHandler() { return new AllUrlHandler(); }
        @Bean JsonMapper jsonMapper() { return JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build(); }
        @Bean @Primary LocalValidatorFactoryBean validator() { return new LocalValidatorFactoryBean(); }
        @Bean ResourceBundleMessageSource messageSource() {
            var messages = new ResourceBundleMessageSource(); messages.setBasename("i18n/messages"); return messages;
        }
        @Bean SaTokenExceptionHandler tokenExceptionHandler() { return new SaTokenExceptionHandler(); }
        @Bean ContractController contractController() { return new ContractController(); }
        @Bean ISysClientService clientService() { return mock(ISysClientService.class); }
        @Bean ISysSocialService socialService() { return mock(ISysSocialService.class); }
        @Bean ScheduledExecutorService scheduler() { return mock(ScheduledExecutorService.class); }
        @Bean({"passwordAuthStrategy", "xcxAuthStrategy", "qrAuthStrategy"}) IAuthStrategy strategy() { return mock(IAuthStrategy.class); }
        @Bean cn.com.nla.web.service.QrLoginService qrService() { return mock(cn.com.nla.web.service.QrLoginService.class); }
        @Bean QrLoginController qrController(cn.com.nla.web.service.QrLoginService qr) { return new QrLoginController(qr); }
        @Bean WechatMiniClient wechatMiniClient() { return mock(WechatMiniClient.class); }
        @Bean SysXcxBindingService xcxBindings() { return mock(SysXcxBindingService.class); }
        @Bean cn.com.nla.web.service.WechatPhoneClient wechatPhoneClient() { return mock(cn.com.nla.web.service.WechatPhoneClient.class); }
        @Bean cn.com.nla.web.config.properties.XcxProperties xcxProperties() { return new cn.com.nla.web.config.properties.XcxProperties(); }
        @Bean cn.com.nla.system.service.ISysUserService xcxUserService() { return mock(cn.com.nla.system.service.ISysUserService.class); }
        @Bean XcxBindingController xcxController(WechatMiniClient provider, SysXcxBindingService bindings,
                cn.com.nla.web.service.WechatPhoneClient phoneClient, cn.com.nla.web.config.properties.XcxProperties props,
                cn.com.nla.system.service.ISysUserService userService) {
            return new XcxBindingController(provider, bindings, phoneClient, props, userService);
        }
        @Bean AuthController authController(ISysClientService clients, ISysSocialService social, ScheduledExecutorService scheduler) {
            return new AuthController(new SocialProperties(), mock(SysLoginService.class), mock(SysRegisterService.class),
                mock(ISysConfigService.class), social, clients, scheduler, mock(MessageService.class));
        }
    }

    @RestController
    static class ContractController {
        @GetMapping("/contract/plain") R<Void> plain() { return R.ok(); }
        @GetMapping("/contract/read") @SaCheckPermission("contract:read") R<Void> read() { return R.ok(); }
        @GetMapping("/contract/role") @SaCheckRole("operator") R<Void> role() { return R.ok(); }
        @GetMapping("/files/{path}") R<Void> files() { return R.ok(); }
        @GetMapping("/assets/open") R<Void> assets() { return R.ok(); }
        @GetMapping("/public") @SaIgnore R<Void> publicEndpoint() { return R.ok(); }
    }
}
