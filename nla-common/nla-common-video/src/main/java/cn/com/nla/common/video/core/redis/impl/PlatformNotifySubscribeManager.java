package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.core.demo.NotifySubscribeInfo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.hutool.core.codec.Base64;
import org.springframework.util.SerializationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 上级平台订阅缓存
 * redis 订阅方式
 */
public class PlatformNotifySubscribeManager {
    //报警订阅缓存key
    private static final String VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE;
    //目录订阅缓存key
    private static final String VIDEO_CATALOG_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_PLATFORM_CATALOG_NOTIFY_SUBSCRIBE;
    //目录订阅缓存key
    private static final String VIDEO_MOBILE_POSITION_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_PLATFORM_MOBILE_POSITION_NOTIFY_SUBSCRIBE;
    private DynamicTask dynamicTask;

    public PlatformNotifySubscribeManager(DynamicTask dynamicTask){
        this.dynamicTask= dynamicTask;
    }

    public NotifySubscribeInfo getAlarmSubscribe(String platformId) {
        String key = String.format("%s%s", VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE, platformId);
        return (NotifySubscribeInfo)SerializationUtils.deserialize((byte[]) RedisUtils.getCacheObject(key));
    }
    public  NotifySubscribeInfo getCatalogSubscribe(String platformId){
        String key = String.format("%s%s", VIDEO_CATALOG_NOTIFY_SUBSCRIBE, platformId);
        Object o = RedisUtils.getCacheObject(key);
        return (NotifySubscribeInfo)SerializationUtils.deserialize((byte[]) o);
    }
    public NotifySubscribeInfo getMobilePositionSubscribe(String platformId) {
        String key = String.format("%s%s", VIDEO_MOBILE_POSITION_NOTIFY_SUBSCRIBE, platformId);
        return (NotifySubscribeInfo)SerializationUtils.deserialize((byte[]) RedisUtils.getCacheObject(key));
    }
    public void putAlarmSubscribe(String platformId, NotifySubscribeInfo info) {
        String key = String.format("%s%s", VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE, platformId);
        //缓存订阅信息并 添加缓存时间
        VideoCache.set(key,SerializationUtils.serialize(info),info.getExpires());
    }
    public void removeAlarmSubscribe(String platformId){
        String key = String.format("%s%s", VIDEO_PLATFORM_ALARM_NOTIFY_SUBSCRIBE, platformId);
        //删除订阅信息
        RedisUtils.deleteObject(key);
    }

    public void putCatalogSubscribe(String platformId, NotifySubscribeInfo info) {
        String key = String.format("%s%s", VIDEO_CATALOG_NOTIFY_SUBSCRIBE, platformId);
        //缓存订阅信息并 添加缓存时间
        VideoCache.set(key,SerializationUtils.serialize(info),info.getExpires());
    }
    public void removeCatalogSubscribe(String platformId){
        String key = String.format("%s%s", VIDEO_CATALOG_NOTIFY_SUBSCRIBE, platformId);
        //删除订阅信息
        RedisUtils.deleteObject(key);
    }
    public void putMobilePositionSubscribe(String platformId, NotifySubscribeInfo info) {
        String key = String.format("%s%s", VIDEO_MOBILE_POSITION_NOTIFY_SUBSCRIBE, platformId);
        //缓存订阅信息并 添加缓存时间
        VideoCache.set(key, SerializationUtils.serialize(info),info.getExpires());
        //添加任务处理GPS推送
        dynamicTask.startCron(key,info.getGpsInterval(),()->{
            VideoService.getDeviceMobilePositionService().sendNotifyMobilePosition(platformId,info);
        });
    }
    public void removeMobilePositionSubscribe(String platformId){
        String key = String.format("%s%s", VIDEO_MOBILE_POSITION_NOTIFY_SUBSCRIBE, platformId);
        //删除订阅信息
        RedisUtils.deleteObject(key);
        //结束任务处理GPS推送
        dynamicTask.stop(key);
    }

    public List<String> getAllCatalogSubscribePlatform() {
        String key = String.format("%s*", VIDEO_CATALOG_NOTIFY_SUBSCRIBE);
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        List<String>platformList = new ArrayList<>();
        for (String keys : scan) {
            String[] split = keys.split(":");
            platformList.add(split[1]);
        }
        return platformList;
    }

    public void removeAllSubscribe(String platformId) {
        removeMobilePositionSubscribe(platformId);
        removeCatalogSubscribe(platformId);
        removeAlarmSubscribe(platformId);
    }

}
