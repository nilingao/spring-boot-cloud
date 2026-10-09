package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.model.fs.GroupInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GroupInfoManager {
    private String FS_GROUP_INFO = RedisConstant.FS_GROUP_INFO;


    public void put(GroupInfo groupInfo){
        if(groupInfo == null ){
            return;
        }
        RedisUtils.setCacheObject(getKey(groupInfo.getId()),groupInfo);
    }


    public GroupInfo get(String id) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(id) + "*"));
        if (!scan.isEmpty()) {
            return (GroupInfo)RedisUtils.getCacheObject(scan.get(0));
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
        return String.format("%s%s",FS_GROUP_INFO,id);
    }
}
