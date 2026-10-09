package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.redis.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CallInfoManager {

    private String FS_CALL_INFO = RedisConstant.FS_CALL_INFO;


    public void put(CallInfo callInfo){
        if(callInfo == null ){
            return;
        }
        RedisUtils.setCacheObject(getKey(callInfo.getCallId()),callInfo);
    }


    public CallInfo get(String callId) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(callId) + "*"));
        if (!scan.isEmpty()) {
            return (CallInfo)RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }

    public CallInfo findDeviceId(String deviceId){
        String callId = RedisService.getDeviceInfoManager().getDeviceCallId(deviceId);
        if(StringUtils.isBlank(callId)){
            return null;
        }
        return get(callId);
    }
    public CallInfo findCaller(String caller){
        String callId = RedisService.getDeviceInfoManager().getCallerCallId(caller);
        if(StringUtils.isBlank(callId)){
            return null;
        }
        return get(callId);
    }


    public void del(String callId){
        CallInfo callInfo = get(callId);
        if(callInfo != null){
            Map<String, DeviceInfo> deviceInfoMap = callInfo.getDeviceInfoMap();
            if(deviceInfoMap!= null && !deviceInfoMap.isEmpty()){
                for (DeviceInfo deviceInfo : deviceInfoMap.values()) {
                    RedisService.getDeviceInfoManager().delDeviceCallId(deviceInfo.getDeviceId());
                    if(StringUtils.isNotEmpty(deviceInfo.getCaller())){
                        RedisService.getDeviceInfoManager().delCallerCallId(deviceInfo.getCaller());
                    }
                    if(StringUtils.isNotEmpty(deviceInfo.getCalled())){
                        RedisService.getDeviceInfoManager().delCallerCallId(deviceInfo.getCalled());
                    }
                }
            }
        }
        RedisUtils.deleteObject(getKey(callId));
    }

    private String getKey(String streamId){
        return String.format("%s%s",FS_CALL_INFO,streamId);
    }

}
