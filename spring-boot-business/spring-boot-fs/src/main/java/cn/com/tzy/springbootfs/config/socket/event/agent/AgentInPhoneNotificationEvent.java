package cn.com.tzy.springbootfs.config.socket.event.agent;

import cn.com.tzy.springbootcomm.common.enumcom.ConstEnum;
import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootcomm.utils.ValidatorUtils;
import cn.com.tzy.springbootfs.config.socket.common.agent.AgentInPhoneNotificationData;
import cn.com.tzy.springbootfs.config.socket.namespace.agent.AgentNamespace;
import cn.com.tzy.springbootfs.service.fs.AgentService;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.SipServer;
import cn.com.tzy.springbootstarterfreeswitch.common.socket.AgentCommon;
import cn.com.tzy.springbootstarterfreeswitch.enums.sip.VideoStreamType;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstarterfreeswitch.redis.subscribe.sip.message.AgentSubscribeHandle;
import cn.com.tzy.springbootstarterfreeswitch.vo.result.RestResultEvent;
import cn.com.tzy.springbootstarterredis.utils.RedisUtils;
import cn.com.tzy.springbootstartersocketio.pool.EventListener;
import cn.com.tzy.springbootstartersocketio.pool.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import java.util.Set;

/**
 * 坐席来电通知事件（纯 SIP 模式）。
 * 坐席选择接听或挂断来电时触发此事件。
 * 不再依赖 ZLM，接听动作通过 SIP callBackId 路径将坐席 SIP 设备连接到呼叫。
 */
@Log4j2
@Component
public class AgentInPhoneNotificationEvent implements EventListener<AgentInPhoneNotificationData> {

    private final AgentNamespace agentNamespace;
    @Resource
    private SipServer sipServer;
    @Resource
    private AgentService agentService;

    public AgentInPhoneNotificationEvent(AgentNamespace agentNamespace) {
        this.agentNamespace = agentNamespace;
    }

    @Override
    public Class<AgentInPhoneNotificationData> getEventClass() {
        return AgentInPhoneNotificationData.class;
    }

    @Override
    public String getEventName() {
        return AgentCommon.AGENT_IN_CALL_NOTIFICATION;
    }

    @Override
    public NamespaceListener getNamespace() {
        return agentNamespace;
    }

    @Override
    public void onData(SocketIOClient client, AgentInPhoneNotificationData data, AckRequest ackSender) {
        Set<ConstraintViolation<AgentInPhoneNotificationData>> violations = ValidatorUtils.validateFast(data);
        if (!violations.isEmpty()) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            violations.iterator().next().getMessage()));
            return;
        }

        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            String.format("客服 %s 未登录", agentKey)));
            return;
        }
        if (data.getType() == null || (data.getType() != 1 && data.getType() != 2)) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            String.format("客服 %s 操作类型错误", agentKey)));
            return;
        }

        if (data.getType() == 1) {
            // 坐席挂断：通知下游放弃本次来电
            String key = String.format("%s%s",
                    AgentSubscribeHandle.VIDEO_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER, data.getCallId());
            RedisUtils.redisTemplate.convertAndSend(key,
                    SerializationUtils.serialize(new RestResultEvent(
                            RespCode.CODE_0.getValue(),
                            String.format("坐席:[%s]挂断电话操作", agentKey), null)));
            return;
        }

        // type == 2：坐席接听，通过 SIP callBackId 路径将坐席连接到呼叫
        String typeName = data.getOnVideo() != null
                && data.getOnVideo() == ConstEnum.Flag.YES.getValue()
                ? VideoStreamType.CALL_VIDEO_PHONE.getCallName()
                : VideoStreamType.CALL_AUDIO_PHONE.getCallName();

        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        String sdp = buildMinimalSdp(localIp);

        agentService.callPhone(typeName, sipServer, agentVoInfo, null, sdp, data.getCallId(),
                (code, msg, vo) -> {
                    client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE,
                            RestResult.result(code, msg, vo));
                    if (code != RespCode.CODE_0.getValue()) {
                        String key = String.format("%s%s",
                                AgentSubscribeHandle.VIDEO_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER,
                                data.getCallId());
                        RedisUtils.redisTemplate.convertAndSend(key,
                                SerializationUtils.serialize(new RestResultEvent(
                                        RespCode.CODE_0.getValue(),
                                        String.format("坐席:[%s]接听电话操作", agentVoInfo.getAgentKey()),
                                        null)));
                    }
                });
    }

    private static String buildMinimalSdp(String localIp) {
        return "v=0\r\n" +
                "o=- 0 0 IN IP4 " + localIp + "\r\n" +
                "s=SIP Call\r\n" +
                "c=IN IP4 " + localIp + "\r\n" +
                "t=0 0\r\n" +
                "m=audio 0 RTP/AVP 0 8 101\r\n" +
                "a=rtpmap:0 PCMU/8000\r\n" +
                "a=rtpmap:8 PCMA/8000\r\n" +
                "a=rtpmap:101 telephone-event/8000\r\n" +
                "a=sendrecv\r\n";
    }
}
