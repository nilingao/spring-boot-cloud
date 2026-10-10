package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.NotNullMap;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.InviteSessionStatus;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedResult;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.demo.InviteInfo;
import cn.com.nla.common.video.core.service.video.MediaServerVoService;
import cn.com.nla.common.video.core.sip.callback.InviteErrorCallback;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class InviteStreamManager {

    private String INVITE_PREFIX = VideoConstant.INVITE_PREFIX;

    private String INVITE_DOWNLOAD_USER_PREFIX = VideoConstant.INVITE_DOWNLOAD_USER_PREFIX;

    private final Map<String, List<InviteErrorCallback<Object>>> inviteErrorCallbackMap = new ConcurrentHashMap<>();

    public InviteInfo updateInviteInfoForSSRC(InviteInfo inviteInfo, String ssrc){
        InviteInfo info = getInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream(),null);
        if(info == null || info.getSsrcInfo() == null){
            return null;
        }
        removeInviteInfo(info);
        info.getSsrcInfo().setSsrc(ssrc);
        String key = INVITE_PREFIX +
                ":" + info.getType() +
                ":" + info.getDeviceId() +
                ":" + info.getChannelId() +
                ":" + info.getStream() +
                ":" + info.getSsrcInfo().getSsrc();
        VideoCache.set(key, info);
        return info;
    }


    public void updateInviteInfo(InviteInfo inviteInfo) {
        if (inviteInfo == null || (inviteInfo.getDeviceId() == null || inviteInfo.getChannelId() == null)) {
            log.warn("[更新Invite信息]，参数不全： {}", JSONUtil.toJsonStr(inviteInfo));
            return;
        }
        InviteInfo inviteInfoForUpdate = null;

        if (InviteSessionStatus.ready == inviteInfo.getStatus()) {
            if (inviteInfo.getDeviceId() == null
                    || inviteInfo.getChannelId() == null
                    || inviteInfo.getType() == null
                    || inviteInfo.getStream() == null
            ) {
                return;
            }
            inviteInfoForUpdate = inviteInfo;
        } else {
            InviteInfo inviteInfoInRedis = getInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(),
                    inviteInfo.getChannelId(), inviteInfo.getStream(),null);
            if (inviteInfoInRedis == null) {
                log.warn("[更新Invite信息]，未从缓存中读取到Invite信息： deviceId: {}, channel: {}, stream: {}",
                        inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
                return;
            }
            if (inviteInfo.getStreamInfo() != null) {
                inviteInfoInRedis.setStreamInfo(inviteInfo.getStreamInfo());
            }
            if (inviteInfo.getSsrcInfo() != null) {
                inviteInfoInRedis.setSsrcInfo(inviteInfo.getSsrcInfo());
            }
            if (inviteInfo.getStreamMode() != null) {
                inviteInfoInRedis.setStreamMode(inviteInfo.getStreamMode());
            }
            if (inviteInfo.getReceiveIp() != null) {
                inviteInfoInRedis.setReceiveIp(inviteInfo.getReceiveIp());
            }
            if (inviteInfo.getReceivePort() != null) {
                inviteInfoInRedis.setReceivePort(inviteInfo.getReceivePort());
            }
            if (inviteInfo.getStatus() != null) {
                inviteInfoInRedis.setStatus(inviteInfo.getStatus());
            }
            inviteInfoForUpdate = inviteInfoInRedis;
        }
        String key = INVITE_PREFIX +
                ":" + inviteInfoForUpdate.getType() +
                ":" + inviteInfoForUpdate.getDeviceId() +
                ":" + inviteInfoForUpdate.getChannelId() +
                ":" + inviteInfoForUpdate.getStream() +
                ":" + inviteInfoForUpdate.getSsrcInfo().getSsrc();;

        //下载操作时关联用户与当前key的
        if(inviteInfoForUpdate.getType() == VideoStreamType.download && InviteSessionStatus.ok == inviteInfoForUpdate.getStatus()){
            String userKey = String.format("%s:%s",INVITE_DOWNLOAD_USER_PREFIX,inviteInfoForUpdate.getUserId());
            VideoCache.addSet(userKey,inviteInfoForUpdate.getStream());
        }
        VideoCache.set(key, inviteInfoForUpdate);
        RedisService.getRecordMp4Manager().put(inviteInfoForUpdate.getStream(),inviteInfoForUpdate);
    }

    public InviteInfo getInviteInfo(VideoStreamType type, String deviceId, String channelId, String stream,String ssrc) {
        String key = INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (deviceId != null ? deviceId : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*") +
                ":"+ (ssrc != null ? ssrc : "*");
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scan.size() > 0) {
            return (InviteInfo)RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }

    /**
     * 获取当前用户下所有下载视频
     * @param userId
     * @return
     */
    public List<NotNullMap> getUserDownloadInviteInfoList(Long userId){
        List<NotNullMap> inviteInfoList= new ArrayList<>();
        String userKey = String.format("%s:%s",INVITE_DOWNLOAD_USER_PREFIX,userId);
        Set<Object> keyList = VideoCache.readSet(userKey);
        if(keyList == null || keyList.isEmpty()){
            return new ArrayList<>();
        }
        for (Object o : keyList) {
            Object obj = RedisUtils.getCacheObject(o.toString());
            if(ObjectUtils.isNotEmpty(obj)){
                InviteInfo inviteInfo = (InviteInfo) obj;
                StreamInfo streamInfo = inviteInfo.getStreamInfo();
                if(streamInfo != null){
                    inviteInfoList.add(new NotNullMap(){{
                        putString("id",o.toString());
                        putString("app",streamInfo.getApp());
                        putString("stream",streamInfo.getStream());
                        putString("deviceId",streamInfo.getDeviceId());
                        putString("channelId",streamInfo.getChannelId());
                        putString("mediaServerId",streamInfo.getMediaServerId());
                        putString("startTime",streamInfo.getStartTime());
                        putString("endTime",streamInfo.getEndTime());
                        putDouble("progress",streamInfo.getProgress());
                    }});
                }
            }
        }
        return inviteInfoList;
    }

    /**
     * 清除用户下载录像
     * @param userId
     * @param key
     */
    public void delUserDownloadInviteInfoList(Long userId,String key){
        String userKey = String.format("%s:%s",INVITE_DOWNLOAD_USER_PREFIX,userId);
        if(VideoCache.containsSet(userKey,key)){
            InviteInfo inviteInfo = (InviteInfo) RedisUtils.getCacheObject(key);
            if(inviteInfo != null && inviteInfo.getStatus() == InviteSessionStatus.ok && inviteInfo.getStreamInfo() != null){
                StreamInfo streamInfo = inviteInfo.getStreamInfo();
                MediaServerVo mediaServerVo = VideoService.getMediaServerService().findOnLineMediaServerId(streamInfo.getMediaServerId());
                MediaClient.deleteRecordDirectory(mediaServerVo,"__defaultVhost__",streamInfo.getApp(),streamInfo.getStream(),null);
                RedisService.getRecordMp4Manager().del(inviteInfo.getStream());
                RedisUtils.deleteObject(key);
            }
            VideoCache.removeSet(userKey,key);
        }
    }

    public InviteInfo getInviteInfoBySSRC(String ssrc) {
        return getInviteInfo(null, null, null, null,ssrc);
    }

    public InviteInfo getInviteInfoByDeviceAndChannel(VideoStreamType type, String deviceId, String channelId) {
        return getInviteInfo(type, deviceId, channelId, null,null);
    }

    /**
     * 流规则Id目前规则
     * 实时播放 流Id规则 国标编号+通道编号
     * 历史播放 流Id规则 国标编号+通道编号+开始时间戳+结束时间戳
     * 下载 流Id规则 ssrc 的16进制 取前8位
     */
    public InviteInfo getInviteInfoByStream(VideoStreamType type, String stream) {
        return getInviteInfo(type, null, null, stream,null);
    }

    public void removeInviteInfo(VideoStreamType type, String deviceId, String channelId, String stream) {
        String keys = INVITE_PREFIX +
                ":" + (type != null ? type : "*") +
                ":" + (deviceId != null ? deviceId : "*") +
                ":" + (channelId != null ? channelId : "*") +
                ":" + (stream != null ? stream : "*") +
                ":*";
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(keys));
        if (scan.size() > 0) {
            MediaServerVoService mediaServerService = VideoService.getMediaServerService();
            for (String key : scan) {
                InviteInfo inviteInfo = (InviteInfo) RedisUtils.getCacheObject(key);;
                boolean del = false;
                if (inviteInfo != null) {
                    del = true;
                    if(inviteInfo.getStreamInfo() != null){
                        MediaServerVo mediaServerVo = mediaServerService.findMediaServerId(inviteInfo.getStreamInfo().getMediaServerId());
                        if(mediaServerVo != null){
                            OnStreamChangedResult result = MediaClient.getMediaInfo(mediaServerVo, null, "rtsp", inviteInfo.getStreamInfo().getApp(), inviteInfo.getStreamInfo().getStream());
                            if(result != null && result.getCode() == RespCode.CODE_0.getValue()){
                                del = false;
                            }
                        }
                    }
                }
                if(del){
                    RedisUtils.deleteObject(key);
                    RedisService.getRecordMp4Manager().del(inviteInfo.getStream());
                    String userKey = String.format("%s:%s",INVITE_DOWNLOAD_USER_PREFIX,inviteInfo.getUserId());
                    VideoCache.removeSet(userKey,key);
                    inviteErrorCallbackMap.remove(buildKey(type, deviceId, channelId, inviteInfo.getStream()));
                }
            }
        }
    }

    public void removeInviteInfoByDeviceAndChannel(VideoStreamType inviteSessionType, String deviceId, String channelId) {
        removeInviteInfo(inviteSessionType, deviceId, channelId, null);
    }

    public void removeInviteInfo(InviteInfo inviteInfo) {
        removeInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
    }

    public void once(VideoStreamType type, String deviceId, String channelId, String stream, InviteErrorCallback<Object> callback) {
        String key = buildKey(type, deviceId, channelId, stream);
        List<InviteErrorCallback<Object>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null) {
            callbacks = new CopyOnWriteArrayList<>();
            inviteErrorCallbackMap.put(key, callbacks);
        }
        callbacks.add(callback);
    }

    public void call(VideoStreamType type, String deviceId, String channelId, String stream, int code, String msg, Object data) {
        String key = buildKey(type, deviceId, channelId, stream);
        List<InviteErrorCallback<Object>> callbacks = inviteErrorCallbackMap.get(key);
        if (callbacks == null) {
            return;
        }
        for (InviteErrorCallback<Object> callback : callbacks) {
            callback.run(code, msg, data);
        }
        inviteErrorCallbackMap.remove(key);
    }

    private String buildKey(VideoStreamType type, String deviceId, String channelId, String stream) {
        String key = type + ":" +  deviceId + ":" + channelId;
        // 如果ssrc未null那么可以实现一个通道只能一次操作，ssrc不为null则可以支持一个通道多次invite
        if (stream != null) {
            key += (":" + stream);
        }
        return key;
    }

    public void clearInviteInfo(String deviceId) {
        removeInviteInfo(VideoStreamType.playback, deviceId, null, null);
        removeInviteInfo(VideoStreamType.play, deviceId, null, null);
    }
}
