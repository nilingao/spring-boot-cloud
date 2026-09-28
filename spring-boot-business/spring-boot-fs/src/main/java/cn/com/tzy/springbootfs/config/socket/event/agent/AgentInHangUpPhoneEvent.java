package cn.com.tzy.springbootfs.config.socket.event.agent;

import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootfs.config.socket.common.agent.AgentInHangUpPhoneData;
import cn.com.tzy.springbootfs.config.socket.namespace.agent.AgentNamespace;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.SipServer;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.tzy.springbootstarterfreeswitch.common.socket.AgentCommon;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstarterfreeswitch.vo.sip.SsrcTransaction;
import cn.com.tzy.springbootstartersocketio.pool.EventListener;
import cn.com.tzy.springbootstartersocketio.pool.NamespaceListener;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

/**
 * 坐席主动挂机。
 * 纯 SIP 模式：通过 SsrcTransactionManager 按 agentKey 找到当前通话的 callId，
 * 然后向 FreeSWITCH 发送 SIP BYE。不再依赖 ZLM InviteStreamManager。
 */
@Log4j2
@Component
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
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            String.format("客服 %s 未登录", agentKey)));
            return;
        }

        // 从 SsrcTransactionManager 按 agentKey 查找当前通话的 callId
        List<SsrcTransaction> transactions = RedisService.getSsrcTransactionManager()
                .getParamAll(agentKey, null, null, null);
        if (transactions == null || transactions.isEmpty()) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            String.format("客服 %s 当前无活跃通话", agentKey)));
            return;
        }

        // 对每一个活跃 callId 发送 BYE（通常只有一个）
        boolean anySuccess = false;
        for (SsrcTransaction tx : transactions) {
            String callId = tx.getCallId();
            if (callId == null || callId.isEmpty()) {
                continue;
            }
            try {
                sipCommanderForPlatform.streamByeCmd(sipServer, agentVoInfo,
                        null, null, callId, null,
                        ok -> log.info("[挂机成功] 坐席: {} callId: {}", agentKey, callId),
                        err -> log.warn("[挂机BYE失败] 坐席: {} callId: {} 原因: {}", agentKey, callId, err.getMsg()));
                anySuccess = true;
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[挂机] 发送BYE失败 坐席: {} callId: {} 错误: {}", agentKey, callId, e.getMessage());
            }
        }

        if (anySuccess) {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                    RestResult.result(RespCode.CODE_0.getValue(), "挂机指令已发送"));
        } else {
            client.sendEvent(AgentCommon.AGENT_OUT_HANG_UP_PHONE,
                    RestResult.result(RespCode.CODE_2.getValue(),
                            String.format("客服 %s BYE发送失败", agentKey)));
        }
    }
}
