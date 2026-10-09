package cn.com.nla.common.freeswitch.redis.impl.sip;

import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 注册相关信息
 */
@Slf4j
@Component
public class SipTransactionManager {
    private String SIP_TRANSACTION =  "SIP_TRANSACTION:";

    private String DEVICE_PREFIX = String.format("%s%s", SipConstant.DEVICE_PREFIX,SIP_TRANSACTION);

    private String PARENT_PLATFORM_PREFIX =  String.format("%s%s",SipConstant.PARENT_PLATFORM_PREFIX,SIP_TRANSACTION);


    public void putDevice(String agentKey, SipTransactionInfo sipTransactionInfo){
        RedisUtils.setCacheObject(getKey(DEVICE_PREFIX, agentKey),sipTransactionInfo);
    }

    public SipTransactionInfo findDevice(String agentKey){
        return (SipTransactionInfo) RedisUtils.getCacheObject(getKey(DEVICE_PREFIX, agentKey));
    }
    public void delDevice(String agentKey){
        RedisUtils.deleteObject(getKey(DEVICE_PREFIX,agentKey));
    }

    public void putParentPlatform(String agentKey, SipTransactionInfo sipTransactionInfo){
        RedisUtils.setCacheObject(getKey(PARENT_PLATFORM_PREFIX, agentKey),sipTransactionInfo);
    }

    public SipTransactionInfo findParentPlatform(String agentKey){
        return (SipTransactionInfo) RedisUtils.getCacheObject(getKey(PARENT_PLATFORM_PREFIX,agentKey));
    }

    public void delParentPlatform(String agentKey){
        RedisUtils.deleteObject(getKey(PARENT_PLATFORM_PREFIX,agentKey));
    }

    private String getKey(String prefix,String key) {
        return String.format("%s%s", prefix, key);
    }



}
