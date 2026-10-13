package cn.com.nla.callcenter.socket.event;

import cn.com.nla.callcenter.socket.AgentNamespace;
import cn.com.nla.callcenter.socket.message.AgentInHangUpPhoneData;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.freeswitch.common.socket.AgentCommon;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.vo.sip.SsrcTransaction;
import cn.com.nla.common.socketio.basic.listener.EventListener;
import cn.com.nla.common.socketio.basic.listener.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 坐席挂断入站事件 {@code AGENT:IN:HANG_UP_PHONE}。
 *
 * <p>按坐席账户查当前全部活跃通话，逐个向 FreeSWITCH 发送 SIP BYE，结果回执坐席。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@ConditionalOnProperty(name = {"socket-io.enabled", "freeswitch.enabled"}, havingValue = "true")
public class AgentInHangUpPhoneEvent implements EventListener<AgentInHangUpPhoneData> {

    private final AgentNamespace agentNamespace;

    @Resource
    private SipServer sipServer;

    @Resource
    private SIPCommanderForPlatform sipCommanderForPlatform;

    public AgentInHangUpPhoneEvent(AgentNamespace agentNamespace) {
        this.agentNamespace = agentNamespace;
    }

    @Override
    public Class<AgentInHangUpPhoneData> getEventClass() {
        return AgentInHangUpPhoneData.class;
    }

    @Override
    public String getEventName() {
        return AgentCommon.AGENT_IN_HANG_UP_PHONE;
    }

    @Override
    public NamespaceListener getNamespace() {
        return agentNamespace;
    }

    @Override
    public void onData(SocketIOClient client, AgentInHangUpPhoneData data, AckRequest request) {
        String agentKey = RedisService.getAgentInfoManager().getAgentKey(client.getSessionId().toString());
        if (agentKey == null || agentKey.isBlank()) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), "坐席连接未完成认证"));
            return;
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 %s 未登录", agentKey)));
            return;
        }

        // 从 SsrcTransactionManager 按 agentKey 查找当前通话的 callId
        List<SsrcTransaction> transactions = RedisService.getSsrcTransactionManager()
            .getParamAll(agentKey, null, null, null);
        if (transactions == null || transactions.isEmpty()) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 %s 当前无活跃通话", agentKey)));
            return;
        }

        // 对每一个活跃 callId 发送 BYE（通常只有一个）
        boolean anySuccess = false;
        for (SsrcTransaction transaction : transactions) {
            String callId = transaction.getCallId();
            if (callId == null || callId.isEmpty()) {
                continue;
            }
            try {
                sipCommanderForPlatform.streamByeCmd(sipServer, agentVoInfo, null, null, callId, null,
                    ok -> log.info("[挂机成功] 坐席: {} callId: {}", agentKey, callId),
                    err -> log.warn("[挂机BYE失败] 坐席: {} callId: {} 原因: {}", agentKey, callId, err.getMsg()));
                anySuccess = true;
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[挂机] 发送BYE失败 坐席: {} callId: {} 错误: {}", agentKey, callId, e.getMessage());
            }
        }

        if (anySuccess) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                new RestResultEvent(RespCode.CODE_0.getValue(), "挂机指令已发送"));
        } else {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                new RestResultEvent(RespCode.CODE_2.getValue(), String.format("客服 %s BYE发送失败", agentKey)));
        }
    }
}
