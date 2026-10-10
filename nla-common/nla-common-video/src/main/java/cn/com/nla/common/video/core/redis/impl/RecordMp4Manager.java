package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RecordMp4Manager {

    @Autowired
    private VideoProperties videoProperties;

    private String VIDEO_RECORD_MP4_INFO = VideoConstant.VIDEO_RECORD_MP4_INFO;


    public Map<String,Object> get(String streamId){
       return (Map<String,Object>) RedisUtils.getCacheObject(getKey(streamId));
    }

    public void put(String streamId,InviteInfo inviteInfo){
        if(inviteInfo == null || inviteInfo.getStreamInfo() == null){
            return;
        }
        String key = getKey(streamId);
        Map<Object, Object> data = new HashMap<>();
        data.put("type",inviteInfo.getType().ordinal());
        data.put("streamInfo", JSONUtil.toJsonStr(inviteInfo.getStreamInfo()));
        VideoCache.set(key,data,-1L);
    }


    public void del(String streamId){
        Map<String, Object> map = get(streamId);
        if(map == null){
            return;
        }
        //设置15秒过期
        VideoCache.set(getKey(streamId),map,15L);
    }


    private String getKey(String streamId){
        return String.format("%s_%s",VIDEO_RECORD_MP4_INFO,streamId);
    }

}
