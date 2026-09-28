package cn.com.tzy.springbootstarterfreeswitch.client.sip.listener.request.impl;

import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.listener.request.AbstractSipRequestEvent;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.tzy.springbootstarterfreeswitch.common.socket.AgentCommon;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstarterfreeswitch.service.FsService;
import cn.com.tzy.springbootstarterfreeswitch.vo.sip.SsrcTransaction;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.HeaderAddress;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;

/**
 * 处理 SIP BYE 请求（对方主动挂机）。
 * 纯 SIP 模式：不再调用 ZLM，仅清理呼叫状态并通知坐席。
 */
@Log4j2
@Component
public class ByeRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {

    @Override
    public String getMethod() { return Request.BYE; }

    @Override
    public void process(RequestEvent evt) {
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK, null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[回复BYE信息失败] {}", e.getMessage());
        }

        CallIdHeader callIdHeader = (CallIdHeader) evt.getRequest().getHeader(CallIdHeader.NAME);
        String callId = callIdHeader.getCallId();
        String agentSip = ((SipURI) ((HeaderAddress) evt.getRequest().getHeader(ToHeader.NAME))
                .getAddress().getURI()).getUser();
        log.info("[收到BYE] callId: {}, to: {}", callId, agentSip);

        // 通过 callId 查找坐席并通知挂机
        List<SsrcTransaction> transactions = RedisService.getSsrcTransactionManager()
                .getParamAll(null, callId, null, null);
        if (transactions != null && !transactions.isEmpty()) {
            String agentKey = transactions.get(0).getAgentKey();
            AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
            if (agentVoInfo != null) {
                FsService.getSendAgentMessage().sendMessage(
                        AgentCommon.SOCKET_AGENT, AgentCommon.AGENT_OUT_HANG_UP_PHONE, agentKey,
                        RestResult.result(RespCode.CODE_0.getValue(), "对方挂机"));
            }
        }
        // 清理通话状态
        RedisService.getSsrcTransactionManager().remove(agentSip, null, callId, null);
    }
}
