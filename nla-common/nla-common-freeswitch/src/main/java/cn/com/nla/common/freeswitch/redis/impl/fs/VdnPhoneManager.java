package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.fs.VdnPhoneInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VdnPhoneManager {

    private String FS_VDN_PHONE_INFO = RedisConstant.FS_VDN_PHONE_INFO;


    public void put(VdnPhoneInfo model){
        if(model == null ){
            return;
        }
        RedisUtils.setCacheObject(getKey(model.getPhone()),model);
    }


    public VdnPhoneInfo get(String phone) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(phone) + "*"));
        if (!scan.isEmpty()) {
            return (VdnPhoneInfo)RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }


    public void del(String id){
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(id) + "*"));
        for (String key : scan) {
            RedisUtils.deleteObject(key);
        }
    }

    public void delAll(){
        del("*");
    }

    private String getKey(String streamId){
        return String.format("%s%s",FS_VDN_PHONE_INFO,streamId);
    }
}
