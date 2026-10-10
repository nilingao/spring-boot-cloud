package cn.com.nla.web.service;

import cn.com.nla.common.core.constant.CacheNames;
import cn.com.nla.common.core.constant.Constants;
import cn.com.nla.common.core.constant.GlobalConstants;
import cn.com.nla.common.core.enums.LoginType;
import cn.com.nla.common.core.exception.user.CaptchaException;
import cn.com.nla.common.core.exception.user.CaptchaExpireException;
import cn.com.nla.common.core.exception.user.UserException;
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
