package cn.com.nla.callcenter.socket.event;

import cn.com.nla.callcenter.socket.AgentNamespace;
import cn.com.nla.callcenter.socket.message.AgentInCallPhoneData;
import cn.com.nla.callcenter.spi.AgentService;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.enums.sip.VideoStreamType;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


/**
 * 坐席外呼入站事件 {@code AGENT:IN:CALL_PHONE}（纯 SIP 模式）。
 *
 * <p>不依赖流媒体，直接通过 SIP INVITE 发起呼叫；SDP 由本服务构建最小模板透传给
 * FreeSWITCH，真实媒体地址在 FS 回复 200 OK 后协商。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"socket-io.enabled", "freeswitch.enabled"}, havingValue = "true")
public class AgentInCallPhoneEvent implements EventListener<AgentInCallPhoneData> {

    private final AgentNamespace agentNamespace;

    @Resource
    private SipServer sipServer;

    @Resource
    private AgentService agentService;

    public AgentInCallPhoneEvent(AgentNamespace agentNamespace) {
        this.agentNamespace = agentNamespace;
    }

    @Override
    public Class<AgentInCallPhoneData> getEventClass() {
        return AgentInCallPhoneData.class;
    }

    @Override
    public String getEventName() {
        return AgentCommon.AGENT_IN_CALL_PHONE;
    }

    @Override
    public NamespaceListener getNamespace() {
        return agentNamespace;
    }

    @Override
    public void onData(SocketIOClient client, AgentInCallPhoneData data, AckRequest request) {
        if (data == null || data.getCaller() == null || data.getCaller().isBlank()) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), "未获取被叫号码"));
            return;
        }
        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        if (agentKey == null || agentKey.isBlank()) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), "坐席连接未完成认证"));
            return;
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 %s 未登录", agentKey)));
            return;
        }

        String caller = data.getCaller().trim();
        String typeName = data.getType() != null
            ? data.getType()
            : VideoStreamType.CALL_AUDIO_PHONE.getCallName();

        // 构建最小 SDP：告知 FreeSWITCH 本端 SIP 服务的 IP，RTP 端口 0 表示延迟协商
        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        String sdp = buildMinimalSdp(localIp);

        agentService.callPhone(typeName, sipServer, agentVoInfo, caller, sdp, null,
            (code, msg, vo) -> client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE, new RestResultEvent(code, msg, vo)));
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
