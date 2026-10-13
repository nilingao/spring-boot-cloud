package cn.com.nla.callcenter.socket.event;

import cn.com.nla.callcenter.socket.AgentNamespace;
import cn.com.nla.callcenter.socket.message.AgentInPhoneNotificationData;
import cn.com.nla.callcenter.spi.AgentService;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.enums.sip.VideoStreamType;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.AgentSubscribeHandle;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

/**
 * 坐席来电操作入站事件 {@code AGENT:IN:PHONE_NOTIFICATION}。
 *
 * <p>type 为 1 时坐席放弃本次来电，向错误订阅通道发布挂断信号中断来电流程；
 * type 为 2 时坐席接听，通过 callBackId 路径把坐席 SIP 连接到该来电，
 * 接听失败同样发布信号放弃来电。参数校验对齐旧 validateFast 的回执协议（错误码 2 + 首个失败提示）。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"socket-io.enabled", "freeswitch.enabled"}, havingValue = "true")
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
        if (data.getType() == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                new RestResultEvent(RespCode.CODE_2.getValue(), "未获取操作类型"));
            return;
        }
        if (data.getCallId() == null || data.getCallId().isBlank()) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                new RestResultEvent(RespCode.CODE_2.getValue(), "未获取CallId编号"));
            return;
        }
        if (data.getOnVideo() == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                new RestResultEvent(RespCode.CODE_2.getValue(), "未获取接听方式"));
            return;
        }

        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 %s 未登录", agentKey)));
            return;
        }
        if (data.getType() != 1 && data.getType() != 2) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_NOTIFICATION,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 %s 操作类型错误", agentKey)));
            return;
        }

        if (data.getType() == 1) {
            // 坐席挂断：通知下游放弃本次来电
            String key = AgentSubscribeHandle.VIDEO_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER + data.getCallId();
            RedisUtils.publish(key, SerializationUtils.serialize(new RestResultEvent(
                RespCode.CODE_0.getValue(), String.format("坐席:[%s]挂断电话操作", agentKey), null)));
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
                client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE, new RestResultEvent(code, msg, vo));
                if (code != RespCode.CODE_0.getValue()) {
                    String key = AgentSubscribeHandle.VIDEO_AGENT_ERROR_EVENT_SUBSCRIBE_MANAGER + data.getCallId();
                    RedisUtils.publish(key, SerializationUtils.serialize(new RestResultEvent(
                        RespCode.CODE_0.getValue(), String.format("坐席:[%s]接听电话操作", agentVoInfo.getAgentKey()),
                        null)));
                }
            });
    }

    /**
     * 构建用于向 FreeSWITCH 发起 INVITE 的最小 SDP。
     * 真实媒体 IP/端口将在 FS 回复 200 OK 后通过 SDP 协商确定。
     *
     * @param localIp 本端 SIP 服务 IP
     * @return SDP 字符串
     */
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
