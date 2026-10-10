package cn.com.nla.common.video.core.redis.subscribe.media;

import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.HookType;
import cn.com.nla.common.video.basic.vo.media.HookKey;
import cn.com.nla.common.video.core.demo.MediaHookVo;
import cn.com.nla.common.video.core.redis.VideoMessageListener;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.hutool.core.bean.BeanUtil;
import jakarta.annotation.PreDestroy;
import org.springframework.util.SerializationUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Shares hook events by type, then filters against each subscription's fields.
 * @author TZY
 */
public class MediaHookSubscribe implements AutoCloseable {
    private final Map<HookType, Map<HookKey, HookEvent>> subscribes = new ConcurrentHashMap<>();
    private final Map<HookType, VideoMessageListener> listeners = new ConcurrentHashMap<>();
    private final DynamicTask tasks;
    public MediaHookSubscribe(DynamicTask tasks) {
        this.tasks = tasks;
        tasks.startCron(VideoConstant.MEDIA_HOOK_SUBSCRIBE_MANAGER, 20, this::execute);
    }
    private String topic(HookType type) { return VideoConstant.MEDIA_HOOK_SUBSCRIBE_MANAGER + ":" + type.getCode(); }
    public void sendNotify(MediaHookVo event) { RedisUtils.publish(topic(event.getType()), SerializationUtils.serialize(event)); }
    public synchronized void addSubscribe(HookKey key, HookEvent event) {
        subscribes.computeIfAbsent(key.getHookType(), ignored -> new ConcurrentHashMap<>()).put(key, event);
        listeners.computeIfAbsent(key.getHookType(), type -> {
            VideoMessageListener listener = new VideoMessageListener(topic(type)) {
                @Override public void onMessage(byte[] payload) {
                    MediaHookVo event = (MediaHookVo) SerializationUtils.deserialize(payload);
                    if (event == null || event.getType() != type) return;
                    Map<HookKey, HookEvent> callbacks = subscribes.get(type);
                    if (callbacks == null) return;
                    Map<String, Object> fields = BeanUtil.beanToMap(event.getHookVo());
                    for (var entry : new ArrayList<>(callbacks.entrySet())) {
                        if (event.getOnAll() == 1 || matches(entry.getKey(), fields))
                            entry.getValue().response(event.getMediaServerVo(), event.getHookVo());
                    }
                }
            };
            listener.subscribe();
            return listener;
        });
    }
    public synchronized void removeSubscribe(HookKey key) {
        Map<HookKey, HookEvent> callbacks = subscribes.get(key.getHookType());
        if (callbacks == null) return;
        callbacks.remove(key);
        if (callbacks.isEmpty()) {
            subscribes.remove(key.getHookType());
            VideoMessageListener listener = listeners.remove(key.getHookType());
            if (listener != null) listener.close();
        }
    }
    public HookKey getHookKey(HookKey key) {
        Map<HookKey, HookEvent> callbacks = subscribes.get(key.getHookType());
        if (callbacks == null) return null;
        return callbacks.keySet().stream().filter(key::equals).findFirst().orElse(null);
    }
    public List<HookEvent> getSubscribes(HookType type) {
        Map<HookKey, HookEvent> callbacks = subscribes.get(type);
        return callbacks == null ? null : new ArrayList<>(callbacks.values());
    }
    private boolean matches(HookKey key, Map<String, Object> fields) {
        if (key.getContent() == null) return true;
        return key.getContent().entrySet().stream().allMatch(entry -> entry.getValue() == null ||
            fields != null && Objects.equals(entry.getValue(), fields.get(entry.getKey())));
    }
    public void execute() {
        Date now = new Date();
        for (var callbacks : subscribes.values())
            for (HookKey key : new ArrayList<>(callbacks.keySet()))
                if (key.getExpires() != null && now.after(key.getExpires())) removeSubscribe(key);
    }
    @Override @PreDestroy
    public synchronized void close() {
        tasks.stop(VideoConstant.MEDIA_HOOK_SUBSCRIBE_MANAGER);
        listeners.values().forEach(VideoMessageListener::close);
        listeners.clear(); subscribes.clear();
    }
}
