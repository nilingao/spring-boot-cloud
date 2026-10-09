package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.fs.PlaybackInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PlaybackInfoManager {

    private String FS_PLAYBACK_INFO = RedisConstant.FS_PLAYBACK_INFO;


    public void put(PlaybackInfo groupInfo){
        if(groupInfo == null ){
            return;
        }
        RedisUtils.setCacheObject(getKey(groupInfo.getId()),groupInfo);
    }


    public PlaybackInfo get(String id) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(id) + "*"));
        if (!scan.isEmpty()) {
            return (PlaybackInfo)RedisUtils.getCacheObject(scan.get(0));
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

    private String getKey(String id){
        return String.format("%s%s",FS_PLAYBACK_INFO,id);
    }
}
