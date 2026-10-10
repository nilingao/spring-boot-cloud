package cn.com.nla.common.video.core.redis;

import cn.com.nla.common.redis.utils.RedisUtils;
import java.time.Duration;
import java.util.*;

/** Redis structures used by SIP sequences, SSRC allocation and ZLM load balancing.
 * TTL arguments are seconds; nonpositive values preserve the legacy persistent-key contract.
 * @author TZY
 */
public final class VideoCache {
    private VideoCache() { }
    public static void set(String key, Object value) { RedisUtils.setCacheObject(key, value); }
    public static void set(String key, Object value, long seconds) {
        if (seconds > 0) RedisUtils.setCacheObject(key, value, Duration.ofSeconds(seconds));
        else RedisUtils.setCacheObject(key, value);
    }
    public static long ttlSeconds(String key) {
        long ttl = RedisUtils.getTimeToLive(key);
        return ttl < 0 ? ttl : Math.max(1, (ttl + 999) / 1000);
    }
    public static long increment(String key, long delta, long seconds) {
        var counter = RedisUtils.getClient().getAtomicLong(key);
        long value = counter.addAndGet(delta);
        if (seconds > 0) counter.expire(Duration.ofSeconds(seconds));
        return value;
    }
    public static <T> Map<String, T> readMap(String key) { return RedisUtils.getCacheMap(key); }
    public static void putMap(String key, Map<String, ?> values, long seconds) {
        var map = RedisUtils.getClient().getMap(key);
        map.putAll(values);
        if (seconds > 0) map.expire(Duration.ofSeconds(seconds));
    }
    public static void addSet(String key, Object... values) { RedisUtils.getClient().getSet(key).addAll(Arrays.asList(values)); }
    public static Object popSet(String key) { return RedisUtils.getClient().getSet(key).removeRandom(); }
    public static boolean containsSet(String key, Object value) { return RedisUtils.getClient().getSet(key).contains(value); }
    public static long setSize(String key) { return RedisUtils.getClient().getSet(key).size(); }
    public static Set<Object> readSet(String key) { return RedisUtils.getClient().getSet(key).readAll(); }
    public static void removeSet(String key, Object value) { RedisUtils.getClient().getSet(key).remove(value); }
    public static void score(String key, Object value, double score) { RedisUtils.getClient().getScoredSortedSet(key).add(score, value); }
    public static Double getScore(String key, Object value) { return RedisUtils.getClient().getScoredSortedSet(key).getScore(value); }
    public static long scoreSize(String key) { return RedisUtils.getClient().getScoredSortedSet(key).size(); }
    public static Set<Object> scoreRange(String key, int start, int end) { return new LinkedHashSet<>(RedisUtils.getClient().getScoredSortedSet(key).valueRange(start, end)); }
    public static void addScore(String key, Object value, double delta) { RedisUtils.getClient().getScoredSortedSet(key).addScore(value, delta); }
}
