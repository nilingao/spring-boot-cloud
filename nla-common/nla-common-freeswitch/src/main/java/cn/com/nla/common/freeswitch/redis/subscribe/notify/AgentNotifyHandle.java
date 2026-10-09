package cn.com.nla.common.freeswitch.redis.subscribe.notify;

import java.time.Duration;

import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.freeswitch.common.fs.RedisConstant;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.vo.fs.AgentNotifyVo;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.hutool.extra.spring.SpringUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.ToHeader;
import java.text.ParseException;

@Slf4j
@Component
public class AgentNotifyHandle {

    @Resource
    private DynamicTask dynamicTask;

    public static final String AGENT_NOTIFY = RedisConstant.AGENT_NOTIFY;
    public static final String AGENT_NOTIFY_PRESENCE = RedisConstant.AGENT_NOTIFY_PRESENCE;

    @PostConstruct
    public void subscribe() {
        RedisUtils.subscribe(AGENT_NOTIFY, byte[].class, this::onMessage);
    }

    public void onMessage(byte[] body) {
        AgentNotifyVo event = (AgentNotifyVo) SerializationUtils.deserialize(body);
        if(event == null){
            log.error("[订阅消息]：消息接收异常！");
            return;
        }
        if(event.getType().equals(AgentNotifyVo.TypeEnum.PRESENCE.getValue())){
            if(event.getOperate().equals(AgentNotifyVo.OperateEnum.ADD.getValue())){
                addPresenceSubscribe(event.getAgentKey());
            }else if(event.getOperate().equals(AgentNotifyVo.OperateEnum.DEL.getValue())){
                delPresenceSubscribe(event.getAgentKey());
            }else {
                log.error("[订阅消息]：消息操作类型错误！");
            }
        }else {
            log.error("[订阅消息]：消息类型错误！");
        }
    }

    private void addPresenceSubscribe(String agentKey){
        SipServer sipServer = SpringUtil.getBean(SipServer.class);
        SIPCommanderForPlatform sipCommanderForPlatform = SpringUtil.getBean(SIPCommanderForPlatform.class);
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if(agentVoInfo == null){
            return;
        }
        String key = String.format("%s%s",AGENT_NOTIFY_PRESENCE, agentVoInfo.getAgentKey());
        if(dynamicTask.contains(key)){
            return;
        }
        dynamicTask.startCron(key, 1,590,()->{
            SIPRequest request = null;
            byte[] req = RedisUtils.getCacheObject(key);
            if(ObjectUtils.isNotEmpty(req)){
                request = (SIPRequest) SerializationUtils.deserialize(req);
            }
            SIPRequest sipRequest = null;
            try {
                sipRequest = sipCommanderForPlatform.presenceSubscribe(sipServer, agentVoInfo, request, eventResult -> {
                    ResponseEvent event = (ResponseEvent) eventResult.getEvent();
                    // 成功
                    log.info("[Presence订阅]成功： {}", agentVoInfo.getAgentKey());
                    ToHeader toHeader = (ToHeader)event.getResponse().getHeader(ToHeader.NAME);
                    byte[] o = RedisUtils.getCacheObject(key);
                    if(ObjectUtils.isEmpty(o)){
                        return;
                    }
                    SIPRequest rq = (SIPRequest) SerializationUtils.deserialize(o);
                    try {
                        rq.getToHeader().setTag(toHeader.getTag());
                        long expire = RedisUtils.getTimeToLive(key);
                        RedisUtils.setCacheObject(key,SerializationUtils.serialize(rq),Duration.ofMillis(expire));
                    } catch (ParseException e) {
                        log.info("[Presence订阅]成功： 但为request设置ToTag失败");
                        RedisUtils.deleteObject(key);
                    }

                },eventResult -> {
                    RedisUtils.deleteObject(key);
                    // 失败
                    log.warn("[Presence订阅]失败，信令发送失败： {}-{} ", agentVoInfo.getAgentKey(), eventResult.getMsg());
                });
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[Presence发送失败] 目录订阅: {}", e.getMessage());
            }
            if (sipRequest != null) {
                RedisUtils.setCacheObject(key,SerializationUtils.serialize(sipRequest),Duration.ofSeconds(590+ SipConstant.DELAY_TIME));
            }

        });
    }

    private void delPresenceSubscribe(String agentKey){
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if(agentVoInfo == null){
            return;
        }
        log.info("[移除目录订阅]: {}", agentVoInfo.getAgentKey());
        String key = String.format("%s%s",AGENT_NOTIFY_PRESENCE, agentVoInfo.getAgentKey());
        dynamicTask.stop(key);
        RedisUtils.deleteObject(key);
    }

}
