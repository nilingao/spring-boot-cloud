package cn.com.nla.common.freeswitch.redis.impl.sip;

import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import cn.com.nla.common.freeswitch.vo.sip.SsrcTransaction;
import cn.com.nla.common.redis.utils.RedisUtils;
import gov.nist.javax.sip.message.SIPMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频流session管理器，管理视频预览、预览回放的通信句柄
 */
@Component
public class SsrcTransactionManager {

    private String MEDIA_TRANSACTION_USED_PREFIX = SipConstant.MEDIA_TRANSACTION_USED_PREFIX;

    /**
     * 添加一个点播/回放的事务信息
     * 后续可以通过流Id/callID
     * @param agentKey 设备ID
     * @param callId 一次请求的CallID
     * @param stream 流名称
     * @param mediaServerId 所使用的流媒体ID
     * @param response 回复
     */
    public void put(String agentKey, String callId, String app, String stream, String ssrc, String mediaServerId, SIPMessage response, String typeName){
        SsrcTransaction ssrcTransaction = new SsrcTransaction();
        ssrcTransaction.setAgentKey(agentKey);
        ssrcTransaction.setStream(app);
        ssrcTransaction.setStream(stream);
        ssrcTransaction.setSipTransactionInfo(ObjectUtils.isEmpty(response) ? null : new SipTransactionInfo(response));
        ssrcTransaction.setCallId(callId);
        ssrcTransaction.setSsrc(ssrc);
        ssrcTransaction.setMediaServerId(mediaServerId);
        ssrcTransaction.setTypeName(typeName);
        RedisUtils.setCacheObject(getKey(agentKey,stream,typeName,callId), ssrcTransaction);
    }

    public void put(SsrcTransaction ssrcTransaction){
        RedisUtils.setCacheObject(getKey(ssrcTransaction.getAgentKey(),ssrcTransaction.getStream(),ssrcTransaction.getTypeName(),ssrcTransaction.getCallId()), ssrcTransaction);
    }

    public SsrcTransaction getParamOne(String agentKey,  String callId, String stream,String type){
        List<String> scanResult = new ArrayList<>(RedisUtils.keys("*" + getKey(agentKey,stream,type,callId) + "*"));
        if (scanResult.size() == 0) {
            return null;
        }
        return (SsrcTransaction)RedisUtils.getCacheObject(scanResult.get(0));
    }

    public List<SsrcTransaction> getParamAll(String agentKey,  String callId, String stream,String typeName){
        List<String> scanResult = new ArrayList<>(RedisUtils.keys("*" + getKey(agentKey,stream,typeName,callId) + "*"));
        if (scanResult.size() == 0) {
            return null;
        }
        List<SsrcTransaction> result = new ArrayList<>();
        for (String keyObj : scanResult) {
            result.add((SsrcTransaction)RedisUtils.getCacheObject( keyObj));
        }
        return result;
    }
    public void remove(String agentKey, String stream) {
        this.remove(agentKey,stream,null,null);
    }

    public void remove(String agentKey, String stream,String typeName) {
        this.remove(agentKey,stream,null,typeName);
    }

    public void remove(String agentKey, String stream,String callId,String typeName) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(agentKey,stream,typeName,callId) + "*"));
        if (scan.size() > 0) {
            for (String keyStr : scan) {
                RedisUtils.deleteObject(keyStr);
            }
        }
    }

    public List<SsrcTransaction> getAllSsrc() {
        List<String> ssrcTransactionKeys = new ArrayList<>(RedisUtils.keys("*" + getKey("*","*",null,"*") + "*"));
        List<SsrcTransaction> result= new ArrayList<>();
        for (int i = 0; i < ssrcTransactionKeys.size(); i++) {
            String key = ssrcTransactionKeys.get(i);
            SsrcTransaction ssrcTransaction = (SsrcTransaction)RedisUtils.getCacheObject(key);
            result.add(ssrcTransaction);
        }
        return result;
    }

    private String getKey(String agentKey,String stream,String typeName ,String callId){
        if(StringUtils.isEmpty(agentKey)){
            agentKey ="*";
        }
        if(StringUtils.isEmpty(stream)){
            stream ="*";
        }
        if(StringUtils.isEmpty(callId)){
            callId ="*";
        }
        if(StringUtils.isEmpty(typeName)){
            typeName ="*";
        }
        return String.format("%s:%s:%s:%s:%s",MEDIA_TRANSACTION_USED_PREFIX,agentKey,stream,typeName,callId);
    }

}
