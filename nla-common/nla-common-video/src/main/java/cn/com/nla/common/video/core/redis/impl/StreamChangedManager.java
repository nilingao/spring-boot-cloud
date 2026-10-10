package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedHookVo;
import cn.com.nla.common.video.core.properties.VideoProperties;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

/**
 * 流注册注销时缓存
 */
public class StreamChangedManager {
    @Autowired
    private VideoProperties videoProperties;
    public static final String VIDEO_MEDIA_STREAM_CHANGED_PREFIX = VideoConstant.VIDEO_MEDIA_STREAM_CHANGED_PREFIX;

    public void put(OnStreamChangedHookVo vo) {
        String key = String.format("%s%s:%s:%s:%s:%s", VIDEO_MEDIA_STREAM_CHANGED_PREFIX,videoProperties.getServerId(), vo.getMediaServerId(), vo.getApp(), vo.getStream(),vo.getSchema());
        VideoCache.set(key, vo);
    }

    public void remove(OnStreamChangedHookVo vo) {
        String key = String.format("%s%s:%s:%s:%s:%s", VIDEO_MEDIA_STREAM_CHANGED_PREFIX,videoProperties.getServerId(), vo.getMediaServerId(), vo.getApp(), vo.getStream(),vo.getSchema());
        RedisUtils.deleteObject(key);
    }

    public List<OnStreamChangedHookVo> getMediaServerAll(String mediaServerId) {
        String key = String.format("%s%s:%s:%s", VIDEO_MEDIA_STREAM_CHANGED_PREFIX,videoProperties.getServerId(), mediaServerId,"*");
        List<String> scanResult = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scanResult.size() == 0) {
            return null;
        }
        List<OnStreamChangedHookVo> result = new ArrayList<>();
        for (String keyObj : scanResult) {
            result.add((OnStreamChangedHookVo)RedisUtils.getCacheObject( keyObj));
        }
        return result;
    }

}
