package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.core.properties.VideoProperties;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

/**
 * 上级 或 设备 注册到服务相关缓存
 * (用于通过注册的上级或设备国标编号去找注册到那个服务)
 */
public class RegisterServerManager {
    @Autowired
    private VideoProperties videoProperties;

    /**
     * 平台自身注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_SIP_CACHE_SERVER = VideoConstant.VIDEO_SIP_CACHE_SERVER;
    /**
     * 设备注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_DEVICE_CACHE_SERVER = VideoConstant.VIDEO_DEVICE_CACHE_SERVER;
    /**
     * 上级注册缓存服务key(服务器共享数据)
     */
    public static final String VIDEO_PLATFORM_CACHE_SERVER = VideoConstant.VIDEO_PLATFORM_CACHE_SERVER;


    public void putSip(String gbId, Address address) {
        VideoCache.set(getSipKey(gbId), address);
    }

    public Address getSip(String gbId) {
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(getSipKey(gbId)));
        if (scan.size() > 0) {
            return (Address)RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }

    public List<Address> getSipList() {
        List<Address> list= new ArrayList<>();
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(getSipKey("*")));
        for (String key : scan) {
            Address address = (Address) RedisUtils.getCacheObject(key);
            if(address != null){
                list.add(address);
            }
        }
        return list;
    }

    public void delSip(String gbId) {
        RedisUtils.deleteObject(getSipKey(gbId));
    }

    public void putDevice(String gbId,int time, Address address) {
        VideoCache.set(getDeviceKey(gbId,true), address,time);
    }

    public Address getDevice(String gbId) {
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(getDeviceKey(gbId,true)));
        if (scan.size() > 0) {
            return (Address)RedisUtils.getCacheObject((String)scan.get(0));
        }else {
            return null;
        }
    }

    public void delDevice(String gbId) {
        RedisUtils.deleteObject(getDeviceKey(gbId,true));
    }

    public void putPlatform(String gbId,int time, Address address) {
        VideoCache.set(getPlatformKey(gbId,true), address,time);
    }

    public Address getPlatform(String gbId) {
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(getPlatformKey(gbId,true)));
        if (!scan.isEmpty()) {
            return (Address)RedisUtils.getCacheObject((String)scan.get(0));
        }else {
            return null;
        }
    }

    public boolean isNotServerDevice(String gbId){
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(getDeviceKey(gbId,false)));
        if (!scan.isEmpty()) {
            return true;
        }else {
            return false;
        }
    }

    public boolean isNotPlatformDevice(String gbId){
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(getPlatformKey(gbId,false)));
        if (!scan.isEmpty()) {
            return true;
        }else {
            return false;
        }
    }

    public void delPlatform(String gbId) {
        RedisUtils.deleteObject(getPlatformKey(gbId,true));
    }

    private String getSipKey(String gbId){
        return String.format("%s%s",VIDEO_SIP_CACHE_SERVER,gbId);
    }

    private String getDeviceKey(String gbId,boolean isServer){
        return String.format("%s%s:%s",VIDEO_DEVICE_CACHE_SERVER,isServer?videoProperties.getServerId():"*",gbId);
    }

    private String getPlatformKey(String gbId,boolean isServer){
        return String.format("%s%s:%s",VIDEO_PLATFORM_CACHE_SERVER,isServer?videoProperties.getServerId():"*",gbId);
    }
}
