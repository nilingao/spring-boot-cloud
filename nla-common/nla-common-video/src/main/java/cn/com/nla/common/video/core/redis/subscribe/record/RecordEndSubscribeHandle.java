package cn.com.nla.common.video.core.redis.subscribe.record;

import cn.com.nla.common.video.core.redis.VideoMessageListener;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.sip.RecordInfo;
import cn.com.nla.common.video.core.utils.DynamicTask;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 录像查询结束后回调事件
 * redis 订阅方式
 */
@Slf4j
public class RecordEndSubscribeHandle {


    private final String VIDEO_RECORD_END_SUBSCRIBE_MANAGER = VideoConstant.VIDEO_RECORD_END_SUBSCRIBE_MANAGER;

    private final Integer millis = 15;
    private final Map<String, Instant> handlerMapSubscribes = new ConcurrentHashMap<>();
    private final Map<String, RecordEndSubscribeEvent> handlerMap = new ConcurrentHashMap<>();

    private final Map<String, VideoMessageListener> handlerListenerMap = new ConcurrentHashMap<>();

    public RecordEndSubscribeHandle(DynamicTask dynamicTask){
        dynamicTask.startCron(VIDEO_RECORD_END_SUBSCRIBE_MANAGER,millis, this::execute);
    }

    public synchronized void execute(){
        Instant instant = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(millis));
        for (String key : handlerMap.keySet()) {
            if (handlerMapSubscribes.get(key).isBefore(instant)){
                delEndEventHandler(key);
            }
        }
        log.info("[定时任务] 清理过期 录像查询结束后回调事件, handlerMap : {},handlerMapSubscribes : {}",handlerMap.size(),handlerMapSubscribes.size());
    }
    public void handlerEvent(RecordInfo recordInfo) {
        String key = String.format("%s%s:%s", VIDEO_RECORD_END_SUBSCRIBE_MANAGER, recordInfo.getDeviceId(), recordInfo.getChannelId());
        log.info("录像查询完成事件触发，deviceId：{}, channelId: {}, 录像数量{}/{}条", recordInfo.getDeviceId(), recordInfo.getChannelId(), recordInfo.getCount(),recordInfo.getSumNum());
        RedisUtils.publish(key, org.springframework.util.SerializationUtils.serialize(recordInfo));
    }

    /**
     * 添加
     * @param device
     * @param channelId
     * @param recordEndSubscribeEvent
     */
    public synchronized void addEndEventHandler(String device, String channelId, RecordEndSubscribeEvent recordEndSubscribeEvent) {
        String key = String.format("%s%s:%s", VIDEO_RECORD_END_SUBSCRIBE_MANAGER, device, channelId);
        delEndEventHandler(key);
        handlerMapSubscribes.put(key, Instant.now());
        handlerMap.put(key, recordEndSubscribeEvent);
        //创建监听
        VideoMessageListener abstractMessageListener = new VideoMessageListener(key) {
            @Override
            public void onMessage(byte[] payload) {
                RecordEndSubscribeEvent endSubscribeEvent = handlerMap.get(key);
                if (endSubscribeEvent != null) {
                    Object body = org.springframework.util.SerializationUtils.deserialize(payload);
                    RecordInfo recordInfo = (RecordInfo) body;
                    try {
                        endSubscribeEvent.handler(recordInfo);
                    } finally {
                        if (recordInfo.getCount() >= recordInfo.getSumNum()) delEndEventHandler(key);
                    }
                }
            }
        };
        abstractMessageListener.subscribe();
        handlerListenerMap.put(key,abstractMessageListener);
    }

    private synchronized void delEndEventHandler(String key){
        handlerMapSubscribes.remove(key);
        handlerMap.remove(key);
        VideoMessageListener abstractMessageListener = handlerListenerMap.remove(key);
        if(abstractMessageListener != null){
            abstractMessageListener.close();
        }
    }
    @jakarta.annotation.PreDestroy
    public synchronized void close() {
        handlerListenerMap.values().forEach(VideoMessageListener::close);
        handlerListenerMap.clear(); handlerMap.clear(); handlerMapSubscribes.clear();
    }

}
