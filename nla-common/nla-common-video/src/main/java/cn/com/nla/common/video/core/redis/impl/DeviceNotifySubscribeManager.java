package cn.com.nla.common.video.core.redis.impl;


import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.demo.DeviceNotifyVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Slf4j
public class DeviceNotifySubscribeManager {
    /**
     * 设备订阅缓存key
     */
    public static final String VIDEO_DEVICE_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_DEVICE_NOTIFY_SUBSCRIBE;

    /**
     * 设备报警订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_ALARM_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_DEVICE_ALARM_NOTIFY_SUBSCRIBE;
    /**
     * 设备目录订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_CATALOG_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_DEVICE_CATALOG_NOTIFY_SUBSCRIBE;
    /**
     * 设备移动位置订阅缓存key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_MOBILE_POSITION_NOTIFY_SUBSCRIBE = VideoConstant.VIDEO_DEVICE_MOBILE_POSITION_NOTIFY_SUBSCRIBE;

    public boolean getAlarmSubscribe(String gbId){
        String key = String.format("%s%s",VIDEO_DEVICE_ALARM_NOTIFY_SUBSCRIBE, gbId);
        return ObjectUtils.isNotEmpty(RedisUtils.getCacheObject(key));
    }

    public boolean getCatalogSubscribe(String gbId){
        String key = String.format("%s%s",VIDEO_DEVICE_CATALOG_NOTIFY_SUBSCRIBE, gbId);
        return ObjectUtils.isNotEmpty(RedisUtils.getCacheObject(key));
    }

    public boolean getMobilePositionSubscribe(String gbId){
        String key = String.format("%s%s",VIDEO_DEVICE_MOBILE_POSITION_NOTIFY_SUBSCRIBE, gbId);
        return ObjectUtils.isNotEmpty(RedisUtils.getCacheObject(key));
    }

    public boolean addAlarmSubscribe(DeviceVo deviceVo,String msg){
        if (deviceVo == null || deviceVo.getSubscribeCycleForAlarm() < 0) {
            return false;
        }
        log.info("[添加报警订阅] {} 设备{}",msg, deviceVo.getDeviceId());
        DeviceNotifyVo build = DeviceNotifyVo.builder().type(DeviceNotifyVo.TypeEnum.ALARM.getValue()).operate(DeviceNotifyVo.OperateEnum.ADD.getValue()).gbId(deviceVo.getDeviceId()).build();
        RedisUtils.publish(VIDEO_DEVICE_NOTIFY_SUBSCRIBE, org.springframework.util.SerializationUtils.serialize(build));
        return true;
    }

    public boolean removeAlarmSubscribe(DeviceVo deviceVo) {
        if (deviceVo == null || deviceVo.getSubscribeCycleForAlarm() < 0) {
            return false;
        }
        log.info("[移除报警订阅]: {}", deviceVo.getDeviceId());
        DeviceNotifyVo build = DeviceNotifyVo.builder().type(DeviceNotifyVo.TypeEnum.ALARM.getValue()).operate(DeviceNotifyVo.OperateEnum.DEL.getValue()).gbId(deviceVo.getDeviceId()).build();
        RedisUtils.publish(VIDEO_DEVICE_NOTIFY_SUBSCRIBE, org.springframework.util.SerializationUtils.serialize(build));
        return true;
    }


    public boolean addCatalogSubscribe(DeviceVo deviceVo,String msg){
        if (deviceVo == null || deviceVo.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        log.info("[添加目录订阅] {} 设备{}",msg, deviceVo.getDeviceId());
        DeviceNotifyVo build = DeviceNotifyVo.builder().type(DeviceNotifyVo.TypeEnum.CATALOG.getValue()).operate(DeviceNotifyVo.OperateEnum.ADD.getValue()).gbId(deviceVo.getDeviceId()).build();
        RedisUtils.publish(VIDEO_DEVICE_NOTIFY_SUBSCRIBE, org.springframework.util.SerializationUtils.serialize(build));
        return true;
    }

    public boolean removeCatalogSubscribe(DeviceVo deviceVo) {
        if (deviceVo == null || deviceVo.getSubscribeCycleForCatalog() < 0) {
            return false;
        }
        log.info("[移除目录订阅]: {}", deviceVo.getDeviceId());
        DeviceNotifyVo build = DeviceNotifyVo.builder().type(DeviceNotifyVo.TypeEnum.CATALOG.getValue()).operate(DeviceNotifyVo.OperateEnum.DEL.getValue()).gbId(deviceVo.getDeviceId()).build();
        RedisUtils.publish(VIDEO_DEVICE_NOTIFY_SUBSCRIBE, org.springframework.util.SerializationUtils.serialize(build));
        return true;
    }
    public boolean addMobilePositionSubscribe(DeviceVo deviceVo,String msg) {
        if (deviceVo == null || deviceVo.getSubscribeCycleForMobilePosition() < 0) {
            return false;
        }
        log.info("[添加移动位置订阅] {} 设备{}",msg, deviceVo.getDeviceId());
        DeviceNotifyVo build = DeviceNotifyVo.builder().type(DeviceNotifyVo.TypeEnum.MOBILE_POSITION.getValue()).operate(DeviceNotifyVo.OperateEnum.ADD.getValue()).gbId(deviceVo.getDeviceId()).build();
        RedisUtils.publish(VIDEO_DEVICE_NOTIFY_SUBSCRIBE, org.springframework.util.SerializationUtils.serialize(build));
        return true;
    }

    public boolean removeMobilePositionSubscribe(DeviceVo deviceVo) {
        if (deviceVo == null || deviceVo.getSubscribeCycleForMobilePosition() < 0) {
            return false;
        }
        log.info("[移除移动位置订阅]: {}", deviceVo.getDeviceId());
        DeviceNotifyVo build = DeviceNotifyVo.builder().type(DeviceNotifyVo.TypeEnum.MOBILE_POSITION.getValue()).operate(DeviceNotifyVo.OperateEnum.DEL.getValue()).gbId(deviceVo.getDeviceId()).build();
        RedisUtils.publish(VIDEO_DEVICE_NOTIFY_SUBSCRIBE, org.springframework.util.SerializationUtils.serialize(build));
        return true;
    }
}
