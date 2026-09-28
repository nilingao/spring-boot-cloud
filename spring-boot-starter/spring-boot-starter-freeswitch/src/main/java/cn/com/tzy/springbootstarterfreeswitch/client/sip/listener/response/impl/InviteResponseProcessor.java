package cn.com.tzy.springbootstarterfreeswitch.client.sip.listener.response.impl;

import cn.com.tzy.springbootcomm.utils.DynamicTask;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.listener.response.AbstractSipResponseEvent;
import cn.com.tzy.springbootstarterfreeswitch.client.sip.utils.SipUtils;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstarterfreeswitch.vo.sip.DeviceRawContent;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 处理 INVITE 响应（纯 SIP 模式）。
 * - 407: 代理认证，用已保存的原始请求重新发起带认证头的 INVITE。
 * - 200: 发送 ACK，完成媒体协商（媒体由 FreeSWITCH 与坐席 SIP 设备直接处理）。
 * ZLM 推流逻辑已移除。
 */
@Log4j2
@Component
public class InviteResponseProcessor extends AbstractSipResponseEvent {

    @Resource
    private DynamicTask dynamicTask;

    @Override
    public String getMethod() { return Request.INVITE; }

    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse) evt.getResponse();
            int statusCode = response.getStatusCode();

            // 407 代理认证
            if (statusCode == Response.PROXY_AUTHENTICATION_REQUIRED) {
                String callId = response.getCallId().getCallId();
                ProxyAuthenticateHeader header = (ProxyAuthenticateHeader) response.getHeader(ProxyAuthenticateHeader.NAME);
                if (header == null) {
                    log.error("[INVITE 407] 未获取认证参数 callId: {}", callId);
                    return;
                }
                SIPRequest sipRequest = RedisService.getAgentInfoManager().getCallPhone(callId);
                if (sipRequest == null) {
                    log.error("[INVITE 407] 未获取缓存请求值 callId: {}", callId);
                    return;
                }
                String userId = SipUtils.getUserIdFromHeader(sipRequest);
                AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().getSip(userId);
                if (agentVoInfo == null) {
                    log.error("[INVITE 407] 未获取客服信息 agentSip: {}", userId);
                    return;
                }
                sipCommanderForPlatform.callPhone(sipServer, agentVoInfo, header, sipRequest, response);
                return;
            }

            if (statusCode != Response.OK) {
                log.error("[INVITE 响应状态码错误] statusCode: {}", statusCode);
                return;
            }

            // 200 OK — 发送 ACK
            sipCommander.sendAckMessage(sipServer, response, null, error ->
                    log.info("[INVITE ACK 异常] {}", error.getMsg()));

            // 更新通话状态
            String agentSip = SipUtils.getUserIdFromHeader(response.getFromHeader());
            AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().getSip(agentSip);
            if (agentVoInfo == null) {
                log.error("[INVITE 200 OK] 坐席 Sip:{} 未上线", agentSip);
                return;
            }
            String callId = response.getCallId().getCallId();
            // 记录 SIP 消息到事务（供 BYE 时使用）
            RedisService.getSsrcTransactionManager().put(
                    agentVoInfo.getAgentKey(), callId, null, callId, null, null,
                    response, null);

            DeviceRawContent deviceRawContent = SipUtils.handleDeviceRawContent(response);
            log.info("[INVITE 200 OK] 坐席 {} 呼叫建立完成, callId: {}, hasVideo: {}",
                    agentVoInfo.getAgentKey(), callId,
                    deviceRawContent != null && deviceRawContent.getVideoInfo() != null);
        } catch (Exception e) {
            log.error("[INVITE 响应处理异常] {}", e.getMessage(), e);
        }
    }
}
