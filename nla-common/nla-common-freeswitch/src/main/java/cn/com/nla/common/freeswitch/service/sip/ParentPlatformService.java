package cn.com.nla.common.freeswitch.service.sip;

import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.enums.fs.LoginTypeEnum;
import cn.com.nla.common.freeswitch.model.bean.ConfigModel;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.sip.SipTransactionManager;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.freeswitch.service.FsService;
import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.freeswitch.vo.result.RestResultEvent;
import cn.com.nla.common.freeswitch.vo.sip.Address;
import cn.com.nla.common.freeswitch.vo.sip.EventResult;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 上级 SIP 平台（FreeSWITCH）交互抽象服务。
 * 纯 SIP 模式：不再依赖 ZLM，仅管理 SIP 注册/注销/心跳等信令。
 */
@Slf4j
public abstract class ParentPlatformService {

    @Resource private SipServer sipServer;
    @Resource private DynamicTask dynamicTask;
    @Resource private SIPCommanderForPlatform sipCommanderForPlatform;

    public abstract ConfigModel random();

    public void login(AgentVoInfo agentVoInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) {
        RedisService.getRegisterServerManager().putPlatform(agentVoInfo.getAgentKey(),
                agentVoInfo.getKeepTimeout() + SipConstant.DELAY_TIME,
                Address.builder().agentKey(agentVoInfo.getAgentKey())
                        .ip(sipServer.getSipConfigProperties().getIp()).port(sipServer.getSipConfigProperties().getPort()).build());
        RedisService.getSipTransactionManager().delParentPlatform(agentVoInfo.getAgentKey());
        register(agentVoInfo,
                ok -> { if (okEvent != null) okEvent.response(ok); },
                error -> {
                    log.info("[SIP注册] {}, 发起注册失败", agentVoInfo.getAgentKey());
                    if (errorEvent != null) errorEvent.response(error);
                });
    }

    private void register(AgentVoInfo agentVoInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) {
        SipTransactionInfo sipTransactionInfo = RedisService.getSipTransactionManager()
                .findParentPlatform(agentVoInfo.getAgentKey());
        if (sipTransactionInfo == null) {
            sipTransactionInfo = new SipTransactionInfo();
            RedisService.getSipTransactionManager().putParentPlatform(agentVoInfo.getAgentKey(), sipTransactionInfo);
        }
        if (sipTransactionInfo.getRegisterAliveReply() > 3) {
            sipTransactionInfo.setRegisterAliveReply(0);
            sipTransactionInfo.setKeepAliveReply(0);
            RedisService.getSipTransactionManager().putParentPlatform(agentVoInfo.getAgentKey(), sipTransactionInfo);
            if (errorEvent != null)
                errorEvent.response(new EventResult<>(new RestResultEvent(RespCode.CODE_2.getValue(),
                        String.format("[SIP注册] %s 注册三次未成功，放弃", agentVoInfo.getAgentKey()))));
            offline(agentVoInfo);
            return;
        }
        sipTransactionInfo.setKeepAliveReply(0);
        sipTransactionInfo.setRegisterAliveReply(sipTransactionInfo.getRegisterAliveReply() + 1);
        RedisService.getSipTransactionManager().putParentPlatform(agentVoInfo.getAgentKey(), sipTransactionInfo);
        agentVoInfo.setLoginType(LoginTypeEnum.SOCKET.getType());
        try {
            sipCommanderForPlatform.register(sipServer, agentVoInfo, null, true,
                    ok -> { if (okEvent != null) okEvent.response(ok); },
                    error -> {
                        offline(agentVoInfo);
                        if (errorEvent != null) errorEvent.response(error);
                    });
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[SIP注册失败] {}", e.getMessage());
        }
    }

    public void unregister(AgentVoInfo parentPlatformVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) {
        parentPlatformVo.setLoginType(LoginTypeEnum.SOCKET.getType());
        try {
            sipCommanderForPlatform.unregister(sipServer, parentPlatformVo,
                    ok -> { if (okEvent != null) okEvent.response(ok); },
                    eventResult -> {
                        log.info("[SIP注销] {}, 失败", parentPlatformVo.getAgentKey());
                        if (errorEvent != null) errorEvent.response(eventResult);
                    });
        } catch (InvalidArgumentException | ParseException | SipException e) {
            log.error("[SIP注销失败] {}", e.getMessage());
            offline(parentPlatformVo);
        }
    }

    public void online(AgentVoInfo agentVoInfo, SipTransactionInfo sipTransactionInfo) {
        log.info("[SIP上线] {}", agentVoInfo.getAgentKey());
        if (sipTransactionInfo != null) {
            SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();
            sipTransactionManager.putParentPlatform(agentVoInfo.getAgentKey(), sipTransactionInfo);
        }
        FsService.getAgentService().online(agentVoInfo, null);
        this.registerTask(agentVoInfo, false);
        RedisService.getRegisterServerManager().putPlatform(agentVoInfo.getAgentKey(),
                agentVoInfo.getKeepTimeout() + SipConstant.DELAY_TIME,
                Address.builder().agentKey(agentVoInfo.getAgentKey())
                        .ip(sipServer.getSipConfigProperties().getIp()).port(sipServer.getSipConfigProperties().getPort()).build());
        RedisService.getAgentNotifySubscribeManager().addPresenceSubscribe(agentVoInfo);
    }

    /**
     * 平台离线。纯 SIP 模式：仅清理 SIP 注册状态和通话事务，不再停止 ZLM 推流。
     */
    public void offline(AgentVoInfo agentVoInfo) {
        log.info("[SIP离线] {}", agentVoInfo.getAgentKey());
        RedisService.getAgentNotifySubscribeManager().removePresenceSubscribe(agentVoInfo);
        RedisService.getRegisterServerManager().delPlatform(agentVoInfo.getAgentKey());
        // 清理活跃通话状态（纯 SIP 模式不需要停止 ZLM 流）
        RedisService.getSsrcTransactionManager().remove(agentVoInfo.getAgentKey(), null, null, null);
        this.registerTask(agentVoInfo, true);
        FsService.getAgentService().offline(agentVoInfo.getAgentKey());
    }

    private void registerTask(AgentVoInfo agentVoInfo, boolean isClose) {
        String key = SipConstant.PLATFORM_REGISTER_TASK_CATCH_PREFIX + agentVoInfo.getAgentKey();
        if (isClose) { dynamicTask.stop(key); return; }
        AgentVoInfo agentVo = RedisService.getAgentInfoManager().get(agentVoInfo.getAgentKey());
        if (agentVo == null) {
            log.error("[SIP注册续订] 平台 {} 未获取客服缓存", agentVoInfo.getAgentKey());
            dynamicTask.stop(key);
            return;
        }
        if (dynamicTask.isAlive(key)) return;
        int expires = Math.max(agentVo.getExpires(), 20) - SipConstant.DELAY_TIME;
        dynamicTask.startCron(key, expires, expires, () -> {
            log.info("[SIP注册续订] 平台 {} 即将到期，开始续订", agentVo.getAgentKey());
            register(agentVo, null, error -> {
                log.error("[SIP注册续订] 失败 code:{} msg:{}", error.getStatusCode(), error.getMsg());
                dynamicTask.stop(key);
            });
        });
    }
}
