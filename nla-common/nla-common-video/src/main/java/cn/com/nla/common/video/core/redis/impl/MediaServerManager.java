package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedHookVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class MediaServerManager {

    private String VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX = VideoConstant.VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX;

    @Autowired
    private VideoProperties videoProperties;

    public void clearRTPServer(MediaServerVo mediaServerVo) {
        VideoCache.score(VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX + videoProperties.getServerId(), mediaServerVo.getId(), 0);
    }

    public void clearMediaServerForOnline() {
        String key = VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX + videoProperties.getServerId();
        RedisUtils.deleteObject(key);
    }

    public void resetOnlineServerItem(MediaServerVo mediaServerVo) {
        // 更新缓存
        String key = VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX + videoProperties.getServerId();
        // 使用zset的分数作为当前并发量， 默认值设置为0
        if (VideoCache.getScore(key,mediaServerVo.getId()) == null) {  // 不存在则设置默认值 已存在则重置
            VideoCache.score(key, mediaServerVo.getId(), 0L);
            MediaRestResult result = MediaClient.getMediaList(mediaServerVo, "__defaultVhost__", "rtsp", null, null);
            if(result != null && result.getCode() == RespCode.CODE_0.getValue()){
                List<OnStreamChangedHookVo> onStreamChangedHookVos = JSONUtil.toList(JSONUtil.toJsonStr(result.getData()), OnStreamChangedHookVo.class);
                if(onStreamChangedHookVos != null){
                    VideoCache.score(key, mediaServerVo.getId(), onStreamChangedHookVos.size());
                }
            }
        }else {
            clearRTPServer(mediaServerVo);
        }
    }

    public void addCount(String mediaServerId) {
        if (mediaServerId == null) {
            return;
        }
        String key = VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX + videoProperties.getServerId();
        VideoCache.addScore(key, mediaServerId, 1);
    }

    public void removeCount(String mediaServerId) {
        if (mediaServerId == null) {
            return;
        }
        String key = VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX + videoProperties.getServerId();
        VideoCache.addScore(key, mediaServerId, -1);
    }
    /**
     * 获取负载最低的节点
     */
    public String getMediaServerForMinimumLoad() {
        String key = VIDEO_MEDIA_ONLINE_SERVERS_COUNT_PREFIX + videoProperties.getServerId();
        Long size = VideoCache.scoreSize(key);
        if (size  == null || size == 0) {
            log.info("获取负载最低的节点时无在线节点");
            return null;
        }
        Set<Object> objects = VideoCache.scoreRange(key, 0, -1);
        ArrayList<Object> mediaServerIdList = new ArrayList<>(objects);
        return (String) mediaServerIdList.get(0);
    }
}
