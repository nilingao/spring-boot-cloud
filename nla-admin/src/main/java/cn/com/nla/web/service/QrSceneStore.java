package cn.com.nla.web.service;

import cn.com.nla.common.json.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/** Redis 原子状态迁移；任何修改保留原剩余 TTL，过期场景不能复活。 */
@Component
@RequiredArgsConstructor
public class QrSceneStore {
    private final RedissonClient redis;
    private static final String PREFIX = "auth:qr:scene:";
    static final String TRANSITION = """
        local value = redis.call('get', KEYS[1])
        if value ~= ARGV[1] then return 0 end
        local ttl = redis.call('pttl', KEYS[1])
        if ttl <= 0 then return 0 end
        redis.call('psetex', KEYS[1], ttl, ARGV[2])
        return 1
        """;

    public boolean create(String scene, State state) {
        return redis.<String>getBucket(PREFIX + scene, StringCodec.INSTANCE)
            .setIfAbsent(JsonUtils.toJsonString(state), Duration.ofSeconds(180));
    }

    public Snapshot read(String scene) {
        String raw = redis.<String>getBucket(PREFIX + scene, StringCodec.INSTANCE).get();
        return raw == null ? null : new Snapshot(raw, JsonUtils.parseObject(raw, State.class));
    }

    public boolean transition(String scene, Snapshot expected, State next) {
        Long result = redis.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, TRANSITION,
            RScript.ReturnType.LONG, List.of(PREFIX + scene), expected.raw(), JsonUtils.toJsonString(next));
        return Long.valueOf(1).equals(result);
    }

    public enum Phase { WAITING, SCANNED, CONFIRMED, CANCELLED, CONSUMED }

    public record State(String appid, String clientId, String browserHash, Phase phase,
                        Long userId, String openid) {
        public State with(Phase next, Long owner, String identity) {
            return new State(appid, clientId, browserHash, next, owner, identity);
        }
    }
    public record Snapshot(String raw, State state) { }
}
