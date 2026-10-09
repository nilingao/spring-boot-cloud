package cn.com.nla.common.freeswitch.client.sip.listener.response.impl;

import cn.com.nla.common.freeswitch.client.sip.listener.response.AbstractSipResponseEvent;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.sip.PlatformRegisterManager;
import cn.com.nla.common.freeswitch.service.SipService;
import cn.com.nla.common.freeswitch.vo.sip.PlatformRegisterInfo;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.time.LocalDateTime;

/**
 * @description:Register响应处理器
 */
@Slf4j
@Component
public class RegisterResponseProcessor extends AbstractSipResponseEvent {

    @Override
    public String getMethod() {
        return Request.REGISTER;
    }

    @Override
    public void process(ResponseEvent evt) {
        SIPResponse response = (SIPResponse)evt.getResponse();
        String callId = response.getCallIdHeader().getCallId();

        PlatformRegisterManager platformRegisterManager = RedisService.getPlatformRegisterManager();
        PlatformRegisterInfo platformRegisterInfo = platformRegisterManager.queryPlatformRegisterInfo(callId);
        if (platformRegisterInfo == null) {
            log.info(String.format("[国标级联]未找到callId： %s 的注册/注销平台id", callId ));
            return;
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(platformRegisterInfo.getAgentKey());
        String action = platformRegisterInfo.isRegister() ? "注册" : "注销";
        log.info(String.format("[国标级联]%s %S响应,%s ", action, response.getStatusCode(), platformRegisterInfo.getAgentKey() ));
        if (agentVoInfo == null) {
            log.warn(String.format("[国标级联]收到 %s %s的%S请求, 但是平台信息未查询到 !!!", platformRegisterInfo.getAgentKey(), action, response.getStatusCode()));
            return;
        }
        SipTransactionInfo sipTransactionInfo = RedisService.getSipTransactionManager().findParentPlatform(agentVoInfo.getAgentKey());
        if(sipTransactionInfo == null){
            log.warn(String.format("[国标级联]收到 %s %s的%S请求, 但未获取平台注册信息 等待重新注册!!!", agentVoInfo.getAgentKey(), action, response.getStatusCode()));
            return;
        }
        if (response.getStatusCode() == Response.UNAUTHORIZED) {
            WWWAuthenticateHeader www = (WWWAuthenticateHeader)response.getHeader(WWWAuthenticateHeader.NAME);
            sipTransactionInfo.sipTransactionInfo(response);
            RedisService.getSipTransactionManager().putParentPlatform(agentVoInfo.getAgentKey(),sipTransactionInfo);
            try {
                sipCommanderForPlatform.register(sipServer, agentVoInfo, www,  platformRegisterInfo.isRegister(),null,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 国标级联 再次注册: {}", e.getMessage());
            }
        }else if (response.getStatusCode() == Response.OK){
            agentVoInfo.setRenewTime(LocalDateTime.now());//续订时间
            if (platformRegisterInfo.isRegister()) {
                sipTransactionInfo.sipTransactionInfo(response);
                sipTransactionInfo.setRegisterAliveReply(0);
                SipService.getParentPlatformService().online(agentVoInfo,sipTransactionInfo);
            }else {
                SipService.getParentPlatformService().offline(agentVoInfo);
            }
            // 注册/注销成功移除缓存的信息
            platformRegisterManager.delPlatformRegisterInfo(callId);
        }
    }
}
