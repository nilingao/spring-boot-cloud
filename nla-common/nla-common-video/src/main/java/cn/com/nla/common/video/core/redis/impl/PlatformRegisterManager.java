package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.sip.PlatformRegisterInfo;
import cn.com.nla.common.video.core.properties.VideoProperties;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Slf4j
public class PlatformRegisterManager {

    @Autowired
    private VideoProperties videoProperties;

    private String PLATFORM_REGISTER_CATCH_PREFIX = VideoConstant.PLATFORM_REGISTER_CATCH_PREFIX;


    public void updatePlatformRegisterInfo(String callId, PlatformRegisterInfo platformRegisterInfo) {
        String key = PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() +  ":" + callId;
        VideoCache.set(key, platformRegisterInfo, 30);
    }


    public PlatformRegisterInfo queryPlatformRegisterInfo(String callId) {
        Object o = RedisUtils.getCacheObject(PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() + ":" + callId);
        return (PlatformRegisterInfo)o;
    }

    public void delPlatformRegisterInfo(String callId) {
        RedisUtils.deleteObject(PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() +  ":" + callId);
    }

    public void cleanPlatformRegisterInfos() {
        List regInfos = new java.util.ArrayList<>(RedisUtils.keys(PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() +  ":" + "*"));
        for (Object key : regInfos) {
            RedisUtils.deleteObject(key.toString());
        }
    }

}
