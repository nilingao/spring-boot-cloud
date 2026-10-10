package cn.com.nla.common.video.core.redis;

import cn.com.nla.common.redis.utils.RedisUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/** One exact protocol topic subscription, released when replaced or destroyed.
 * @author TZY
 */
public abstract class VideoMessageListener implements AutoCloseable {
    private final String topic;
    private Integer listenerId;

    protected VideoMessageListener(String topic) { this.topic = topic; }

    @PostConstruct
    public synchronized void subscribe() {
        if (listenerId == null) {
            listenerId = RedisUtils.subscribeAndGetListenerId(topic, byte[].class, this::onMessage);
        }
    }

    public abstract void onMessage(byte[] payload);

    @Override
    @PreDestroy
    public synchronized void close() {
        if (listenerId != null) {
            RedisUtils.unsubscribe(topic, listenerId);
            listenerId = null;
        }
    }
}
