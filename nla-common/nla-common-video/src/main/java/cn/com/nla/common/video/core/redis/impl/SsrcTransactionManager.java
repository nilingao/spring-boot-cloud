package cn.com.nla.common.video.core.redis.impl;

import cn.com.nla.common.video.core.redis.VideoCache;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.VideoStreamType;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import gov.nist.javax.sip.message.SIPMessage;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频流session管理器，管理视频预览、预览回放的通信句柄
 */
public class SsrcTransactionManager {

    private String MEDIA_TRANSACTION_USED_PREFIX = VideoConstant.MEDIA_TRANSACTION_USED_PREFIX;

    /**
     * 添加一个点播/回放的事务信息
     * 后续可以通过流Id/callID
     * @param deviceId 设备ID
     * @param channelId 通道ID
     * @param callId 一次请求的CallID
     * @param stream 流名称
     * @param mediaServerId 所使用的流媒体ID
     * @param response 回复
     */
    public void put(String deviceId, String channelId, String callId, String app, String stream, String ssrc, String mediaServerId, SIPMessage response, VideoStreamType type){
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setDeviceId(deviceId);
        ssrcTransaction.setChannelId(channelId);
        ssrcTransaction.setStream(app);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setSipTransactionInfo(ObjectUtils.isEmpty(response) ? null : new SipTransactionInfo(response));
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setType(type);
        String key = String.format("%s:%s:%s:%s:%s:%s",MEDIA_TRANSACTION_USED_PREFIX,deviceId,channelId,stream,type.ordinal(),callId);
        VideoCache.set(key, ssrcTransaction);
    }

    public SsrcTransaction getParamOne(String deviceId, String channelId, String callId, String stream,VideoStreamType type){
        if (ObjectUtils.isEmpty(deviceId)) {
            deviceId ="*";
        }
        if (ObjectUtils.isEmpty(channelId)) {
            channelId ="*";
        }
        if (ObjectUtils.isEmpty(callId)) {
            callId ="*";
        }
        if (ObjectUtils.isEmpty(stream)) {
            stream ="*";
        }
        String typeStr = (type != null?String.valueOf(type.ordinal()):"*");
        String key = String.format("%s:%s:%s:%s:%s:%s",MEDIA_TRANSACTION_USED_PREFIX,deviceId,channelId,stream,typeStr,callId);
        List<String> scanResult = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scanResult.size() == 0) {
            return null;
        }
        return (SsrcTransaction)RedisUtils.getCacheObject(scanResult.get(0));
    }

    public List<SsrcTransaction> getParamAll(String deviceId, String channelId, String callId, String stream,VideoStreamType type){
        if (ObjectUtils.isEmpty(deviceId)) {
            deviceId ="*";
        }
        if (ObjectUtils.isEmpty(channelId)) {
            channelId ="*";
        }
        if (ObjectUtils.isEmpty(callId)) {
            callId ="*";
        }
        if (ObjectUtils.isEmpty(stream)) {
            stream ="*";
        }
        String typeStr = (type != null?String.valueOf(type.ordinal()):"*");
        String key = String.format("%s:%s:%s:%s:%s:%s",MEDIA_TRANSACTION_USED_PREFIX,deviceId,channelId,stream,typeStr,callId);
        List<String> scanResult = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scanResult.size() == 0) {
            return null;
        }
        List<SsrcTransaction> result = new ArrayList<>();
        for (String keyObj : scanResult) {
            result.add((SsrcTransaction)RedisUtils.getCacheObject( keyObj));
        }
        return result;
    }
    public void remove(String deviceId, String channelId, String stream) {
        this.remove(deviceId,channelId,stream,null,null);
    }

    public void remove(String deviceId, String channelId, String stream,VideoStreamType type) {
        this.remove(deviceId,channelId,stream,null,type);
    }

    public void remove(String deviceId, String channelId, String stream,String callId,VideoStreamType type) {
        if (ObjectUtils.isEmpty(stream)) {
            stream ="*";
        }
        if (ObjectUtils.isEmpty(callId)) {
            callId ="*";
        }
        String typeStr = (type != null?String.valueOf(type.ordinal()):"*");
        String key = String.format("%s:%s:%s:%s:%s:%s",MEDIA_TRANSACTION_USED_PREFIX,deviceId,channelId,stream,typeStr,callId);
        List<String> scan = new java.util.ArrayList<>(RedisUtils.keys(key));
        if (scan.size() > 0) {
            for (String keyStr : scan) {
                RedisUtils.deleteObject(keyStr);
            }
        }
    }

    public List<SsrcTransaction> getAllSsrc() {
        String keys = String.format("%s:*:*:*:*", MEDIA_TRANSACTION_USED_PREFIX);
        List<String> ssrcTransactionKeys = new java.util.ArrayList<>(RedisUtils.keys(keys));
        List<SsrcTransaction> result= new ArrayList<>();
        for (int i = 0; i < ssrcTransactionKeys.size(); i++) {
            String key = ssrcTransactionKeys.get(i);
            SsrcTransaction ssrcTransaction = (SsrcTransaction)RedisUtils.getCacheObject(key);
            result.add(ssrcTransaction);
        }
        return result;
    }

}
