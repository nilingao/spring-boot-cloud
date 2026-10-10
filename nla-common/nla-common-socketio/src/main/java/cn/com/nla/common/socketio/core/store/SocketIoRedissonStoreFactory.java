package cn.com.nla.common.socketio.core.store;

import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.store.Store;
import com.corundumstudio.socketio.store.pubsub.BaseStoreFactory;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.util.Map;
import java.util.UUID;

/** Redisson 实时 Hash 存储，服务前缀隔离，连接关闭删除会话数据。 @author TZY */
public class SocketIoRedissonStoreFactory extends BaseStoreFactory {
    private final RedissonClient client;
    private final String prefix;
    private final Codec codec;
    private final Long nodeId = UUID.randomUUID().getMostSignificantBits();
    private final SocketIoRedissonPubSubStore pubsub;

    public SocketIoRedissonStoreFactory(RedissonClient client, String prefix) {
        this(client, prefix, new SocketIoRedisCodec());
    }

    public SocketIoRedissonStoreFactory(RedissonClient client, String prefix, Codec codec) {
        this.client = client;
        this.prefix = prefix;
        this.codec = codec;
        this.pubsub = new SocketIoRedissonPubSubStore(client, prefix, codec, nodeId);
    }

    @Override protected Long getNodeId() { return nodeId; }
    @Override public PubSubStore pubSubStore() { return pubsub; }
    @Override public <K, V> Map<K, V> createMap(String name) { return client.getMap(prefix + ":map:" + name, codec); }

    @Override
    public Store createStore(UUID sessionId) {
        RMap<String, Object> map = client.getMap(sessionKey(sessionId), codec);
        return new Store() {
            @Override public void set(String key, Object value) { map.put(key, value); }
            @Override @SuppressWarnings("unchecked") public <T> T get(String key) { return (T) map.get(key); }
            @Override public boolean has(String key) { return map.containsKey(key); }
            @Override public void del(String key) { map.remove(key); }
        };
    }

    @Override public void onDisconnect(ClientHead clientHead) { client.getMap(sessionKey(clientHead.getSessionId()), codec).delete(); }
    @Override public void shutdown() { pubsub.shutdown(); }
    private String sessionKey(UUID sessionId) { return prefix + ":session:" + sessionId; }
}
