package cn.com.nla.common.video.core.sip.listener.event.response.impl;

import cn.com.nla.common.video.basic.vo.sip.PlatformRegisterInfo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.PlatformRegisterManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.sip.listener.event.response.AbstractSipResponseEvent;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;

import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * @description:Register响应处理器
 */
@Slf4j
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
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();

        PlatformRegisterInfo platformRegisterInfo = platformRegisterManager.queryPlatformRegisterInfo(callId);
        if (platformRegisterInfo == null) {
            log.info(String.format("[国标级联]未找到callId： %s 的注册/注销平台id", callId ));
            return;
        }

        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(platformRegisterInfo.getPlatformId());
        String action = platformRegisterInfo.isRegister() ? "注册" : "注销";
        log.info(String.format("[国标级联]%s %S响应,%s ", action, response.getStatusCode(), platformRegisterInfo.getPlatformId() ));
        if (parentPlatformVo == null) {
            log.warn(String.format("[国标级联]收到 %s %s的%S请求, 但是平台信息未查询到 !!!", platformRegisterInfo.getPlatformId(), action, response.getStatusCode()));
            return;
        }
        SipTransactionInfo sipTransactionInfo = RedisService.getSipTransactionManager().findParentPlatform(parentPlatformVo.getServerGbId());
        if(sipTransactionInfo == null){
            log.warn(String.format("[国标级联]收到 %s %s的%S请求, 但未获取平台注册信息 等待重新注册!!!", platformRegisterInfo.getPlatformId(), action, response.getStatusCode()));
            return;
        }
        if (response.getStatusCode() == Response.UNAUTHORIZED) {
            WWWAuthenticateHeader www = (WWWAuthenticateHeader)response.getHeader(WWWAuthenticateHeader.NAME);
            sipTransactionInfo.sipTransactionInfo(response);
            RedisService.getSipTransactionManager().putParentPlatform(parentPlatformVo.getServerGbId(),sipTransactionInfo);
            try {
                sipCommanderForPlatform.register(sipServer, parentPlatformVo, www,  platformRegisterInfo.isRegister(),null,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 国标级联 再次注册: {}", e.getMessage());
            }
        }else if (response.getStatusCode() == Response.OK){
            if (platformRegisterInfo.isRegister()) {
                sipTransactionInfo.sipTransactionInfo(response);
                sipTransactionInfo.setRegisterAliveReply(0);
                parentPlatformVoService.online(parentPlatformVo, sipTransactionInfo);
            }else {
                parentPlatformVoService.offline(parentPlatformVo);
            }
            // 注册/注销成功移除缓存的信息
            platformRegisterManager.delPlatformRegisterInfo(callId);
        }
    }
}
