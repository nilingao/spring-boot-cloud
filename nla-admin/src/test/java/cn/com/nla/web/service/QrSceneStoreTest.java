package cn.com.nla.web.service;

import cn.com.nla.common.core.utils.SpringUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 验证实际 Redis 适配器的 codec、JSON 和 CAS 参数；不连接外部 Redis。 */
class QrSceneStoreTest {
    private static GenericApplicationContext context;
    private static Object previousContext;
    private static Object previousFactory;
    private RedissonClient redis;
    private RBucket<String> bucket;
    private RScript script;
    private QrSceneStore store;
    private static final String SCENE = "0123456789abcdef0123456789abcdef";
    private static final String KEY = "auth:qr:scene:" + SCENE;
    private final QrSceneStore.State state = new QrSceneStore.State("wx0123456789abcdef", "web-client", "browser-hash",
        QrSceneStore.Phase.CONFIRMED, 9_007_199_254_740_993L, "verified-openid");

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
    @BeforeEach @SuppressWarnings("unchecked") void prepare() {
        redis = mock(RedissonClient.class); bucket = mock(RBucket.class); script = mock(RScript.class);
        when(redis.<String>getBucket(KEY, StringCodec.INSTANCE)).thenReturn(bucket);
        when(redis.getScript(StringCodec.INSTANCE)).thenReturn(script); store = new QrSceneStore(redis);
    }

    @Test void creationUsesStringCodecAndThreeMinuteNxWithoutReplacingExistingState() {
        when(bucket.setIfAbsent(anyString(), eq(Duration.ofSeconds(180)))).thenReturn(true, false);
        assertTrue(store.create(SCENE, state)); assertFalse(store.create(SCENE, state));
        verify(bucket, times(2)).setIfAbsent(JsonUtils.toJsonString(state), Duration.ofSeconds(180));
        verify(bucket, never()).set(anyString());
    }

    @Test void readsRecordWithExactSnowflakeIdAndRetainsOriginalRawCasValue() {
        String raw = JsonUtils.toJsonString(state); when(bucket.get()).thenReturn(raw);
        var snapshot = store.read(SCENE);
        assertEquals(state, snapshot.state()); assertEquals(raw, snapshot.raw());
        assertEquals(9_007_199_254_740_993L, snapshot.state().userId());
        when(bucket.get()).thenReturn(null); assertNull(store.read(SCENE));
    }

    @Test void transitionPassesExactPriorBytesAndFixedKeyToWriteScript() {
        var next = state.with(QrSceneStore.Phase.CONSUMED, state.userId(), state.openid());
        var snapshot = new QrSceneStore.Snapshot("original stored JSON bytes", state);
        when(script.<Long>eval(RScript.Mode.READ_WRITE, QrSceneStore.TRANSITION, RScript.ReturnType.LONG,
            List.of(KEY), snapshot.raw(), JsonUtils.toJsonString(next))).thenReturn(1L);
        assertTrue(store.transition(SCENE, snapshot, next));
        verify(script).eval(RScript.Mode.READ_WRITE, QrSceneStore.TRANSITION, RScript.ReturnType.LONG,
            List.of(KEY), snapshot.raw(), JsonUtils.toJsonString(next));
        verify(bucket, never()).set(anyString());
    }

    @Test void scriptRejectionOrMissingResultCannotReportSuccess() {
        var snapshot = new QrSceneStore.Snapshot(JsonUtils.toJsonString(state), state);
        when(script.<Long>eval(RScript.Mode.READ_WRITE, QrSceneStore.TRANSITION, RScript.ReturnType.LONG,
            List.of(KEY), snapshot.raw(), JsonUtils.toJsonString(state))).thenReturn(0L, null);
        assertFalse(store.transition(SCENE, snapshot, state)); assertFalse(store.transition(SCENE, snapshot, state));
    }
}
