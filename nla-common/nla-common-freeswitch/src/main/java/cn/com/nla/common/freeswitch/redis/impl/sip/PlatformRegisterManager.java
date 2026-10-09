package cn.com.nla.common.freeswitch.redis.impl.sip;

import java.time.Duration;

import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.client.sip.properties.VideoProperties;
import cn.com.nla.common.freeswitch.vo.sip.PlatformRegisterInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PlatformRegisterManager {

    @Resource
    private VideoProperties videoProperties;

    private String PLATFORM_REGISTER_CATCH_PREFIX = SipConstant.PLATFORM_REGISTER_CATCH_PREFIX;


    public void updatePlatformRegisterInfo(String callId, PlatformRegisterInfo platformRegisterInfo) {
        String key = PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() +  ":" + callId;
        RedisUtils.setCacheObject(key,platformRegisterInfo,Duration.ofSeconds(30));
    }


    public PlatformRegisterInfo queryPlatformRegisterInfo(String callId) {
        Object o = RedisUtils.getCacheObject(PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() + ":" + callId);
        return (PlatformRegisterInfo)o;
    }

    public void delPlatformRegisterInfo(String callId) {
        RedisUtils.deleteObject(PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() +  ":" + callId);
    }

    public void cleanPlatformRegisterInfos() {
        List regInfos = new ArrayList<>(RedisUtils.keys("*" + PLATFORM_REGISTER_CATCH_PREFIX + videoProperties.getServerId() +  ":" + "*" + "*"));
        for (Object key : regInfos) {
            RedisUtils.deleteObject(key.toString());
        }
    }

}
