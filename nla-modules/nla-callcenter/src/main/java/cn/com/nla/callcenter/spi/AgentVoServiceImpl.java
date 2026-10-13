package cn.com.nla.callcenter.spi;

import cn.com.nla.callcenter.domain.Agent;
import cn.com.nla.callcenter.domain.AgentSip;
import cn.com.nla.callcenter.mapper.AgentMapper;
import cn.com.nla.callcenter.mapper.AgentSipMapper;
import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.service.freeswitch.AgentVoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 坐席运行时数据协议侧实现。
 *
 * <p>迁移自旧 service.freeswitch.AgentVoServiceImpl：负责从库表加载坐席信息并与 Redis
 * 中的运行时缓存同步；纯 SIP 模式，媒体通道由客户端 SIP 设备与 FreeSWITCH 直接协商。
 * 旧 MapStruct AgentConvert 的同名隐式转换（companyId/groupId Long 与 String 互转）由
 * {@link FsSpiConvert} 承担。仅在 freeswitch.enabled=true 时注册，
 * 避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class AgentVoServiceImpl extends AgentVoService {

    private final AgentMapper agentMapper;
    private final AgentSipMapper agentSipMapper;

    /**
     * 按 SIP 号加载坐席。
     *
     * @param sip SIP 号码
     * @return 坐席运行时信息，未找到返回 null
     */
    @Override
    public AgentVoInfo getAgentBySip(String sip) {
        return buildAgentVoInfo(agentMapper.getAgentBySip(sip));
    }

    /**
     * 按坐席账户加载坐席。
     *
     * @param agentKey 坐席账户
     * @return 坐席运行时信息，未找到返回 null
     */
    @Override
    public AgentVoInfo getAgentByKey(String agentKey) {
        Agent agent = agentMapper.selectOne(
                Wrappers.<Agent>lambdaQuery().eq(Agent::getAgentKey, agentKey));
        return buildAgentVoInfo(agent);
    }

    /**
     * 挂断流（旧实现已迁移至 AgentServiceImpl，通过 SIP BYE 挂断）。
     *
     * @param callId 通话编号
     */
    @Override
    public void stopStream(String callId) {
        // 已移至 AgentServiceImpl.stopStream()，通过 SIP BYE 挂断
    }

    /**
     * 按企业与坐席分机号加载坐席。
     *
     * @param company   企业编号（协议层以字符串传递，SQL 隐式转换比较）
     * @param agentCode 坐席分机号
     * @return 坐席运行时信息，未找到返回 null
     */
    @Override
    public AgentVoInfo getAgentByCompanyCode(String company, String agentCode) {
        Agent agent = agentMapper.selectOne(Wrappers.<Agent>lambdaQuery()
                .eq(Agent::getCompanyId, company)
                .eq(Agent::getAgentCode, agentCode));
        return buildAgentVoInfo(agent);
    }

    /**
     * 按主键加载坐席。
     *
     * @param id 坐席主键
     * @return 坐席运行时信息，未找到返回 null
     */
    @Override
    public AgentVoInfo findAgentId(String id) {
        return buildAgentVoInfo(agentMapper.selectById(id));
    }

    /**
     * 保存坐席运行时信息（在线注册/状态同步落库）。
     *
     * @param entity 坐席运行时信息
     */
    @Override
    public void save(AgentVoInfo entity) {
        Agent agent = FsSpiConvert.to(entity, Agent.class);
        if (agent.getId() != null) {
            agentMapper.updateById(agent);
        } else {
            agentMapper.insert(agent);
        }
    }

    /**
     * 更新坐席在线状态（清空 host 并刷新注册/续订/心跳时间）。
     *
     * @param id     坐席主键
     * @param online 是否在线
     */
    @Override
    public void updateStatus(Long id, boolean online) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            return;
        }
        Agent update = new Agent();
        update.setId(agent.getId());
        update.setHost("");
        update.setState(online ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue());
        update.setRegisterTime(online ? LocalDateTime.now() : null);
        update.setRenewTime(online ? LocalDateTime.now() : null);
        update.setKeepaliveTime(online ? LocalDateTime.now() : null);
        agentMapper.updateById(update);
    }

    /**
     * 启动播放（纯 SIP 模式下由 FreeSWITCH 管理媒体流，无需本地启动播放）。
     *
     * @param agentCode 坐席分机号
     * @param stream    流标识
     */
    @Override
    public void startPlay(String agentCode, String stream) {
        // 纯 SIP 模式下由 FreeSWITCH 管理媒体流，无需本地启动播放
    }

    /**
     * 停止播放（纯 SIP 模式下由 FreeSWITCH 管理媒体流，无需本地停止播放）。
     *
     * @param agentCode 坐席分机号
     */
    @Override
    public void stopPlay(String agentCode) {
        // 纯 SIP 模式下由 FreeSWITCH 管理媒体流，无需本地停止播放
    }

    // ---- private helpers ----

    /**
     * 装配坐席运行时信息：补充绑定 SIP 号列表并复位缓存标记。
     *
     * @param agent 座席实体
     * @return 坐席运行时信息，实体为空时返回 null
     */
    private AgentVoInfo buildAgentVoInfo(Agent agent) {
        if (agent == null) {
            return null;
        }
        AgentVoInfo info = FsSpiConvert.to(agent, AgentVoInfo.class);
        List<AgentSip> agentSips = agentSipMapper.selectList(
                Wrappers.<AgentSip>lambdaQuery().eq(AgentSip::getAgentId, agent.getId()));
        if (!agentSips.isEmpty()) {
            info.setSipPhoneList(agentSips.stream().map(AgentSip::getSip).toList());
        }
        info.setAgentOnline(ConstEnum.Flag.NO.getValue());
        info.setSsrcCheck(ConstEnum.Flag.NO.getValue());
        return info;
    }
}
