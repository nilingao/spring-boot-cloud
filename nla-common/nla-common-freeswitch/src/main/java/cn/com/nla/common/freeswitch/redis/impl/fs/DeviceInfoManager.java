package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeviceInfoManager {

    private String FS_CALLER_CALL_ID = RedisConstant.FS_CALLER_CALL_ID;
    private String FS_DEVICE_CALL_ID = RedisConstant.FS_DEVICE_CALL_ID;

    public void putCallerCallId(String caller, String callId){
        RedisUtils.setCacheObject(getCallerCallIdKey(caller),callId);
    }
    public String getCallerCallId(String key) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getCallerCallIdKey(key) + "*"));
        if (!scan.isEmpty()) {
            return (String) RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }
    public void delCallerCallId(String key){
        RedisUtils.deleteObject(getCallerCallIdKey(key));
    }
    private String getCallerCallIdKey(String key){
        return String.format("%s%s", FS_CALLER_CALL_ID,key);
    }

    //**********************************************

    public void putDeviceCallId(String deviceId,String callId){
        RedisUtils.setCacheObject(getKeyCallId(deviceId),callId);
    }
    public String getDeviceCallId(String key) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKeyCallId(key) + "*"));
        if (!scan.isEmpty()) {
            return (String) RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }
    public void delDeviceCallId(String key){
        RedisUtils.deleteObject(getKeyCallId(key));
    }
    private String getKeyCallId(String key){
        return String.format("%s%s", FS_DEVICE_CALL_ID,key);
    }
}
