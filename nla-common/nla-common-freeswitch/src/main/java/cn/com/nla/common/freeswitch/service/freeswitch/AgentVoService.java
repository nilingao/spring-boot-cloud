package cn.com.nla.common.freeswitch.service.freeswitch;

import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.utils.DynamicTask;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.sip.SipTransactionManager;
import cn.com.nla.common.freeswitch.redis.impl.sip.SsrcTransactionManager;
import cn.com.nla.common.freeswitch.vo.sip.Address;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import cn.hutool.extra.spring.SpringUtil;
import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 坐席运行时数据服务抽象基类。
 * 纯 SIP 模式：不依赖 ZLM，媒体通道由客户端 SIP 设备与 FreeSWITCH 直接协商。
 */
@Slf4j
public abstract class AgentVoService {

    public abstract AgentVoInfo getAgentBySip(String sip);
    public abstract AgentVoInfo getAgentByKey(String agentKey);
    public abstract void stopStream(String callId);
    public abstract AgentVoInfo getAgentByCompanyCode(String company, String agentCode);
    public abstract AgentVoInfo findAgentId(String id);
    public abstract void save(AgentVoInfo entity);
    public abstract void updateStatus(Long id, boolean online);
    public abstract void startPlay(String agentCode, String stream);
    public abstract void stopPlay(String agentCode);

    @Resource
    private SipConfigProperties sipConfigProperties;

    /**
     * 坐席 SIP 注册上线。
     * 更新 Redis 缓存，启动心跳超时任务。
     */
    public void online(AgentVoInfo agentVoInfo, SipTransactionInfo sipTransactionInfo) {
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();

        log.info("[设备上线] agentInfo：{}->{}", agentVoInfo.getAgentKey(), agentVoInfo.getRemoteAddress());
        if (agentVoInfo.getKeepTimeout() == null || agentVoInfo.getKeepTimeout() == 0) {
            agentVoInfo.setKeepTimeout(60);
        }
        if (sipTransactionInfo != null) {
            sipTransactionManager.putDevice(agentVoInfo.getAgentKey(), sipTransactionInfo);
        }
        agentVoInfo.setKeepaliveTime(LocalDateTime.now());
        AgentVoInfo existing = this.findAgentId(String.valueOf(agentVoInfo.getId()));
        if (existing == null) {
            agentVoInfo.setState(ConstEnum.Flag.YES.getValue());
            agentVoInfo.setAgentOnline(ConstEnum.Flag.YES.getValue());
            agentVoInfo.setRegisterTime(LocalDateTime.now());
            log.info("[设备上线,首次注册]: {}", agentVoInfo.getAgentKey());
            this.save(agentVoInfo);
        } else {
            if (agentVoInfo.getAgentOnline() == ConstEnum.Flag.NO.getValue()) {
                log.info("[设备上线]: {}", agentVoInfo.getAgentKey());
                agentVoInfo.setState(ConstEnum.Flag.YES.getValue());
                agentVoInfo.setAgentOnline(ConstEnum.Flag.YES.getValue());
                agentVoInfo.setRegisterTime(LocalDateTime.now());
            }
            this.save(agentVoInfo);
        }
        if (sipTransactionInfo != null) {
            String key = String.format("%s_%s", SipConstant.REGISTER_EXPIRE_TASK_KEY_PREFIX, agentVoInfo.getAgentKey());
            dynamicTask.startDelay(key, agentVoInfo.getKeepTimeout() + SipConstant.DELAY_TIME,
                    () -> offline(agentVoInfo.getAgentKey()));
            RedisService.getRegisterServerManager().putDevice(agentVoInfo.getAgentKey(),
                    agentVoInfo.getKeepTimeout() + SipConstant.DELAY_TIME,
                    Address.builder()
                            .agentKey(agentVoInfo.getAgentKey())
                            .ip(sipConfigProperties.getIp())
                            .port(sipConfigProperties.getPort())
                            .build());
        }
        RedisService.getAgentInfoManager().put(agentVoInfo);
    }

    /**
     * 坐席 SIP 注销下线。
     * 清理通话状态和 Redis 缓存；纯 SIP 模式不再关闭 ZLM 流。
     */
    public void offline(String agentKey) {
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        log.info("[设备下线] device：{}", agentKey);
        AgentVoInfo agentVoInfo = this.getAgentByKey(agentKey);
        if (agentVoInfo == null) {
            log.warn("[设备下线] 未获取设备信息 agentKey：{}", agentKey);
            return;
        }
        String key = String.format("%s_%s", SipConstant.REGISTER_EXPIRE_TASK_KEY_PREFIX, agentVoInfo.getAgentKey());
        dynamicTask.stop(key);
        this.updateStatus(agentVoInfo.getId(), false);
        // 清理所有活跃通话状态
        ssrcTransactionManager.remove(agentKey, null, null, null);
        RedisService.getRegisterServerManager().delDevice(agentKey);
        RedisService.getAgentInfoManager().del(agentKey);
    }
}
