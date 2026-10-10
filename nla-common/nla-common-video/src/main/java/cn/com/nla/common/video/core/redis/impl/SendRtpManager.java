package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.core.properties.VideoProperties;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SendRtpManager {


    @Autowired
    private VideoProperties videoProperties;

    private String PLATFORM_SEND_RTP_INFO_PREFIX = VideoConstant.PLATFORM_SEND_RTP_INFO_PREFIX;

    public void put(SendRtp sendRtpItem) {
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                sendRtpItem.getMediaServerId(),
                sendRtpItem.getPlatformId(),
                sendRtpItem.getChannelId(),
                sendRtpItem.getStreamId(),
                sendRtpItem.getCallId()
        );
        VideoCache.set(key, sendRtpItem);
    }

    public SendRtp querySendRTPServer(String platformGbId, String channelId, String streamId, String callId) {
        if (platformGbId == null) {
            platformGbId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (streamId == null) {
            streamId = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                platformGbId,
                channelId,
                streamId,
                callId
        );
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scan.size() > 0) {
            return (SendRtp)RedisUtils.getCacheObject((String)scan.get(0));
        }else {
            return null;
        }
    }

    public List<SendRtp> querySendRTPServerByChnnelId(String channelId) {
        if (channelId == null) {
            return null;
        }
        String platformGbId = "*";
        String callId = "*";
        String streamId = "*";
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                platformGbId,
                channelId,
                streamId,
                callId
        );
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        List<SendRtp> result = new ArrayList<>();
        for (Object o : scan) {
            result.add((SendRtp) RedisUtils.getCacheObject((String) o));
        }
        return result;
    }

    public List<SendRtp> querySendRTPServerByStream(String stream) {
        if (stream == null) {
            return null;
        }
        String platformGbId = "*";
        String callId = "*";
        String channelId = "*";
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                platformGbId,
                channelId,
                stream,
                callId
        );
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        List<SendRtp> result = new ArrayList<>();
        for (Object o : scan) {
            result.add((SendRtp) RedisUtils.getCacheObject((String) o));
        }
        return result;
    }

    public List<SendRtp> querySendRTPServer(String platformGbId) {
        if (platformGbId == null) {
            platformGbId = "*";
        }
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                platformGbId,
                "*",
                "*",
                "*"
        );
        List<String> queryResult = new java.util.ArrayList<>(RedisUtils.keys(key));
        List<SendRtp> result= new ArrayList<>();

        for (String o : queryResult) {
            result.add((SendRtp) RedisUtils.getCacheObject(o));
        }

        return result;
    }

    /**
     * 删除RTP推送信息缓存
     * @param platformGbId
     * @param channelId
     */
    public void deleteSendRTPServer(String platformGbId, String channelId, String streamId ,String callId) {
        if (streamId == null) {
            streamId = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                platformGbId,
                channelId,
                streamId,
                callId
        );
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scan.size() > 0) {
            for (Object keyStr : scan) {
                RedisUtils.deleteObject((String)keyStr);
            }
        }
    }

    public List<SendRtp> queryAllSendRTPServer() {
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                "*",
                "*",
                "*",
                "*"
        );
        List<String> queryResult = new java.util.ArrayList<>(RedisUtils.keys(key));
        List<SendRtp> result= new ArrayList<>();

        for (String o : queryResult) {
            result.add((SendRtp) RedisUtils.getCacheObject(o));
        }

        return result;
    }

    /**
     * 查询某个通道是否存在上级点播（RTP推送）
     * @param channelId
     */
    public boolean isChannelSendingRTP(String channelId) {
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                "*",
                channelId,
                "*",
                "*"
        );
        List<String> RtpStreams = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (RtpStreams.size() > 0) {
            return true;
        } else {
            return false;
        }
    }


    public int getGbSendCount(String platformGbId) {
        String key = String.format(
                "%s:%s:%s:%s:%s:%s:%s",
                PLATFORM_SEND_RTP_INFO_PREFIX,
                videoProperties.getServerId(),
                "*",
                platformGbId,
                "*",
                "*",
                "*"
        );
        return new java.util.ArrayList<>(RedisUtils.keys(key)).size();
    }
}
