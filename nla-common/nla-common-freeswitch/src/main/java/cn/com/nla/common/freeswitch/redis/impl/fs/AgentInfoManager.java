package cn.com.nla.common.freeswitch.redis.impl.fs;

import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import gov.nist.javax.sip.message.SIPRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AgentInfoManager {

    private String FS_AGENT_INFO = RedisConstant.FS_AGENT_INFO;
    private String FS_AGENT_SIP_INFO = RedisConstant.FS_AGENT_SIP_INFO;
    private String FS_SOCKET_AGENT_CODE = RedisConstant.FS_SOCKET_AGENT_CODE;
    private String FS_CALL_PHONE = RedisConstant.FS_CALL_PHONE;
    private String FS_COMPANY_AGENT = RedisConstant.FS_COMPANY_AGENT;
    public void put(AgentVoInfo model){
        if(model == null ){
            return;
        }
        AgentVoInfo agentVoInfo = this.get(model.getAgentKey());
        if(agentVoInfo !=null){
            AgentStateEnum agentState = agentVoInfo.getAgentState();
            BeanUtil.copyProperties(model, agentVoInfo, CopyOptions.create().setIgnoreNullValue(true));
            if(model.getAgentState() ==  AgentStateEnum.LOGIN && agentState != AgentStateEnum.LOGIN){
                agentVoInfo.setAgentState(agentState);
            }
        }else {
            agentVoInfo = model;
        }
        RedisUtils.setCacheObject(getKey(agentVoInfo.getAgentKey()),agentVoInfo);
        RedisUtils.setCacheObject(getCompanyAgentIdKey(agentVoInfo.getCompanyId(),agentVoInfo.getAgentId()),agentVoInfo.getAgentKey());
        if(!ObjectUtils.isEmpty(agentVoInfo.getSipPhoneList())){
            for (String sipPhone : agentVoInfo.getSipPhoneList()) {
                RedisUtils.setCacheObject(getAgentSipKey(sipPhone),agentVoInfo.getAgentKey());
            }
        }
    }
    public AgentVoInfo get(String agentKey) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(agentKey) + "*"));
        if (!scan.isEmpty()) {
            return RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }
    public AgentVoInfo getSip(String agentSip) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getAgentSipKey(agentSip) + "*"));
        if (scan.isEmpty()) {
            return null;

        }
        String agentKey = RedisUtils.getCacheObject(scan.get(0));
        return get(agentKey);
    }
    public AgentVoInfo getCompanyAgentId(String companyId, String agentId) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getCompanyAgentIdKey(companyId,agentId) + "*"));
        if (scan.isEmpty()) {
            return null;
        }
        String agentKey = RedisUtils.getCacheObject(scan.get(0));
        return get(agentKey);
    }
    public void delSip(String sipPhone){
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getAgentSipKey(sipPhone) + "*"));
        for (String key : scan) {
            RedisUtils.deleteObject(key);
        }
    }
    public void delCompanyAgent(String companyId, String agentId){
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getCompanyAgentIdKey(companyId,agentId) + "*"));
        for (String key : scan) {
            RedisUtils.deleteObject(key);
        }
    }
    public void del(String agentKey){
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getKey(agentKey) + "*"));
        for (String key : scan) {
            AgentVoInfo agentVoInfo = RedisUtils.getCacheObject(key);
            if(!ObjectUtils.isEmpty(agentVoInfo.getSipPhoneList())){
                for (String sipPhone : agentVoInfo.getSipPhoneList()) {
                    delSip(sipPhone);
                }
            }
            delCompanyAgent(agentVoInfo.getCompanyId(),agentVoInfo.getAgentId());
            RedisUtils.deleteObject(key);
        }
    }
    public void delAll(){
        del("*");
    }

    public void putCallPhone(String callId, SIPRequest model){
        if(model == null ){
            return;
        }
        RedisUtils.setCacheObject(getCallPhoneKey(callId),SerializationUtils.serialize(model),Duration.ofSeconds(30));
    }

    public SIPRequest getCallPhone(String callId) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getCallPhoneKey(callId) + "*"));
        if (!scan.isEmpty()) {
            byte[] o = RedisUtils.getCacheObject(scan.get(0));
            if(ObjectUtils.isEmpty(o)){
                return null;
            }
            return (SIPRequest)SerializationUtils.deserialize(o);
        }else {
            return null;
        }
    }
    public void delCallPhone(String callId){
        RedisUtils.deleteObject(getCallPhoneKey(callId));
    }

    public void putAgentKey(String uuid, String agentKey){
        if(uuid == null || agentKey == null){
            return;
        }
        RedisUtils.setCacheObject(getAgentKeyKey(uuid),agentKey);
    }
    public String getAgentKey(String uuid) {
        List<String> scan = new ArrayList<>(RedisUtils.keys("*" + getAgentKeyKey(uuid) + "*"));
        if (!scan.isEmpty()) {
            return RedisUtils.getCacheObject(scan.get(0));
        }else {
            return null;
        }
    }

    public void delAgentKey(String uuid){
        if(StringUtils.isEmpty(uuid)){
            uuid = "*";
        }
        for (String key : RedisUtils.keys("*" + getAgentKeyKey(uuid) + "*")) {
            RedisUtils.deleteObject(key);
        }
    }

    private String getKey(String agentKey){
        return String.format("%s%s",FS_AGENT_INFO,agentKey);
    }

    private String getAgentSipKey(String agentSip){
        return String.format("%s%s",FS_AGENT_SIP_INFO,agentSip);
    }

    private String getCallPhoneKey(String agentKey){
        return String.format("%s%s",FS_CALL_PHONE,agentKey);
    }

    private String getAgentKeyKey(String uuid){
        return String.format("%s%s",FS_SOCKET_AGENT_CODE,uuid);
    }

    private String getCompanyAgentIdKey(String companyId, String agentId){
        return String.format("%s%s:%s",FS_COMPANY_AGENT,companyId,agentId);
    }
}
