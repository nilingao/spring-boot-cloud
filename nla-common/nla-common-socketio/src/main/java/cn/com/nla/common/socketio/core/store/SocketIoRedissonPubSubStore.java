package cn.com.nla.common.socketio.core.store;

import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** 每个工厂只移除自己拥有的订阅；禁止关闭外部共享的 RedissonClient。 @author TZY */
public class SocketIoRedissonPubSubStore implements PubSubStore {
    private final RedissonClient client;
    private final String prefix;
    private final Codec codec;
    private final Long nodeId;
    private final EnumMap<PubSubType, List<Subscription>> subscriptions = new EnumMap<>(PubSubType.class);
    private volatile boolean closed;

    public SocketIoRedissonPubSubStore(RedissonClient client, String prefix, Codec codec, Long nodeId) {
        this.client = client;
        this.prefix = prefix;
        this.codec = codec;
        this.nodeId = nodeId;
    }

    @Override
    public void publish(PubSubType type, PubSubMessage message) {
        synchronized (this) {
            if (closed) { throw new IllegalStateException("Socket.IO pubsub store is closed"); }
        }
        message.setNodeId(nodeId);
        client.getTopic(prefix + ":topic:" + type, codec).publish(message);
    }

    @Override
    public synchronized <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
        if (closed) { throw new IllegalStateException("Socket.IO pubsub store is closed"); }
        RTopic topic = client.getTopic(prefix + ":topic:" + type, codec);
        int id = topic.addListener(clazz, (channel, message) -> {
            if (closed) { return; }
            if (!nodeId.equals(message.getNodeId())) { listener.onMessage(message); }
        });
        subscriptions.computeIfAbsent(type, key -> new ArrayList<>()).add(new Subscription(topic, id));
    }

    @Override
    public synchronized void unsubscribe(PubSubType type) {
        List<Subscription> owned = subscriptions.remove(type);
        if (owned != null) { owned.forEach(subscription -> subscription.topic().removeListener(subscription.id())); }
    }

    @Override
    public synchronized void shutdown() {
        if (closed) { return; }
        closed = true;
        for (PubSubType type : PubSubType.values()) { unsubscribe(type); }
    }

    private record Subscription(RTopic topic, int id) { }
}
