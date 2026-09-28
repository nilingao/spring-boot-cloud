package cn.com.tzy.springbootfs.service.freeswitch;

import cn.com.tzy.springbootcomm.common.enumcom.ConstEnum;
import cn.com.tzy.springbootcomm.common.vo.RespCode;
import cn.com.tzy.springbootcomm.common.vo.RestResult;
import cn.com.tzy.springbootentity.dome.fs.Agent;
import cn.com.tzy.springbootentity.dome.fs.AgentSip;
import cn.com.tzy.springbootfs.convert.fs.AgentConvert;
import cn.com.tzy.springbootfs.mapper.fs.AgentMapper;
import cn.com.tzy.springbootfs.mapper.fs.AgentSipMapper;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.RedisService;
import cn.com.tzy.springbootstarterfreeswitch.service.freeswitch.AgentVoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 坐席运行时数据服务实现。
 * 负责从数据库加载坐席信息并与 Redis 中的运行时缓存同步。
 * ZLM/WebRTC 推流能力已移除，媒体通道由客户端 SIP 设备与 FreeSWITCH 直接协商。
 */
@Log4j2
@Service
public class AgentVoServiceImpl extends AgentVoService {

    @Resource
    private AgentMapper agentMapper;
    @Resource
    private AgentSipMapper agentSipMapper;

    @Override
    public AgentVoInfo getAgentBySip(String sip) {
        Agent agent = agentMapper.getAgentBySip(sip);
        return buildAgentVoInfo(agent);
    }

    @Override
    public AgentVoInfo getAgentByKey(String agentKey) {
        Agent agent = agentMapper.selectOne(
                new LambdaQueryWrapper<Agent>().eq(Agent::getAgentKey, agentKey));
        return buildAgentVoInfo(agent);
    }

    @Override
    public void stopStream(String callId) {
        // 已移至 AgentServiceImpl.stopStream()，通过 SIP BYE 挂断
    }

    @Override
    public AgentVoInfo getAgentByCompanyCode(String company, String agentCode) {
        Agent agent = agentMapper.selectOne(
                new LambdaQueryWrapper<Agent>()
                        .eq(Agent::getCompanyId, company)
                        .eq(Agent::getAgentCode, agentCode));
        return buildAgentVoInfo(agent);
    }

    @Override
    public AgentVoInfo findAgentId(String id) {
        Agent agent = agentMapper.selectById(id);
        return buildAgentVoInfo(agent);
    }

    @Override
    public void save(AgentVoInfo entity) {
        Agent agent = AgentConvert.INSTANCE.convert(entity);
        if (agent.getId() != null) {
            agentMapper.updateById(agent);
        } else {
            agentMapper.insert(agent);
        }
    }

    @Override
    public void updateStatus(Long id, boolean online) {
        Agent agent = agentMapper.selectById(id);
        if (agent == null) {
            return;
        }
        Agent update = Agent.builder()
                .id(agent.getId())
                .host("")
                .state(online ? ConstEnum.Flag.YES.getValue() : ConstEnum.Flag.NO.getValue())
                .registerTime(online ? new Date() : null)
                .renewTime(online ? new Date() : null)
                .keepaliveTime(online ? new Date() : null)
                .build();
        agentMapper.updateById(update);
    }

    @Override
    public void startPlay(String agentCode, String stream) {
        // 纯 SIP 模式下由 FreeSWITCH 管理媒体流，无需本地启动播放
    }

    @Override
    public void stopPlay(String agentCode) {
        // 纯 SIP 模式下由 FreeSWITCH 管理媒体流，无需本地停止播放
    }

    // ---- private helpers ----

    private AgentVoInfo buildAgentVoInfo(Agent agent) {
        if (agent == null) {
            return null;
        }
        List<AgentSip> agentSips = agentSipMapper.selectList(Wrappers.<AgentSip>lambdaQuery().eq(AgentSip::getAgentId, agent.getId()));
        if (!agentSips.isEmpty()) {
            agent.setSipPhoneList(agentSips.stream().map(AgentSip::getSip).collect(Collectors.toList()));
        }
        AgentVoInfo info = AgentConvert.INSTANCE.convert(agent);
        info.setAgentOnline(ConstEnum.Flag.NO.getValue());
        info.setSsrcCheck(ConstEnum.Flag.NO.getValue());
        return info;
    }
}
