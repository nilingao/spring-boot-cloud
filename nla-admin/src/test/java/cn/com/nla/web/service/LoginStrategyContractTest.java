package cn.com.nla.web.service;

import cn.com.nla.common.core.constant.CacheNames;
import cn.com.nla.common.core.constant.Constants;
import cn.com.nla.common.core.constant.GlobalConstants;
import cn.com.nla.common.core.enums.LoginType;
import cn.com.nla.common.core.exception.user.CaptchaException;
import cn.com.nla.common.core.exception.user.CaptchaExpireException;
import cn.com.nla.common.core.exception.user.UserException;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.common.mail.config.properties.MailProperties;
import cn.com.nla.common.log.event.LoginInfoEvent;
import cn.com.nla.common.mybatis.core.mapper.LambdaCrudChainWrapper;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.web.config.properties.CaptchaProperties;
import cn.com.nla.message.domain.MobileMessage;
import cn.com.nla.message.domain.MobileMessageTemplate;
import cn.com.nla.message.domain.SmsConfig;
import cn.com.nla.message.mapper.MobileMessageMapper;
import cn.com.nla.message.sms.SmsConstant;
import cn.com.nla.message.sms.core.SmsChannelManager;
import cn.com.nla.message.sms.core.SmsSendManager;
import cn.com.nla.system.domain.vo.SysClientVo;
import cn.com.nla.system.domain.SysUser;
import cn.com.nla.system.domain.vo.SysDeptVo;
import cn.com.nla.system.domain.vo.SysUserVo;
import cn.com.nla.system.mapper.SysUserMapper;
import cn.com.nla.system.service.*;
import cn.com.nla.web.domain.vo.LoginVo;
import cn.com.nla.web.controller.CaptchaController;
import cn.com.nla.web.service.impl.EmailAuthStrategy;
import cn.com.nla.web.service.impl.PasswordAuthStrategy;
import cn.com.nla.web.service.impl.SmsAuthStrategy;
import cn.com.nla.web.service.impl.XcxAuthStrategy;
import cn.com.nla.web.service.impl.QrAuthStrategy;
import cn.com.nla.web.config.properties.XcxProperties;
import cn.com.nla.web.domain.model.QrLoginModels;
import cn.com.nla.system.api.model.XcxLoginUser;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.json.SaJsonTemplate;
import cn.dev33.satoken.json.SaJsonTemplateForJackson3;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 执行真实登录策略、BCrypt、失败计数服务及 JWT 会话；用户查询和 Redis 操作使用测试替身。
 * 不连接数据库、Redis 或短信/邮件供应商，不改变基线认证策略。
 */
class LoginStrategyContractTest {
    private static final long USER_ID = 9_007_199_254_740_993L;
    private static final String USERNAME = "contract-user";
    private static final String PASSWORD = "test-password";
    private static final String HASH = BCrypt.hashpw(PASSWORD, BCrypt.gensalt(4));
    private static final String PHONE = "13800138000";
    private static final String SMS_KEY = SmsConstant.verificationCodeKey(SmsConstant.TYPE_LOGIN, PHONE);
    private static final String EMAIL = "contract@example.com";
    private static final String ERROR_KEY = CacheNames.PWD_ERR_CNT_KEY + USERNAME;
    private static GenericApplicationContext context;
    private static Object previousSpringContext;
    private static Object previousSpringFactory;
    private static final List<LoginInfoEvent> loginEvents = new ArrayList<>();

    private final Map<String, Object> cache = new HashMap<>();
    private final Map<String, Duration> durations = new HashMap<>();
    private MockedStatic<RedisUtils> redis;
    private SaTokenConfig previousConfig;
    private SaTokenDao previousDao;
    private SaTokenContext previousContext;
    private SaJsonTemplate previousJson;
    private StpLogic previousLogic;
    private RequestAttributes previousRequest;
    private SaTokenDaoDefaultImpl tokenDao;
    private SysUserMapper users;
    private SysUserVo user;
    private SysClientVo client;
    private SysLoginService loginService;
    private CaptchaProperties captcha;
    private PasswordAuthStrategy passwordStrategy;
    private SmsAuthStrategy smsStrategy;
    private EmailAuthStrategy emailStrategy;
    private RBucket<String> smsBucket;
    private MockedStatic<SmsFactory> smsFactory;
    private SmsChannelManager channels;
    private MobileMessageMapper smsRecords;
    private SmsBlend provider;
    private MobileMessageTemplate smsTemplate;
    private CaptchaController smsController;

    @BeforeAll
    static void prepareUtilities() {
        previousSpringContext = ReflectionTestUtils.getField(SpringUtil.class, "applicationContext");
        previousSpringFactory = ReflectionTestUtils.getField(SpringUtil.class, "beanFactory");
        context = new GenericApplicationContext();
        context.addApplicationListener(event -> {
            if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof LoginInfoEvent info) {
                loginEvents.add(info);
            }
        });
        context.registerBean(SpringUtils.class, SpringUtils::new);
        context.registerBean(JsonMapper.class, () -> JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build());
        context.registerBean(LocalValidatorFactoryBean.class, LocalValidatorFactoryBean::new);
        context.registerBean("messageSource", ResourceBundleMessageSource.class, () -> {
            var messages = new ResourceBundleMessageSource();
            messages.setBasename("i18n/messages");
            return messages;
        });
        // RedisUtils 的静态客户端初始化需要此 Bean；实际 Redis 操作由每测静态替身接管。
        context.registerBean(RedissonClient.class, () -> mock(RedissonClient.class));
        context.refresh();
    }

    @AfterAll
    static void restoreUtilities() {
        if (context != null) {
            context.close();
        }
        ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", previousSpringContext);
        ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", previousSpringFactory);
    }

    @BeforeEach
    void prepareStrategies() {
        loginEvents.clear();
        previousConfig = SaManager.getConfig();
        previousDao = SaManager.getSaTokenDao();
        previousContext = SaManager.getSaTokenContext();
        previousJson = SaManager.getSaJsonTemplate();
        previousLogic = StpUtil.getStpLogic();
        SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization").setTokenPrefix("Bearer")
            .setJwtSecretKey("strategy-contract-key-with-no-production-use").setIsReadCookie(false)
            .setIsShare(false).setDynamicActiveTimeout(true).setIsPrint(false).setIsLog(false));
        tokenDao = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(tokenDao);
        SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
        SaManager.setSaJsonTemplate(new SaJsonTemplateForJackson3());
        StpUtil.setStpLogic(new StpLogicJwtForSimple());
        previousRequest = RequestContextHolder.getRequestAttributes();
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("clientid", "web-client");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));

        redis = mockStatic(RedisUtils.class);
        redis.when(() -> RedisUtils.getCacheObject(anyString())).thenAnswer(call -> cache.get(call.getArgument(0)));
        redis.when(() -> RedisUtils.setCacheObject(anyString(), any(), any(Duration.class))).thenAnswer(call -> {
            cache.put(call.getArgument(0), call.getArgument(1));
            durations.put(call.getArgument(0), call.getArgument(2));
            return null;
        });
        redis.when(() -> RedisUtils.deleteObject(anyString())).thenAnswer(call -> {
            durations.remove(call.getArgument(0));
            return cache.remove(call.getArgument(0)) != null;
        });
        redis.when(() -> RedisUtils.isExistsObject(anyString())).thenAnswer(call -> cache.containsKey(call.getArgument(0)));
        redis.when(() -> RedisUtils.getTimeToLive(anyString())).thenAnswer(call ->
            durations.getOrDefault(call.getArgument(0), Duration.ZERO).toMillis());
        var redisClient = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        smsBucket = bucket;
        when(redisClient.<String>getBucket(SMS_KEY)).thenReturn(smsBucket);
        redis.when(RedisUtils::getClient).thenReturn(redisClient);
        when(smsBucket.compareAndSet(anyString(), isNull())).thenAnswer(call -> {
            synchronized (cache) {
                if (!call.getArgument(0).equals(cache.get(SMS_KEY))) {
                    return false;
                }
                cache.remove(SMS_KEY);
                durations.remove(SMS_KEY);
                return true;
            }
        });

        users = mock(SysUserMapper.class);
        user = new SysUserVo();
        user.setUserId(USER_ID);
        user.setDeptId(USER_ID + 6);
        user.setUserName(USERNAME);
        user.setNickName("Contract User");
        user.setUserType("sys_user");
        user.setPassword(HASH);
        user.setStatus("0");
        @SuppressWarnings("unchecked")
        LambdaCrudChainWrapper<SysUser, SysUserVo> query = mock(LambdaCrudChainWrapper.class);
        when(users.lambda()).thenReturn(query);
        when(query.eq(any(), any())).thenReturn(query);
        when(query.voOne()).thenAnswer(call -> user);
        clearInvocations(users);
        var permissions = mock(ISysPermissionService.class);
        when(permissions.getMenuPermission(USER_ID)).thenReturn(Set.of("contract:read"));
        when(permissions.getRolePermission(USER_ID)).thenReturn(Set.of("operator"));
        var departments = mock(ISysDeptService.class);
        var department = new SysDeptVo();
        department.setDeptName("Contract Department");
        department.setDeptCategory("test");
        when(departments.selectDeptById(USER_ID + 6)).thenReturn(department);
        loginService = spy(new SysLoginService(permissions, mock(ISysSocialService.class),
            mock(ISysRoleService.class), departments, mock(ISysPostService.class), users));
        ReflectionTestUtils.setField(loginService, "maxRetryCount", 5);
        ReflectionTestUtils.setField(loginService, "lockTime", 10);

        captcha = new CaptchaProperties();
        captcha.setEnable(false);
        passwordStrategy = new PasswordAuthStrategy(captcha, loginService, users);
        smsStrategy = new SmsAuthStrategy(loginService, users);
        emailStrategy = new EmailAuthStrategy(loginService, users);
        client = new SysClientVo();
        client.setClientId("web-client");
        client.setClientKey("web");
        client.setDeviceType("pc");
        client.setTimeout(1800L);
        client.setActiveTimeout(600L);
    }

    @AfterEach
    void restoreTokenState() {
        RequestContextHolder.setRequestAttributes(previousRequest);
        if (smsFactory != null) {
            smsFactory.close();
        }
        if (redis != null) {
            redis.close();
        }
        StpUtil.setStpLogic(previousLogic);
        SaManager.setConfig(previousConfig);
        SaManager.setSaTokenDao(previousDao);
        SaManager.setSaTokenContext(previousContext);
        SaManager.setSaJsonTemplate(previousJson);
        if (tokenDao != null) {
            tokenDao.destroy();
        }
    }

    @Test
    void realBcryptPasswordBuildsJwtAndPermissionsAndClearsPriorFailures() {
        cache.put(ERROR_KEY, 2);
        assertSession(passwordStrategy.login(passwordBody(PASSWORD, null, null), client));
        assertFalse(cache.containsKey(ERROR_KEY));
        verify(loginService).buildLoginUser(user);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "curl/8.0", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"})
    void optionalUserAgentNeverBlocksValidPasswordAndRetainsAvailableMetadata(String userAgent) {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        if (userAgent != null) {
            ((MockHttpServletRequest) request).addHeader("User-Agent", userAgent);
        }
        LoginVo view = passwordStrategy.login(passwordBody(PASSWORD, null, null), client);
        assertSession(view);
        var session = LoginHelper.getLoginUser(view.getAccessToken());
        assertEquals("127.0.0.1", session.getIpaddr());
        if (userAgent == null || userAgent.isBlank()) {
            assertNull(session.getBrowser());
            assertNull(session.getOs());
        } else {
            assertNotNull(session.getBrowser());
            assertNotNull(session.getOs());
        }
    }

    @Test
    void fifthWrongPasswordLocksAccountAndCorrectPasswordCannotBypassLock() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            UserException failure = assertThrows(UserException.class,
                () -> passwordStrategy.login(passwordBody("wrong-password", null, null), client));
            assertEquals(attempt == 5 ? LoginType.PASSWORD.getRetryLimitExceed()
                : LoginType.PASSWORD.getRetryLimitCount(), failure.getCode());
            assertEquals(attempt, cache.get(ERROR_KEY));
            assertEquals(Duration.ofMinutes(10), durations.get(ERROR_KEY));
        }
        UserException locked = assertThrows(UserException.class,
            () -> passwordStrategy.login(passwordBody(PASSWORD, null, null), client));
        assertEquals(LoginType.PASSWORD.getRetryLimitExceed(), locked.getCode());
        assertEquals(5, cache.get(ERROR_KEY));
        verify(loginService, never()).buildLoginUser(any());
        assertEquals(6, loginEvents.size());
        assertTrue(loginEvents.stream().allMatch(event -> USERNAME.equals(event.getUsername())
            && Constants.LOGIN_FAIL.equals(event.getStatus()) && "127.0.0.1".equals(event.getIp())
            && "web-client".equals(event.getClientId())));
    }

    @Test
    void lockIsCheckedBeforeCredentialSupplierAndDoesNotExtendDuration() {
        cache.put(ERROR_KEY, 5);
        var evaluated = new AtomicBoolean();
        assertThrows(UserException.class, () -> loginService.checkLogin(LoginType.PASSWORD, USERNAME, () -> {
            evaluated.set(true);
            return false;
        }));
        assertFalse(evaluated.get());
        redis.verify(() -> RedisUtils.setCacheObject(anyString(), any(), any(Duration.class)), never());
    }

    @Test
    void removedExpiredLockAllowsValidPasswordAgain() {
        cache.put(ERROR_KEY, 5);
        assertThrows(UserException.class, () -> passwordStrategy.login(passwordBody(PASSWORD, null, null), client));
        // 模拟缓存项过期；不宣称验证了真实 Redis 的时间推进。
        cache.remove(ERROR_KEY);
        assertSession(passwordStrategy.login(passwordBody(PASSWORD, null, null), client));
    }

    @Test
    void retryCountersAreIsolatedByUsername() {
        cache.put(ERROR_KEY, 5);
        loginService.checkLogin(LoginType.PASSWORD, "other-user", () -> false);
        assertEquals(5, cache.get(ERROR_KEY));
        assertThrows(UserException.class, () -> loginService.checkLogin(LoginType.PASSWORD, "other-user", () -> true));
        assertEquals(1, cache.get(CacheNames.PWD_ERR_CNT_KEY + "other-user"));
    }

    @Test
    void validImageCaptchaIsCaseInsensitiveConsumedAndCannotBeReplayed() {
        captcha.setEnable(true);
        cache.put(GlobalConstants.CAPTCHA_CODE_KEY + "image-id", "AbC1");
        assertSession(passwordStrategy.login(passwordBody(PASSWORD, "aBc1", "image-id"), client));
        assertFalse(cache.containsKey(GlobalConstants.CAPTCHA_CODE_KEY + "image-id"));
        clearInvocations(loginService);
        assertThrows(CaptchaExpireException.class,
            () -> passwordStrategy.login(passwordBody(PASSWORD, "aBc1", "image-id"), client));
        verify(loginService, never()).buildLoginUser(any());
    }

    @Test
    void wrongImageCaptchaIsConsumedBeforeUserLookupWithoutPasswordRetry() {
        captcha.setEnable(true);
        cache.put(GlobalConstants.CAPTCHA_CODE_KEY + "image-id", "AbC1");
        assertThrows(CaptchaException.class,
            () -> passwordStrategy.login(passwordBody(PASSWORD, "wrong", "image-id"), client));
        assertFalse(cache.containsKey(GlobalConstants.CAPTCHA_CODE_KEY + "image-id"));
        verifyNoInteractions(users);
        assertFalse(cache.containsKey(ERROR_KEY));
    }

    @Test
    void imageCaptchaIdDoesNotConsumeAnotherCaptcha() {
        captcha.setEnable(true);
        cache.put(GlobalConstants.CAPTCHA_CODE_KEY + "other-id", "AbC1");
        assertThrows(CaptchaExpireException.class,
            () -> passwordStrategy.login(passwordBody(PASSWORD, "AbC1", "missing-id"), client));
        assertEquals("AbC1", cache.get(GlobalConstants.CAPTCHA_CODE_KEY + "other-id"));
        verifyNoInteractions(users);
    }

    @Test
    void disabledImageCaptchaDoesNotReadCaptchaCache() {
        assertSession(passwordStrategy.login(passwordBody(PASSWORD, null, null), client));
        redis.verify(() -> RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY), never());
    }

    @ParameterizedTest
    @ValueSource(strings = {"math", "char"})
    void generatedCaptchaImageAndCachedAnswerWorkWithRealPasswordStrategy(String type) throws Exception {
        captcha.setEnable(true);
        captcha.setType(type);
        captcha.setNumberLength(1);
        captcha.setCharLength(4);
        var controller = new CaptchaController(captcha, new MailProperties(), mock(SmsLoginCodeService.class));
        // 直接验证生成方法；限流切面的真实 Redis 行为不在此测试内。
        var generated = controller.getCodeImpl();
        assertTrue(generated.captchaEnabled());
        assertNotNull(generated.uuid());
        String key = GlobalConstants.CAPTCHA_CODE_KEY + generated.uuid();
        String answer = (String) cache.get(key);
        assertNotNull(answer);
        assertFalse(answer.isBlank());
        assertEquals(Duration.ofMinutes(cn.com.nla.common.core.constant.Constants.CAPTCHA_EXPIRATION), durations.get(key));
        var image = javax.imageio.ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(generated.img())));
        assertNotNull(image);
        assertEquals(160, image.getWidth());
        assertEquals(60, image.getHeight());
        assertSession(passwordStrategy.login(passwordBody(PASSWORD, answer, generated.uuid()), client));
        assertFalse(cache.containsKey(key));
    }

    @Test
    void disabledCaptchaEndpointDoesNotGenerateOrCacheAnImage() {
        var result = new CaptchaController(captcha, new MailProperties(), mock(SmsLoginCodeService.class)).getCode();
        assertEquals(200, result.getCode());
        assertFalse(result.getData().captchaEnabled());
        assertNull(result.getData().uuid());
        assertNull(result.getData().img());
        redis.verifyNoInteractions();
    }

    @ParameterizedTest
    @CsvSource({"username,x", "username,abcdefghijklmnopqrstuvwxyz12345", "password,1234",
        "password,abcdefghijklmnopqrstuvwxyz12345", "clientId,''", "grantType,''"})
    void invalidPasswordRequestIsRejectedBeforeUserLookup(String field, String value) {
        var body = baseBody("password");
        body.put("username", USERNAME);
        body.put("password", PASSWORD);
        body.put(field, value);
        assertThrows(ConstraintViolationException.class, () -> passwordStrategy.login(JsonUtils.toJsonString(body), client));
        verifyNoInteractions(users);
        redis.verifyNoInteractions();
    }

    @ParameterizedTest
    @CsvSource({"password,missing", "password,disabled", "sms,missing", "sms,disabled", "email,missing", "email,disabled"})
    void missingOrDisabledAccountCannotCreateSession(String grant, String state) {
        if ("missing".equals(state)) {
            user = null;
        } else {
            user.setStatus("1");
        }
        UserException failure = assertThrows(UserException.class, () -> login(grant, "1234"));
        assertEquals("missing".equals(state) ? "user.not.exists" : "user.blocked", failure.getCode());
        verify(loginService, never()).checkLogin(any(), anyString(), any());
        verify(loginService, never()).buildLoginUser(any());
        redis.verifyNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms", "email"})
    void validCodeCreatesRealSessionAndClearsRetryCount(String grant) {
        cache.put(codeKey(grant), "1234");
        cache.put(ERROR_KEY, 2);
        assertSession(login(grant, "1234"));
        assertFalse(cache.containsKey(ERROR_KEY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms", "email"})
    void wrongCodeIncrementsAccountRetryAndCannotBuildSession(String grant) {
        cache.put(codeKey(grant), "1234");
        UserException failure = assertThrows(UserException.class, () -> login(grant, "4321"));
        assertEquals("sms".equals(grant) ? LoginType.SMS.getRetryLimitCount() : LoginType.EMAIL.getRetryLimitCount(), failure.getCode());
        assertEquals(1, cache.get(ERROR_KEY));
        assertEquals(Duration.ofMinutes(10), durations.get(ERROR_KEY));
        verify(loginService, never()).buildLoginUser(any());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> missingCodes() {
        return Stream.of("sms", "email").flatMap(grant -> Stream.of(null, "", " ")
            .map(code -> org.junit.jupiter.params.provider.Arguments.of(grant, code)));
    }

    @ParameterizedTest
    @MethodSource("missingCodes")
    void missingOrExpiredCodeDoesNotCreateSessionOrIncrementRetry(String grant, String cachedCode) {
        cache.put(codeKey(grant), cachedCode);
        assertThrows(CaptchaExpireException.class, () -> login(grant, "1234"));
        assertFalse(cache.containsKey(ERROR_KEY));
        verify(loginService, never()).buildLoginUser(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sms", "email"})
    void passwordLockAlsoRejectsCodeStrategiesWithoutReadingCode(String grant) {
        cache.put(ERROR_KEY, 5);
        cache.put(codeKey(grant), "1234");
        UserException failure = assertThrows(UserException.class, () -> login(grant, "1234"));
        assertEquals("sms".equals(grant) ? LoginType.SMS.getRetryLimitExceed() : LoginType.EMAIL.getRetryLimitExceed(), failure.getCode());
        redis.verify(() -> RedisUtils.getCacheObject(codeKey(grant)), never());
        verify(loginService, never()).buildLoginUser(any());
    }

    private XcxAuthStrategy prepareXcx(WechatMiniClient provider, SysXcxBindingService bindings) {
        when(provider.exchange("wx0123456789abcdef", "wx-code", "web-client"))
            .thenReturn(new WechatMiniClient.Identity("wx0123456789abcdef", "verified-openid", null));
        when(bindings.findUserId("wx0123456789abcdef", "verified-openid")).thenReturn(USER_ID);
        when(users.selectVoById(USER_ID)).thenAnswer(call -> user);
        return new XcxAuthStrategy(loginService, provider, bindings, users);
    }

    private String xcxBody() {
        var body = baseBody("xcx");
        body.put("appid", "wx0123456789abcdef"); body.put("xcxCode", "wx-code");
        return JsonUtils.toJsonString(body);
    }

    @Test
    void verifiedXcxIdentityBuildsFullPermissionsAndJwtFromBoundAccount() {
        var provider = mock(WechatMiniClient.class); var bindings = mock(SysXcxBindingService.class);
        var strategy = prepareXcx(provider, bindings);
        cache.put(ERROR_KEY, 2);
        var view = strategy.login(xcxBody(), client);
        assertSession(view);
        assertFalse(cache.containsKey(ERROR_KEY));
        XcxLoginUser session = LoginHelper.getLoginUser(view.getAccessToken());
        assertEquals("wx0123456789abcdef", session.getAppid());
        assertEquals("verified-openid", session.getOpenid());
        assertEquals("verified-openid", view.getOpenid());
        verify(loginService).buildLoginUser(user);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unbound", "missing", "disabled", "locked", "provider"})
    void rejectedXcxLoginNeverBuildsSession(String state) {
        var provider = mock(WechatMiniClient.class); var bindings = mock(SysXcxBindingService.class);
        var strategy = prepareXcx(provider, bindings);
        switch (state) {
            case "unbound" -> when(bindings.findUserId(anyString(), anyString())).thenThrow(new ServiceException("unbound"));
            case "missing" -> user = null;
            case "disabled" -> user.setStatus("1");
            case "locked" -> cache.put(ERROR_KEY, 5);
            case "provider" -> when(provider.exchange(anyString(), anyString(), anyString())).thenThrow(new ServiceException("provider failed"));
            default -> fail("Unknown scenario");
        }
        if ("unbound".equals(state) || "provider".equals(state)) {
            assertThrows(ServiceException.class, () -> strategy.login(xcxBody(), client));
        } else {
            var failure = assertThrows(UserException.class, () -> strategy.login(xcxBody(), client));
            assertEquals(switch (state) {
                case "missing" -> "user.not.exists";
                case "disabled" -> "user.blocked";
                default -> LoginType.XCX.getRetryLimitExceed();
            }, failure.getCode());
        }
        assertFalse(StpUtil.isLogin());
        verify(loginService, never()).buildLoginUser(any());
        if ("locked".equals(state)) { assertEquals(5, cache.get(ERROR_KEY)); }
        if ("provider".equals(state)) { verifyNoInteractions(bindings); }
    }

    @ParameterizedTest
    @CsvSource({"appid,''", "appid,invalid", "xcxCode,''", "clientId,''", "grantType,''"})
    void invalidXcxInputCannotReachProvider(String field, String value) {
        var provider = mock(WechatMiniClient.class); var bindings = mock(SysXcxBindingService.class);
        var strategy = new XcxAuthStrategy(loginService, provider, bindings, users);
        var body = baseBody("xcx"); body.put("appid", "wx0123456789abcdef"); body.put("xcxCode", "wx-code");
        body.put(field, value);
        assertThrows(ConstraintViolationException.class, () -> strategy.login(JsonUtils.toJsonString(body), client));
        verifyNoInteractions(provider, bindings, users);
    }

    @Test
    void xcxUsesClientSelectedByServerAndIgnoresSpoofedOpenidAndUserId() {
        var provider = mock(WechatMiniClient.class); var bindings = mock(SysXcxBindingService.class);
        var strategy = prepareXcx(provider, bindings);
        var body = baseBody("xcx"); body.put("appid", "wx0123456789abcdef"); body.put("xcxCode", "wx-code");
        body.put("userId", USER_ID + 1); body.put("openid", "spoofed-openid"); body.put("clientId", "spoofed-client");
        assertSession(strategy.login(JsonUtils.toJsonString(body), client));
        verify(provider).exchange("wx0123456789abcdef", "wx-code", "web-client");
        verify(bindings).findUserId("wx0123456789abcdef", "verified-openid");
        verify(users).selectVoById(USER_ID);
    }

    /** 执行真实 Controller → Service → 发送引擎，仅渠道查询、供应商和 Redis 使用替身。 */
    private void prepareSmsSending() {
        channels = mock(SmsChannelManager.class);
        smsRecords = mock(MobileMessageMapper.class);
        provider = mock(SmsBlend.class);
        var account = new SmsConfig();
        account.setId(101L);
        account.setSmsType(5);
        smsTemplate = new MobileMessageTemplate();
        smsTemplate.setType(SmsConstant.TYPE_LOGIN);
        smsTemplate.setCode("5:login-template,8:other-template");
        smsTemplate.setContent("验证码 VERIFICATION_CODE，有效期 REDIS_CODE 分钟");
        smsTemplate.setVariable("{\"VERIFICATION_CODE\":\"\",\"REDIS_CODE\":\"\"}");
        when(channels.findActiveConfigs(SmsConstant.TYPE_LOGIN)).thenReturn(List.of(account));
        when(channels.findLastTemplate(101L, SmsConstant.TYPE_LOGIN)).thenReturn(smsTemplate);
        SmsResponse response = mock(SmsResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getData()).thenReturn("provider-message-id");
        when(provider.sendMessage(eq(PHONE), eq("login-template"), any(LinkedHashMap.class))).thenReturn(response);
        smsFactory = mockStatic(SmsFactory.class);
        smsFactory.when(() -> SmsFactory.getSmsBlend("101")).thenReturn(provider);
        var service = new SmsLoginCodeService(users, new SmsSendManager(channels, smsRecords));
        smsController = new CaptchaController(captcha, new MailProperties(), service);
    }

    @Test
    void tableDrivenSmsReachesRealLoginAndCodeCannotBeReplayed() {
        prepareSmsSending();
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        String code = (String) cache.get(SMS_KEY);
        assertNotNull(code);
        assertTrue(code.matches("\\d{6}"));
        assertEquals(Duration.ofMinutes(5), durations.get(SMS_KEY));
        assertFalse(cache.containsKey(GlobalConstants.CAPTCHA_CODE_KEY + PHONE));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LinkedHashMap<String, String>> parameters = ArgumentCaptor.forClass(LinkedHashMap.class);
        verify(provider).sendMessage(eq(PHONE), eq("login-template"), parameters.capture());
        assertEquals(code, parameters.getValue().get(SmsConstant.VERIFICATION_CODE));
        var record = ArgumentCaptor.forClass(MobileMessage.class);
        verify(smsRecords).insert(record.capture());
        assertEquals(101L, record.getValue().getSenderId());
        assertEquals(SmsConstant.STATUS_SUCCESS, record.getValue().getStatus());
        assertTrue(record.getValue().getContent().contains(code));
        cache.put(ERROR_KEY, 2);
        assertSession(login("sms", code));
        assertFalse(cache.containsKey(SMS_KEY));
        assertFalse(cache.containsKey(ERROR_KEY));
        clearInvocations(loginService);
        assertThrows(CaptchaExpireException.class, () -> login("sms", code));
        verify(loginService, never()).buildLoginUser(any());
    }

    @Test
    void resendWhileCodeExistsRetainsCodeAndDoesNotDispatchOrRecordAgain() {
        prepareSmsSending();
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        Object code = cache.get(SMS_KEY);
        var repeated = smsController.smsCode(PHONE);
        assertEquals(200, repeated.getCode());
        assertTrue(repeated.getMsg().contains("已发送短信"));
        assertEquals(code, cache.get(SMS_KEY));
        verify(provider, times(1)).sendMessage(eq(PHONE), eq("login-template"), any(LinkedHashMap.class));
        verify(smsRecords, times(1)).insert(any(MobileMessage.class));
    }

    @Test
    void expiredCodeAllowsNewSendAndPreviousCodeCannotLogin() {
        prepareSmsSending();
        smsTemplate.setVariable("{\"VERIFICATION_CODE\":\"123456\",\"REDIS_CODE\":5}");
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        String oldCode = (String) cache.get(SMS_KEY);
        // 模拟过期；固定模板测试值避免随机碰撞，不宣称验证真实 Redis 时间推进。
        cache.remove(SMS_KEY);
        durations.remove(SMS_KEY);
        smsTemplate.setVariable("{\"VERIFICATION_CODE\":\"654321\",\"REDIS_CODE\":5}");
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        String fresh = (String) cache.get(SMS_KEY);
        assertThrows(UserException.class, () -> login("sms", oldCode));
        assertEquals(fresh, cache.get(SMS_KEY));
        assertSession(login("sms", fresh));
        verify(provider, times(2)).sendMessage(eq(PHONE), eq("login-template"), any(LinkedHashMap.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "disabled"})
    void unavailableAccountDoesNotSendOrConsumeExistingSmsCode(String state) {
        prepareSmsSending();
        cache.put(SMS_KEY, "123456");
        if ("missing".equals(state)) { user = null; } else { user.setStatus("1"); }
        assertEquals(500, smsController.smsCode(PHONE).getCode());
        assertThrows(UserException.class, () -> login("sms", "123456"));
        assertEquals("123456", cache.get(SMS_KEY));
        verifyNoInteractions(channels, provider, smsRecords, smsBucket);
        redis.verifyNoInteractions();
    }

    @Test
    void accountDisabledAfterSendingCannotConsumeCodeOrBuildSession() {
        prepareSmsSending();
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        String code = (String) cache.get(SMS_KEY);
        user.setStatus("1");
        assertThrows(UserException.class, () -> login("sms", code));
        assertEquals(code, cache.get(SMS_KEY));
        verifyNoInteractions(smsBucket);
        verify(loginService, never()).buildLoginUser(any());
    }

    @Test
    void lockedAccountCannotConsumeCorrectSmsCode() {
        prepareSmsSending();
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        String code = (String) cache.get(SMS_KEY);
        cache.put(ERROR_KEY, 5);
        assertThrows(UserException.class, () -> login("sms", code));
        assertEquals(code, cache.get(SMS_KEY));
        verifyNoInteractions(smsBucket);
        verify(loginService, never()).buildLoginUser(any());
    }

    @Test
    void wrongSmsCodePreservesValidCodeAndIncrementsRetry() {
        prepareSmsSending();
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        String code = (String) cache.get(SMS_KEY);
        assertThrows(UserException.class, () -> login("sms", "wrong-code"));
        assertEquals(1, cache.get(ERROR_KEY));
        assertEquals(code, cache.get(SMS_KEY));
        verifyNoInteractions(smsBucket);
        assertSession(login("sms", code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"consumed", "replaced", "expired"})
    void losingAtomicConsumeNeverBuildsSessionOrDeletesReplacement(String race) {
        cache.put(SMS_KEY, "123456");
        doAnswer(call -> {
            if ("replaced".equals(race)) { cache.put(SMS_KEY, "654321"); }
            else { cache.remove(SMS_KEY); }
            return false;
        }).when(smsBucket).compareAndSet("123456", null);
        assertThrows(CaptchaExpireException.class, () -> login("sms", "123456"));
        verify(loginService, never()).buildLoginUser(any());
        assertFalse(cache.containsKey(ERROR_KEY));
        assertEquals("replaced".equals(race) ? "654321" : null, cache.get(SMS_KEY));
        redis.verify(() -> RedisUtils.deleteObject(SMS_KEY), never());
    }

    @Test
    void registrationAndLegacyCodesCannotBeUsedForSmsLogin() {
        String registerKey = SmsConstant.verificationCodeKey(SmsConstant.TYPE_REGISTER, PHONE);
        cache.put(registerKey, "123456");
        cache.put(GlobalConstants.CAPTCHA_CODE_KEY + PHONE, "123456");
        assertThrows(CaptchaExpireException.class, () -> login("sms", "123456"));
        assertEquals("123456", cache.get(registerKey));
        assertEquals("123456", cache.get(GlobalConstants.CAPTCHA_CODE_KEY + PHONE));
        verifyNoInteractions(smsBucket);
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-channel", "no-template", "unregistered", "failure", "exception"})
    void failedSendingCannotCacheCodeOrEnableLogin(String failure) {
        prepareSmsSending();
        switch (failure) {
            case "no-channel" -> when(channels.findActiveConfigs(1)).thenReturn(List.of());
            case "no-template" -> when(channels.findLastTemplate(101L, 1)).thenReturn(null);
            case "unregistered" -> smsFactory.when(() -> SmsFactory.getSmsBlend("101")).thenReturn(null);
            case "failure" -> {
                SmsResponse response = mock(SmsResponse.class);
                when(response.getData()).thenReturn("provider-rejected");
                when(provider.sendMessage(eq(PHONE), eq("login-template"), any(LinkedHashMap.class))).thenReturn(response);
            }
            case "exception" -> when(provider.sendMessage(eq(PHONE), eq("login-template"), any(LinkedHashMap.class)))
                .thenThrow(new IllegalStateException("provider unavailable"));
            default -> fail("Unknown scenario");
        }
        assertEquals(500, smsController.smsCode(PHONE).getCode());
        assertFalse(cache.containsKey(SMS_KEY));
        assertThrows(CaptchaExpireException.class, () -> login("sms", "123456"));
        verify(loginService, never()).buildLoginUser(any());
        if ("no-channel".equals(failure) || "no-template".equals(failure)) {
            verifyNoInteractions(provider, smsRecords);
        } else {
            var record = ArgumentCaptor.forClass(MobileMessage.class);
            verify(smsRecords).insert(record.capture());
            assertEquals(SmsConstant.STATUS_FAIL, record.getValue().getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "broken-json", "{\"REDIS_CODE\":5}",
        "{\"VERIFICATION_CODE\":\"\"}", "{\"VERIFICATION_CODE\":\"\",\"REDIS_CODE\":0}",
        "{\"VERIFICATION_CODE\":\"\",\"REDIS_CODE\":-1}",
        "{\"VERIFICATION_CODE\":\"\",\"REDIS_CODE\":\"invalid\"}",
        "{\"OTHER\":\"VERIFICATION_CODE REDIS_CODE\"}"})
    void invalidLoginTemplateNeverSendsAnUnusableCode(String variables) {
        prepareSmsSending();
        smsTemplate.setVariable(variables);
        assertEquals(500, smsController.smsCode(PHONE).getCode());
        assertFalse(cache.containsKey(SMS_KEY));
        verifyNoInteractions(provider, smsRecords);
    }

    @Test
    void loginTemplateOfWrongTypeIsRejectedBeforeSending() {
        prepareSmsSending();
        smsTemplate.setType(SmsConstant.TYPE_REGISTER);
        assertEquals(500, smsController.smsCode(PHONE).getCode());
        verifyNoInteractions(provider, smsRecords);
        assertFalse(cache.containsKey(SMS_KEY));
    }

    @Test
    void failedChannelFallsBackAndCachesOnlyTheSuccessfulChannelCode() {
        prepareSmsSending();
        var first = new SmsConfig(); first.setId(100L); first.setSmsType(5);
        var second = new SmsConfig(); second.setId(101L); second.setSmsType(5);
        when(channels.findActiveConfigs(1)).thenReturn(List.of(first, second));
        when(channels.findLastTemplate(100L, 1)).thenReturn(smsTemplate);
        SmsBlend failingProvider = mock(SmsBlend.class);
        smsFactory.when(() -> SmsFactory.getSmsBlend("100")).thenReturn(failingProvider);
        when(failingProvider.sendMessage(eq(PHONE), anyString(), any(LinkedHashMap.class)))
            .thenThrow(new IllegalStateException("first channel unavailable"));
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        var record = ArgumentCaptor.forClass(MobileMessage.class);
        verify(smsRecords, times(1)).insert(record.capture());
        assertEquals(101L, record.getValue().getSenderId());
        assertSession(login("sms", (String) cache.get(SMS_KEY)));
    }

    @Test
    void templateCacheDurationIsUsedBySenderAndLogin() {
        prepareSmsSending();
        smsTemplate.setVariable("{\"VERIFICATION_CODE\":\"\",\"REDIS_CODE\":2}");
        assertEquals(200, smsController.smsCode(PHONE).getCode());
        assertEquals(Duration.ofMinutes(2), durations.get(SMS_KEY));
        assertSession(login("sms", (String) cache.get(SMS_KEY)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "138", "email@example.com"})
    void invalidPhoneIsRejectedBeforeAccountOrProviderLookup(String phone) {
        prepareSmsSending();
        assertEquals(500, smsController.smsCode(phone).getCode());
        verifyNoInteractions(users, channels, provider, smsRecords);
        redis.verifyNoInteractions();
    }

    private LoginVo login(String grant, String code) {
        if ("password".equals(grant)) {
            return passwordStrategy.login(passwordBody(PASSWORD, null, null), client);
        }
        var body = baseBody(grant);
        body.put("sms".equals(grant) ? "phoneNumber" : "email", identifier(grant));
        body.put("sms".equals(grant) ? "smsCode" : "emailCode", code);
        return ("sms".equals(grant) ? smsStrategy : emailStrategy).login(JsonUtils.toJsonString(body), client);
    }

    private static final String QR_APP = "wx0123456789abcdef";
    private QrLoginService qr;
    private QrAuthStrategy qrStrategy;
    private XcxProperties qrProperties;
    private XcxProperties.App qrApp;
    private WechatQrClient qrImages;
    private QrSceneStore qrStore;
    private SysXcxBindingService qrBindings;
    private ISysClientService qrClients;
    private SysClientVo miniClient;
    private final Map<String, QrSceneStore.Snapshot> qrScenes = new HashMap<>();

    private QrLoginModels.Created prepareQr() {
        client.setStatus("0"); client.setGrantType("password, qr ,xcx");
        qrProperties = new XcxProperties(); qrProperties.setEnabled(true); qrProperties.setQrEnabled(true);
        qrApp = new XcxProperties.App(); qrApp.setSecret("test-secret"); qrApp.setQrPage("pages/login/index");
        qrApp.setQrClientIds(List.of("web-client")); qrApp.setClientIds(List.of("mini-client"));
        qrProperties.getApps().put(QR_APP, qrApp);
        qrClients = mock(ISysClientService.class);
        when(qrClients.queryByClientId("web-client")).thenAnswer(call -> client);
        miniClient = new SysClientVo(); miniClient.setClientId("mini-client"); miniClient.setStatus("0");
        miniClient.setGrantType("xcx"); miniClient.setDeviceType("mini"); miniClient.setTimeout(600L); miniClient.setActiveTimeout(300L);
        when(qrClients.queryByClientId("mini-client")).thenReturn(miniClient);
        qrImages = mock(WechatQrClient.class);
        when(qrImages.generate(eq(QR_APP), same(qrApp), anyString())).thenReturn("data:image/png;base64,test-image");
        qrStore = mock(QrSceneStore.class);
        when(qrStore.create(anyString(), any())).thenAnswer(call -> {
            String scene = call.getArgument(0); QrSceneStore.State value = call.getArgument(1);
            return qrScenes.putIfAbsent(scene, new QrSceneStore.Snapshot(JsonUtils.toJsonString(value), value)) == null;
        });
        when(qrStore.read(anyString())).thenAnswer(call -> qrScenes.get(call.getArgument(0)));
        when(qrStore.transition(anyString(), any(), any())).thenAnswer(call -> {
            String scene = call.getArgument(0); QrSceneStore.Snapshot expected = call.getArgument(1);
            QrSceneStore.State next = call.getArgument(2);
            return qrScenes.replace(scene, expected, new QrSceneStore.Snapshot(JsonUtils.toJsonString(next), next));
        });
        qrBindings = mock(SysXcxBindingService.class);
        when(qrBindings.findUserId(QR_APP, "verified-openid")).thenReturn(USER_ID);
        when(users.selectVoById(USER_ID)).thenAnswer(call -> user);
        qr = new QrLoginService(qrProperties, qrClients, qrImages, qrStore, qrBindings, users, loginService);
        qrStrategy = new QrAuthStrategy(qr);
        return qr.create(QR_APP, "web-client");
    }

    private XcxLoginUser qrScanner(String appid, String openid, Long userId) {
        var scanner = new XcxLoginUser();
        scanner.setUserType("sys_user"); scanner.setUserId(userId); scanner.setUsername(USERNAME);
        scanner.setAppid(appid); scanner.setOpenid(openid);
        LoginHelper.login(scanner, IAuthStrategy.buildLoginParameter(miniClient));
        String token = StpUtil.getTokenValue();
        var request = new MockHttpServletRequest(); request.setRemoteAddr("127.0.0.2");
        request.addHeader("Authorization", "Bearer " + token); request.addHeader("clientid", "mini-client");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
        return scanner;
    }

    private void qrBrowser() {
        var request = new MockHttpServletRequest(); request.setRemoteAddr("127.0.0.3");
        request.addHeader("clientid", "web-client"); request.addHeader("User-Agent", "Mozilla/5.0 Chrome/120.0.0.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));
    }

    private String qrBody(QrLoginModels.Created scene) {
        return JsonUtils.toJsonString(Map.of("clientId", "web-client", "grantType", "qr", "scene", scene.scene(),
            "browserToken", scene.browserToken(), "userId", 1, "openid", "spoofed"));
    }

    private void qrConfirm(QrLoginModels.Created scene) {
        qrScanner(QR_APP, "verified-openid", USER_ID);
        assertEquals("web-client", qr.scan(scene.scene()).clientId());
        qr.confirm(scene.scene(), true);
        qrBrowser();
    }

    @Test
    void qrBuildsBrowserJwtWithFullPermissionsAndFreshClientPolicyOnlyOnce() {
        var scene = prepareQr();
        assertTrue(scene.scene().matches("[0-9a-f]{32}")); assertTrue(scene.browserToken().matches("[A-Za-z0-9_-]{43}"));
        assertEquals(180, scene.expireIn());
        assertFalse(qrScenes.get(scene.scene()).raw().contains(scene.browserToken()));
        assertFalse(scene.toString().contains(scene.browserToken()));
        assertEquals("WAITING", qr.status(scene.scene(), scene.browserToken(), "web-client").status());
        qrConfirm(scene);
        assertEquals("CONFIRMED", qr.status(scene.scene(), scene.browserToken(), "web-client").status());
        client.setAccessPath("/contract/**"); client.setIpWhitelist("127.0.0.3");
        var view = qrStrategy.login(qrBody(scene), client); assertSession(view);
        var session = LoginHelper.getLoginUser(view.getAccessToken());
        assertFalse(session instanceof XcxLoginUser); assertEquals("127.0.0.3", session.getIpaddr());
        assertEquals("/contract/**", StpUtil.getExtra(LoginHelper.CLIENT_ACCESS_PATH_KEY));
        assertEquals("127.0.0.3", StpUtil.getExtra(LoginHelper.CLIENT_IP_WHITELIST_KEY));
        assertEquals("CONSUMED", qr.status(scene.scene(), scene.browserToken(), "web-client").status());
        clearInvocations(loginService);
        assertThrows(ServiceException.class, () -> qrStrategy.login(qrBody(scene), client));
        verify(loginService, never()).buildLoginUser(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"WAITING", "SCANNED", "CANCELLED", "expired"})
    void qrCannotIssueTokenBeforeConfirmationOrAfterCancellationOrExpiry(String phase) {
        var scene = prepareQr();
        if (!phase.equals("WAITING") && !phase.equals("expired")) {
            qrScanner(QR_APP, "verified-openid", USER_ID); qr.scan(scene.scene());
            if (phase.equals("CANCELLED")) { qr.confirm(scene.scene(), false); }
        }
        if (phase.equals("expired")) { qrScenes.remove(scene.scene()); }
        qrBrowser(); clearInvocations(loginService);
        assertThrows(ServiceException.class, () -> qrStrategy.login(qrBody(scene), client));
        verify(loginService, never()).buildLoginUser(any()); assertFalse(StpUtil.isLogin());
    }

    @Test
    void qrBrowserPossessionAndClientIsolationApplyToPollingAndRedemption() {
        var first = prepareQr(); var second = qr.create(QR_APP, "web-client"); qrConfirm(first);
        assertNotEquals(first.scene(), second.scene()); assertNotEquals(first.browserToken(), second.browserToken());
        assertThrows(ServiceException.class, () -> qr.status(first.scene(), second.browserToken(), "web-client"));
        assertThrows(ServiceException.class, () -> qr.redeem(first.scene(), second.browserToken(), "web-client"));
        assertThrows(ServiceException.class, () -> qr.status(first.scene(), first.browserToken(), "mini-client"));
        assertThrows(ServiceException.class, () -> qr.redeem(first.scene(), first.browserToken(), "mini-client"));
        assertEquals("CONFIRMED", qr.status(first.scene(), first.browserToken(), "web-client").status());
        assertSession(qrStrategy.login(qrBody(first), client));
    }

    @Test
    void qrRequiresExplicitScanAndOnlyFirstScannerCanConfirmOnce() {
        var scene = prepareQr(); qrScanner(QR_APP, "verified-openid", USER_ID);
        assertThrows(ServiceException.class, () -> qr.confirm(scene.scene(), true));
        qr.scan(scene.scene()); qr.scan(scene.scene()); // 本人重扫不覆盖场景。
        when(qrBindings.findUserId(QR_APP, "other-openid")).thenReturn(USER_ID + 1);
        when(users.selectVoById(USER_ID + 1)).thenReturn(user);
        qrBrowser(); qrScanner(QR_APP, "other-openid", USER_ID + 1);
        assertThrows(ServiceException.class, () -> qr.scan(scene.scene()));
        assertThrows(ServiceException.class, () -> qr.confirm(scene.scene(), true));
        qrBrowser(); qrScanner(QR_APP, "verified-openid", USER_ID);
        qr.confirm(scene.scene(), true);
        assertThrows(ServiceException.class, () -> qr.confirm(scene.scene(), true));
        assertThrows(ServiceException.class, () -> qr.confirm(scene.scene(), false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"web-session", "other-app", "binding-owner", "scanner-client-disabled", "scanner-allowlist"})
    void qrScannerMustHaveVerifiedMiniSessionWithCurrentBindingAndAllowedClient(String reason) {
        var scene = prepareQr();
        if (reason.equals("web-session")) { assertSession(passwordStrategy.login(passwordBody(PASSWORD, null, null), client)); }
        else { qrScanner(reason.equals("other-app") ? "wxfedcba9876543210" : QR_APP, "verified-openid", USER_ID); }
        if (reason.equals("binding-owner")) { when(qrBindings.findUserId(QR_APP, "verified-openid")).thenReturn(USER_ID + 1); }
        if (reason.equals("scanner-client-disabled")) { miniClient.setStatus("1"); }
        if (reason.equals("scanner-allowlist")) { qrApp.setClientIds(List.of()); }
        assertThrows(ServiceException.class, () -> qr.scan(scene.scene()));
        assertEquals(QrSceneStore.Phase.WAITING, qrScenes.get(scene.scene()).state().phase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "missing", "locked", "unbound", "reassigned", "client-disabled", "grant-removed", "app-disabled"})
    void qrReloadsAccountBindingAndConfigBeforeRedemption(String reason) {
        var scene = prepareQr(); qrConfirm(scene); clearInvocations(loginService);
        switch (reason) {
            case "disabled" -> user.setStatus("1");
            case "missing" -> user = null;
            case "locked" -> cache.put(ERROR_KEY, 5);
            case "unbound" -> when(qrBindings.findUserId(QR_APP, "verified-openid")).thenThrow(new ServiceException("unbound"));
            case "reassigned" -> when(qrBindings.findUserId(QR_APP, "verified-openid")).thenReturn(USER_ID + 1);
            case "client-disabled" -> client.setStatus("1");
            case "grant-removed" -> client.setGrantType("password,qrcode");
            case "app-disabled" -> qrProperties.setQrEnabled(false);
            default -> fail();
        }
        assertThrows(RuntimeException.class, () -> qrStrategy.login(qrBody(scene), client));
        verify(loginService, never()).buildLoginUser(any()); assertFalse(StpUtil.isLogin());
        assertEquals(QrSceneStore.Phase.CONFIRMED, qrScenes.get(scene.scene()).state().phase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"feature", "app", "page", "env", "allowlist", "secret", "grant"})
    void qrCreationRejectsUnavailableConfigBeforeImageGeneration(String reason) {
        prepareQr(); clearInvocations(qrImages, qrStore);
        switch (reason) {
            case "feature" -> qrProperties.setQrEnabled(false);
            case "app" -> qrApp.setEnabled(false);
            case "page" -> qrApp.setQrPage("/pages/login");
            case "env" -> qrApp.setQrEnvVersion("invalid");
            case "allowlist" -> qrApp.setQrClientIds(List.of());
            case "secret" -> qrApp.setSecret(" ");
            case "grant" -> client.setGrantType("password,qrcode");
            default -> fail();
        }
        assertThrows(ServiceException.class, () -> qr.create(QR_APP, "web-client"));
        verifyNoInteractions(qrImages, qrStore);
    }

    @Test
    void qrImageFailureLeavesNoSceneAndCreationCollisionDoesNotOverwrite() {
        prepareQr(); int size = qrScenes.size(); clearInvocations(qrStore);
        doThrow(new ServiceException("provider failed")).when(qrImages).generate(anyString(), any(), anyString());
        assertThrows(ServiceException.class, () -> qr.create(QR_APP, "web-client"));
        verifyNoInteractions(qrStore); assertEquals(size, qrScenes.size());
        doReturn("image").when(qrImages).generate(anyString(), any(), anyString());
        doReturn(false).when(qrStore).create(anyString(), any());
        assertThrows(ServiceException.class, () -> qr.create(QR_APP, "web-client"));
        assertEquals(size, qrScenes.size());
    }

    @Test
    void qrExpiredOrCompetingCasCannotMintJwtOrReplaceNewState() {
        var scene = prepareQr(); qrConfirm(scene);
        doAnswer(call -> { qrScenes.remove(scene.scene()); return false; }).when(qrStore).transition(anyString(), any(), any());
        assertThrows(ServiceException.class, () -> qrStrategy.login(qrBody(scene), client));
        assertFalse(StpUtil.isLogin()); assertFalse(qrScenes.containsKey(scene.scene()));
    }

    @Test
    void qrSigningFailureNeverReopensConsumedScene() {
        var scene = prepareQr(); qrConfirm(scene);
        try (var helper = mockStatic(LoginHelper.class)) {
            helper.when(() -> LoginHelper.login(any(), any())).thenThrow(new IllegalStateException("session store down"));
            assertThrows(IllegalStateException.class, () -> qrStrategy.login(qrBody(scene), client));
        }
        assertEquals(QrSceneStore.Phase.CONSUMED, qrScenes.get(scene.scene()).state().phase());
        assertThrows(ServiceException.class, () -> qrStrategy.login(qrBody(scene), client));
    }

    private void assertSession(LoginVo view) {
        assertNotNull(view.getAccessToken());
        var session = LoginHelper.getLoginUser(view.getAccessToken());
        assertNotNull(session);
        assertEquals(USER_ID, session.getUserId());
        assertEquals(USER_ID + 6, session.getDeptId());
        assertEquals("Contract Department", session.getDeptName());
        assertEquals(USERNAME, session.getUsername());
        assertEquals(Set.of("contract:read"), session.getMenuPermission());
        assertEquals(Set.of("operator"), session.getRolePermission());
        assertEquals("web", session.getClientKey());
        assertEquals("pc", session.getDeviceType());
        assertEquals("web-client", view.getClientId());
        assertTrue(view.getExpireIn() > 0 && view.getExpireIn() <= 1800);
    }

    private static Map<String, Object> baseBody(String grant) {
        var body = new HashMap<String, Object>();
        body.put("clientId", "web-client");
        body.put("grantType", grant);
        return body;
    }

    private static String passwordBody(String password, String code, String uuid) {
        var body = baseBody("password");
        body.put("username", USERNAME);
        body.put("password", password);
        body.put("code", code);
        body.put("uuid", uuid);
        return JsonUtils.toJsonString(body);
    }

    private static String identifier(String grant) {
        return "sms".equals(grant) ? PHONE : EMAIL;
    }

    private static String codeKey(String grant) {
        return "sms".equals(grant) ? SMS_KEY : GlobalConstants.CAPTCHA_CODE_KEY + EMAIL;
    }
}
