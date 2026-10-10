package cn.com.nla.common.video.core.redis.subscribe.sip.message;

import cn.com.nla.common.video.core.redis.VideoMessageListener;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SipSubscribeHandle {

    private final static String VIDEO_EVENT_SUBSCRIBE_MANAGER = VideoConstant.VIDEO_SIP_EVENT_SUBSCRIBE_MANAGER;

    public final static String VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER = VideoConstant.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER;

    public final static String VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER = VideoConstant.VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER;
    private final Integer millis = 15;


    private final Map<String, VideoMessageListener> errorMessageListener = new ConcurrentHashMap<>();

    private final Map<String, VideoMessageListener> okMessageListener = new ConcurrentHashMap<>();

    private final Map<String, Instant> okTimeSubscribes = new ConcurrentHashMap<>();

    private final Map<String, Instant> errorTimeSubscribes = new ConcurrentHashMap<>();

    private final Map<String, List<SipSubscribeEvent>> errorSubscribes = new ConcurrentHashMap<>();

    private final Map<String, List<SipSubscribeEvent>> okSubscribes = new ConcurrentHashMap<>();



    public SipSubscribeHandle(DynamicTask dynamicTask){
        dynamicTask.startCron(VIDEO_EVENT_SUBSCRIBE_MANAGER,millis, this::execute);
    }

    public synchronized void execute(){
        Instant instant = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(millis));
        for (String key : okTimeSubscribes.keySet()) {
            if (okTimeSubscribes.get(key).isBefore(instant)){
                removeOkSubscribe(key);
            }
        }
        for (String key : errorTimeSubscribes.keySet()) {
            if (errorTimeSubscribes.get(key).isBefore(instant)){
                removeErrorSubscribe(key);
            }
        }
        log.info("[定时任务] 清理过期的SIP订阅信息, okSubscribes : {},okTimeSubscribes : {},errorSubscribes : {},errorTimeSubscribes : {}",okSubscribes.size(),okTimeSubscribes.size(),errorSubscribes.size(),errorTimeSubscribes.size());
    }


    public synchronized void addErrorSubscribe(String key, SipSubscribeEvent event) {
        errorTimeSubscribes.put(key, Instant.now());
        errorSubscribes.computeIfAbsent(key, o -> new ArrayList<SipSubscribeEvent>()).add(event);
        String redisListenerKey = String.format("%s%s", VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, key);
        //创建监听
        VideoMessageListener abstractMessageListener = errorMessageListener.computeIfAbsent(key, o -> new VideoMessageListener(redisListenerKey) {
            @Override
            public void onMessage(byte[] payload) {
                List<SipSubscribeEvent> errprSubscribe = getErrorSubscribe(key);
                if(errprSubscribe == null || errprSubscribe.isEmpty()){
                    //移除
                    removeAllSubscribe(key);
                    return;
                }
                Object body = org.springframework.util.SerializationUtils.deserialize(payload);
                Object deserialize = body;
                EventResult result = deserialize instanceof EventResult existing ? existing : new EventResult((EventObject) deserialize);
                try {
                    for (SipSubscribeEvent sipSubscribeEvent : new ArrayList<>(errprSubscribe)) {
                        sipSubscribeEvent.response(result);
                    }
                } finally { removeAllSubscribe(key); }
            }
        });
        abstractMessageListener.subscribe();
    }

    public synchronized void addOkSubscribe(String key, SipSubscribeEvent event) {
        okTimeSubscribes.put(key, Instant.now());
        okSubscribes.computeIfAbsent(key, o -> new ArrayList<SipSubscribeEvent>()).add(event);
        String redisListenerKey = String.format("%s%s", VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER, key);
        //创建监听
        VideoMessageListener abstractMessageListener = okMessageListener.computeIfAbsent(key, o -> new VideoMessageListener(redisListenerKey) {
            @Override
            public void onMessage(byte[] payload) {

                List<SipSubscribeEvent> okSubscribe = getOkSubscribe(key);
                if(okSubscribe == null || okSubscribe.isEmpty()){
                    //移除
                    removeAllSubscribe(key);
                    return;
                }
                Object body = org.springframework.util.SerializationUtils.deserialize(payload);
                Object deserialize = body;
                EventResult result = deserialize instanceof EventResult existing ? existing : new EventResult((EventObject) deserialize);
                try {
                    for (SipSubscribeEvent sipSubscribeEvent : new ArrayList<>(okSubscribe)) {
                        sipSubscribeEvent.response(result);
                    }
                } finally { removeAllSubscribe(key); }
            }
        });
        abstractMessageListener.subscribe();
    }

    public List<SipSubscribeEvent> getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public  void removeAllSubscribe(String key){
        removeErrorSubscribe(key);
        removeOkSubscribe(key);
    }

    public synchronized void removeErrorSubscribe(String key) {
        if(key == null){
            return;
        }
        errorSubscribes.remove(key);
        errorTimeSubscribes.remove(key);
        VideoMessageListener abstractMessageListener = errorMessageListener.remove(key);
        if(abstractMessageListener != null){
            abstractMessageListener.close();
        }
    }

    public List<SipSubscribeEvent> getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public synchronized void removeOkSubscribe(String key) {
        if(key == null){
            return;
        }
        okSubscribes.remove(key);
        okTimeSubscribes.remove(key);
        VideoMessageListener abstractMessageListener = okMessageListener.remove(key);
        if(abstractMessageListener != null){
            abstractMessageListener.close();
        }
    }
    public int getErrorSubscribesSize(){
        return errorSubscribes.size();
    }
    public int getOkSubscribesSize(){
        return okSubscribes.size();
    }
    @jakarta.annotation.PreDestroy
    public synchronized void close() {
        errorMessageListener.values().forEach(VideoMessageListener::close);
        okMessageListener.values().forEach(VideoMessageListener::close);
        errorMessageListener.clear(); okMessageListener.clear();
        errorSubscribes.clear(); okSubscribes.clear();
        errorTimeSubscribes.clear(); okTimeSubscribes.clear();
    }
}
