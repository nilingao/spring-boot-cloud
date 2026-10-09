package cn.com.nla.common.freeswitch.redis.subscribe.sip.message;

import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.vo.sip.EventResult;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SipSubscribeHandle {

    private final static String VIDEO_EVENT_SUBSCRIBE_MANAGER = SipConstant.VIDEO_SIP_EVENT_SUBSCRIBE_MANAGER;

    public final static String VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER = SipConstant.VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER;

    public final static String VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER = SipConstant.VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER;
    private final Integer millis = 15;

    private final Map<String, Integer> errorMessageListener = new ConcurrentHashMap<>();

    private final Map<String, Integer> okMessageListener = new ConcurrentHashMap<>();

    private final Map<String, Instant> okTimeSubscribes = new ConcurrentHashMap<>();

    private final Map<String, Instant> errorTimeSubscribes = new ConcurrentHashMap<>();

    private final Map<String, List<SipSubscribeEvent>> errorSubscribes = new ConcurrentHashMap<>();

    private final Map<String, List<SipSubscribeEvent>> okSubscribes = new ConcurrentHashMap<>();



    public SipSubscribeHandle(DynamicTask dynamicTask){
        dynamicTask.startCron(VIDEO_EVENT_SUBSCRIBE_MANAGER,millis, this::execute);
    }

    public void execute(){
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


    public void addErrorSubscribe(String key, SipSubscribeEvent event) {
        errorTimeSubscribes.put(key, Instant.now());
        errorSubscribes.computeIfAbsent(key, o -> new ArrayList<SipSubscribeEvent>()).add(event);
        String redisListenerKey = String.format("%s%s", VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, key);
        //创建监听
        errorMessageListener.computeIfAbsent(key, o -> RedisUtils.subscribeAndGetListenerId(redisListenerKey, byte[].class, body -> {
            List<SipSubscribeEvent> errprSubscribe = getErrorSubscribe(key);
            if(errprSubscribe == null || errprSubscribe.isEmpty()){
                //移除
                removeAllSubscribe(key);
                return;
            }
            EventObject eventObject = (EventObject) SerializationUtils.deserialize(body);
            for (SipSubscribeEvent sipSubscribeEvent : errprSubscribe) {
                sipSubscribeEvent.response(new EventResult(eventObject));
            }
            //移除
            removeAllSubscribe(key);
        }));
    }

    public void addOkSubscribe(String key, SipSubscribeEvent event) {
        okTimeSubscribes.put(key, Instant.now());
        okSubscribes.computeIfAbsent(key, o -> new ArrayList<SipSubscribeEvent>()).add(event);
        String redisListenerKey = String.format("%s%s", VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER, key);
        //创建监听
        okMessageListener.computeIfAbsent(key, o -> RedisUtils.subscribeAndGetListenerId(redisListenerKey, byte[].class, body -> {
            List<SipSubscribeEvent> okSubscribe = getOkSubscribe(key);
            if(okSubscribe == null || okSubscribe.isEmpty()){
                //移除
                removeAllSubscribe(key);
                return;
            }
            EventObject eventObject = (EventObject) SerializationUtils.deserialize(body);
            for (SipSubscribeEvent sipSubscribeEvent : okSubscribe) {
                sipSubscribeEvent.response(new EventResult(eventObject));
            }
            //移除
            removeAllSubscribe(key);
        }));
    }

    public List<SipSubscribeEvent> getErrorSubscribe(String key) {
        return errorSubscribes.get(key);
    }

    public  void removeAllSubscribe(String key){
        removeErrorSubscribe(key);
        removeOkSubscribe(key);
    }

    public void removeErrorSubscribe(String key) {
        if(key == null){
            return;
        }
        errorSubscribes.remove(key);
        errorTimeSubscribes.remove(key);
        Integer listenerId = errorMessageListener.remove(key);
        if(listenerId != null){
            RedisUtils.unsubscribe(String.format("%s%s", VIDEO_SIP_ERROR_EVENT_SUBSCRIBE_MANAGER, key), listenerId);
        }
    }

    public List<SipSubscribeEvent> getOkSubscribe(String key) {
        return okSubscribes.get(key);
    }

    public void removeOkSubscribe(String key) {
        if(key == null){
            return;
        }
        okSubscribes.remove(key);
        okTimeSubscribes.remove(key);
        Integer listenerId = okMessageListener.remove(key);
        if(listenerId != null){
            RedisUtils.unsubscribe(String.format("%s%s", VIDEO_SIP_OK_EVENT_SUBSCRIBE_MANAGER, key), listenerId);
        }
    }
    public int getErrorSubscribesSize(){
        return errorSubscribes.size();
    }
    public int getOkSubscribesSize(){
        return okSubscribes.size();
    }
}
