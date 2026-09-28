package cn.com.tzy.springbootfs.config.socket.event.agent;

import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootstarterfreeswitch.common.socket.AgentCommon;
import cn.com.tzy.springbootfs.config.socket.common.agent.AgentInCallPhoneData;
import cn.com.tzy.springbootfs.config.socket.namespace.agent.AgentNamespace;
import cn.com.tzy.springbootfs.service.fs.AgentService;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.SipServer;
import cn.com.tzy.springbootstarterfreeswitch.enums.sip.VideoStreamType;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstartersocketio.pool.EventListener;
import cn.com.tzy.springbootstartersocketio.pool.NamespaceListener;
import cn.com.tzy.springbootstartervideobasic.enums.InviteErrorCode;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 客服拨打电话（纯 SIP 模式）。
 * 不再依赖 ZLM 流媒体，直接通过 SIP INVITE 发起呼叫。
 * SDP 由本服务构建并透传给 FreeSWITCH，RTP 媒体由客户端 SIP 设备与 FS 直接协商。
 */
@Log4j2
@Component
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
        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            String.format("客服 %s 未登录", agentKey)));
            return;
        }

        String caller = data.getCaller();
        String typeName = data.getType() != null
                ? data.getType()
                : VideoStreamType.CALL_AUDIO_PHONE.getCallName();

        // 构建最小 SDP：告知 FreeSWITCH 本端 SIP 服务的 IP，RTP 端口 0 表示延迟协商
        String localIp = sipServer.getLocalIp(agentVoInfo.getFsHost());
        String sdp = buildMinimalSdp(localIp);

        agentService.callPhone(typeName, sipServer, agentVoInfo, caller, sdp, null, (code, msg, vo) ->
                client.sendEvent(AgentCommon.AGENT_OUT_CALL_PHONE,
                        RestResult.result(code, msg, vo)));
    }

    /**
     * 构建用于向 FreeSWITCH 发起 INVITE 的最小 SDP。
     * 真实媒体 IP/端口将在 FS 回复 200 OK 后通过 SDP 协商确定。
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
