package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import lombok.extern.slf4j.Slf4j;


/**
 * 注册相关信息
 */
@Slf4j
public class SipTransactionManager {
    private String SIP_TRANSACTION =  "SIP_TRANSACTION:";

    private String DEVICE_PREFIX = String.format("%s%s", VideoConstant.DEVICE_PREFIX,SIP_TRANSACTION);

    private String PARENT_PLATFORM_PREFIX =  String.format("%s%s", VideoConstant.PARENT_PLATFORM_PREFIX,SIP_TRANSACTION);


    public void putDevice(String deviceId, SipTransactionInfo sipTransactionInfo){
        String key = String.format("%s%s", DEVICE_PREFIX, deviceId);
        VideoCache.set(key,sipTransactionInfo);
    }

    public SipTransactionInfo findDevice(String deviceId){
        String key = String.format("%s%s", DEVICE_PREFIX, deviceId);
        return (SipTransactionInfo) RedisUtils.getCacheObject(key);
    }

    public void putParentPlatform(String parentPlatformId, SipTransactionInfo sipTransactionInfo){
        String key = String.format("%s%s", PARENT_PLATFORM_PREFIX, parentPlatformId);
        VideoCache.set(key,sipTransactionInfo);
    }

    public SipTransactionInfo findParentPlatform(String parentPlatformId){
        String key = String.format("%s%s", PARENT_PLATFORM_PREFIX, parentPlatformId);
        return (SipTransactionInfo) RedisUtils.getCacheObject(key);
    }
}
