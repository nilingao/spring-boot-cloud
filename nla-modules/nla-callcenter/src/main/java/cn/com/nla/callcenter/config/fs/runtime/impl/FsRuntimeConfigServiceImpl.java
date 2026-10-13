package cn.com.nla.callcenter.config.fs.runtime.impl;

import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigService;
import cn.com.nla.callcenter.domain.Company;
import cn.com.nla.callcenter.domain.CompanyConference;
import cn.com.nla.callcenter.domain.CompanyPhone;
import cn.com.nla.callcenter.domain.CompanyPhoneGroup;
import cn.com.nla.callcenter.domain.Group;
import cn.com.nla.callcenter.domain.GroupAgentStrategy;
import cn.com.nla.callcenter.domain.GroupMemoryConfig;
import cn.com.nla.callcenter.domain.GroupOverflow;
import cn.com.nla.callcenter.domain.GroupStrategyExp;
import cn.com.nla.callcenter.domain.OverflowExp;
import cn.com.nla.callcenter.domain.OverflowFront;
import cn.com.nla.callcenter.domain.Playback;
import cn.com.nla.callcenter.domain.RouteCall;
import cn.com.nla.callcenter.domain.RouteGateway;
import cn.com.nla.callcenter.domain.RouteGatewayGroup;
import cn.com.nla.callcenter.domain.RouteGroup;
import cn.com.nla.callcenter.domain.SkillGroup;
import cn.com.nla.callcenter.domain.VdnCode;
import cn.com.nla.callcenter.domain.VdnConfig;
import cn.com.nla.callcenter.domain.VdnDtmf;
import cn.com.nla.callcenter.domain.VdnPhone;
import cn.com.nla.callcenter.domain.VdnSchedule;
import cn.com.nla.callcenter.mapper.CompanyConferenceMapper;
import cn.com.nla.callcenter.mapper.CompanyMapper;
import cn.com.nla.callcenter.mapper.CompanyPhoneGroupMapper;
import cn.com.nla.callcenter.mapper.CompanyPhoneMapper;
import cn.com.nla.callcenter.mapper.GroupAgentStrategyMapper;
import cn.com.nla.callcenter.mapper.GroupMapper;
import cn.com.nla.callcenter.mapper.GroupMemoryConfigMapper;
import cn.com.nla.callcenter.mapper.GroupOverflowMapper;
import cn.com.nla.callcenter.mapper.GroupStrategyExpMapper;
import cn.com.nla.callcenter.mapper.OverflowExpMapper;
import cn.com.nla.callcenter.mapper.OverflowFrontMapper;
import cn.com.nla.callcenter.mapper.PlaybackMapper;
import cn.com.nla.callcenter.mapper.RouteCallMapper;
import cn.com.nla.callcenter.mapper.RouteGatewayGroupMapper;
import cn.com.nla.callcenter.mapper.RouteGatewayMapper;
import cn.com.nla.callcenter.mapper.RouteGroupMapper;
import cn.com.nla.callcenter.mapper.SkillGroupMapper;
import cn.com.nla.callcenter.mapper.VdnCodeMapper;
import cn.com.nla.callcenter.mapper.VdnConfigMapper;
import cn.com.nla.callcenter.mapper.VdnDtmfMapper;
import cn.com.nla.callcenter.mapper.VdnPhoneMapper;
import cn.com.nla.callcenter.mapper.VdnScheduleMapper;
import cn.com.nla.common.core.utils.MapstructUtils;
import cn.com.nla.common.freeswitch.client.fs.AgentStrategy;
import cn.com.nla.common.freeswitch.client.fs.assign.LeastAnswerAssign;
import cn.com.nla.common.freeswitch.client.fs.assign.LeastTalkAssign;
import cn.com.nla.common.freeswitch.client.fs.assign.LongReadyAssign;
import cn.com.nla.common.freeswitch.client.fs.assign.TotalReadyTimesAssign;
import cn.com.nla.common.freeswitch.client.fs.lineup.CustomLineupStrategy;
import cn.com.nla.common.freeswitch.client.fs.lineup.DefaultLineupStrategy;
import cn.com.nla.common.freeswitch.client.fs.lineup.VipLineupStrategy;
import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.model.fs.CompanyConferenceInfo;
import cn.com.nla.common.freeswitch.model.fs.CompanyInfo;
import cn.com.nla.common.freeswitch.model.fs.GroupAgentStrategyInfo;
import cn.com.nla.common.freeswitch.model.fs.GroupInfo;
import cn.com.nla.common.freeswitch.model.fs.GroupMemoryConfigInfo;
import cn.com.nla.common.freeswitch.model.fs.GroupOverFlowInfo;
import cn.com.nla.common.freeswitch.model.fs.GroupStrategyExpInfo;
import cn.com.nla.common.freeswitch.model.fs.OverflowExpInfo;
import cn.com.nla.common.freeswitch.model.fs.OverflowFrontInfo;
import cn.com.nla.common.freeswitch.model.fs.PlaybackInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteCallInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteGateWayInfo;
import cn.com.nla.common.freeswitch.model.fs.RouteGroupInfo;
import cn.com.nla.common.freeswitch.model.fs.SkillGroupInfo;
import cn.com.nla.common.freeswitch.model.fs.VdnConfigInfo;
import cn.com.nla.common.freeswitch.model.fs.VdnDtmfInfo;
import cn.com.nla.common.freeswitch.model.fs.VdnScheduleInfo;
import cn.com.nla.common.freeswitch.model.fs.VdnCodeInfo;
import cn.com.nla.common.freeswitch.model.fs.VdnPhoneInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企业呼叫中心运行时配置装配服务。
 *
 * <p>迁移自旧 config.fs.runtime.impl.FsRuntimeConfigServiceImpl：将 22 张管理域表
 * 装配为协议栈 Redis 缓存（企业/技能组/路由/VDN/放音/会议）。旧 IService 依赖改为
 * mapper 直注（对齐 nla 分层惯例），旧 17 个 MapStruct Convert 的同名隐式转换
 * 统一由 MapstructUtils 承担。</p>
 *
 * <p>仅在 freeswitch.enabled=true 时注册，避免协议组件缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class FsRuntimeConfigServiceImpl implements FsRuntimeConfigService {

    private final CompanyMapper companyMapper;
    private final RouteCallMapper routeCallMapper;
    private final RouteGroupMapper routeGroupMapper;
    private final RouteGatewayMapper routeGatewayMapper;
    private final RouteGatewayGroupMapper routeGatewayGroupMapper;
    private final GroupMapper groupMapper;
    private final GroupOverflowMapper groupOverflowMapper;
    private final OverflowFrontMapper overflowFrontMapper;
    private final OverflowExpMapper overflowExpMapper;
    private final GroupAgentStrategyMapper groupAgentStrategyMapper;
    private final GroupStrategyExpMapper groupStrategyExpMapper;
    private final GroupMemoryConfigMapper groupMemoryConfigMapper;
    private final CompanyPhoneMapper companyPhoneMapper;
    private final CompanyPhoneGroupMapper companyPhoneGroupMapper;
    private final SkillGroupMapper skillGroupMapper;
    private final VdnCodeMapper vdnCodeMapper;
    private final VdnConfigMapper vdnConfigMapper;
    private final VdnScheduleMapper vdnScheduleMapper;
    private final VdnDtmfMapper vdnDtmfMapper;
    private final VdnPhoneMapper vdnPhoneMapper;
    private final PlaybackMapper playbackMapper;
    private final CompanyConferenceMapper companyConferenceMapper;

    @Override
    public void refreshAll() {
        RedisService.getAgentInfoManager().delAll();
        RedisService.getCompanyInfoManager().delAll();
        RedisService.getVdnPhoneManager().delAll();
        RedisService.getPlaybackInfoManager().delAll();
        RedisService.getCompanyConferenceInfoManager().delAll();
        initAllCompanies();
    }

    @Override
    public void refreshCompany(Long companyId) {
        Company company = companyMapper.selectById(companyId);
        if (company == null || company.getStatus() == null || company.getStatus() == 0) {
            RedisService.getCompanyInfoManager().del(String.valueOf(companyId));
            return;
        }
        refreshCompanies(Collections.singletonList(companyId));
    }

    // ---- private helpers ----

    private void initAllCompanies() {
        List<Company> companyList = companyMapper.selectList(
                new LambdaQueryWrapper<Company>().ne(Company::getStatus, 0));
        if (companyList.isEmpty()) {
            return;
        }
        List<Long> companyIds = companyList.stream().map(Company::getId).collect(Collectors.toList());
        buildAndCacheCompanies(companyList, companyIds);
    }

    private void refreshCompanies(List<Long> companyIds) {
        List<Company> companyList = companyMapper.selectBatchIds(companyIds);
        if (companyList.isEmpty()) {
            return;
        }
        buildAndCacheCompanies(companyList, companyIds);
    }

    private void buildAndCacheCompanies(List<Company> companyList, List<Long> companyIds) {
        List<CompanyInfo> companyInfoList = MapstructUtils.convert(companyList, CompanyInfo.class);
        Map<Long, CompanyInfo> companyIdMap = companyInfoList.stream()
                .collect(Collectors.toMap(o -> Long.parseLong(o.getId()), Function.identity()));

        // 会议
        List<CompanyConference> companyConferenceList = companyConferenceMapper.selectList(
                Wrappers.<CompanyConference>lambdaQuery().in(CompanyConference::getCompanyId, companyIds));
        // 路由
        List<RouteGroup> routeGroupList = routeGroupMapper.selectList(Wrappers.emptyWrapper());
        List<RouteGroupInfo> routeGroupInfoList = MapstructUtils.convert(routeGroupList, RouteGroupInfo.class);
        List<RouteCall> routeCallList = routeCallMapper.selectList(
                new LambdaQueryWrapper<RouteCall>().in(RouteCall::getCompanyId, companyIds));
        Map<Long, List<RouteCall>> routeCallCompanyMap = routeCallList.stream()
                .collect(Collectors.groupingBy(RouteCall::getCompanyId));
        // 技能组
        List<Group> groupList = groupMapper.selectList(
                new LambdaQueryWrapper<Group>().in(Group::getCompanyId, companyIds));
        List<GroupInfo> groupInfoList = MapstructUtils.convert(groupList, GroupInfo.class);
        List<GroupOverFlowInfo> groupOverFlowInfoList = groupOverflowMapper.findGroupOverFlowInfo();
        List<OverflowFront> overflowFrontList = overflowFrontMapper.selectList(Wrappers.emptyWrapper());
        List<OverflowFrontInfo> overflowFrontInfoList = MapstructUtils.convert(overflowFrontList, OverflowFrontInfo.class);
        List<OverflowExp> overflowExpList = overflowExpMapper.selectList(Wrappers.emptyWrapper());
        List<OverflowExpInfo> overflowExpInfoList = MapstructUtils.convert(overflowExpList, OverflowExpInfo.class);
        List<GroupAgentStrategy> groupAgentStrategyList = groupAgentStrategyMapper.selectList(Wrappers.emptyWrapper());
        List<GroupAgentStrategyInfo> groupAgentStrategyInfoList = MapstructUtils.convert(groupAgentStrategyList, GroupAgentStrategyInfo.class);
        List<GroupStrategyExp> groupStrategyExpList = groupStrategyExpMapper.selectList(Wrappers.emptyWrapper());
        List<GroupStrategyExpInfo> groupStrategyExpInfoList = MapstructUtils.convert(groupStrategyExpList, GroupStrategyExpInfo.class);
        List<GroupMemoryConfig> groupMemoryConfigList = groupMemoryConfigMapper.selectList(Wrappers.emptyWrapper());
        List<GroupMemoryConfigInfo> groupMemoryConfigInfoList = MapstructUtils.convert(groupMemoryConfigList, GroupMemoryConfigInfo.class);
        // 号码
        List<CompanyPhoneGroup> companyPhoneGroupList = companyPhoneGroupMapper.selectList(Wrappers.emptyWrapper());
        List<CompanyPhone> companyPhoneList = companyPhoneMapper.selectList(Wrappers.emptyWrapper());
        // 技能组关系
        List<SkillGroup> skillGroupList = skillGroupMapper.selectList(
                new LambdaQueryWrapper<SkillGroup>().in(SkillGroup::getCompanyId, companyIds));
        List<SkillGroupInfo> skillGroupInfoList = MapstructUtils.convert(skillGroupList, SkillGroupInfo.class);
        // VDN
        List<VdnCode> vdnCodeList = vdnCodeMapper.selectList(
                new LambdaQueryWrapper<VdnCode>().in(VdnCode::getCompanyId, companyIds)
                        .eq(VdnCode::getStatus, ConstEnum.Flag.YES.getValue()));
        Map<Long, List<VdnCode>> vdnCodeCompanyMap = vdnCodeList.stream().collect(Collectors.groupingBy(VdnCode::getCompanyId));
        List<VdnConfig> vdnConfigList = vdnConfigMapper.selectList(
                new LambdaQueryWrapper<VdnConfig>().in(VdnConfig::getCompanyId, companyIds));
        Map<Long, List<VdnConfig>> vdnConfigCompanyMap = vdnConfigList.stream().collect(Collectors.groupingBy(VdnConfig::getCompanyId));
        List<VdnSchedule> vdnScheduleList = vdnScheduleMapper.selectList(
                new LambdaQueryWrapper<VdnSchedule>().in(VdnSchedule::getCompanyId, companyIds));
        Map<Long, List<VdnSchedule>> vdnScheduleCompanyMap = vdnScheduleList.stream().collect(Collectors.groupingBy(VdnSchedule::getCompanyId));
        List<VdnDtmf> vdnDtmfList = vdnDtmfMapper.selectList(
                new LambdaQueryWrapper<VdnDtmf>().in(VdnDtmf::getCompanyId, companyIds));
        Map<Long, List<VdnDtmf>> vdnDtmfCompanyMap = vdnDtmfList.stream().collect(Collectors.groupingBy(VdnDtmf::getCompanyId));
        // VDN 号码
        List<VdnPhone> vdnPhoneList = vdnPhoneMapper.selectList(
                new LambdaQueryWrapper<VdnPhone>().in(VdnPhone::getCompanyId, companyIds));
        List<VdnPhoneInfo> vdnPhoneInfoList = MapstructUtils.convert(vdnPhoneList, VdnPhoneInfo.class);
        // 放音
        List<Playback> playbackList = playbackMapper.selectList(
                new LambdaQueryWrapper<Playback>().in(Playback::getCompanyId, companyIds));
        List<PlaybackInfo> playbackInfoList = MapstructUtils.convert(playbackList, PlaybackInfo.class);

        // 初始化路由网关
        initRouteGroupInfo(routeGroupInfoList);
        // 初始化技能组
        initGroupStrategy(groupInfoList, groupOverFlowInfoList, overflowFrontInfoList, overflowExpInfoList,
                groupAgentStrategyInfoList, groupStrategyExpInfoList, groupMemoryConfigInfoList,
                companyPhoneGroupList, companyPhoneList, skillGroupInfoList);
        // 会议
        companyConferenceList.forEach(c -> {
            List<CompanyConferenceInfo> list = MapstructUtils.convert(Collections.singletonList(c), CompanyConferenceInfo.class);
            list.forEach(o -> RedisService.getCompanyConferenceInfoManager().put(o));
        });

        for (CompanyInfo companyInfo : companyIdMap.values()) {
            companyInfo.initRouteCalls(routeGroupInfoList,
                    MapstructUtils.convert(
                            routeCallCompanyMap.computeIfAbsent(Long.parseLong(companyInfo.getId()), k -> new ArrayList<>()),
                            RouteCallInfo.class));
            companyInfo.initVdn(
                    MapstructUtils.convert(vdnCodeCompanyMap.computeIfAbsent(Long.parseLong(companyInfo.getId()), k -> new ArrayList<>()), VdnCodeInfo.class),
                    MapstructUtils.convert(vdnConfigCompanyMap.computeIfAbsent(Long.parseLong(companyInfo.getId()), k -> new ArrayList<>()), VdnConfigInfo.class),
                    MapstructUtils.convert(vdnScheduleCompanyMap.computeIfAbsent(Long.parseLong(companyInfo.getId()), k -> new ArrayList<>()), VdnScheduleInfo.class),
                    MapstructUtils.convert(vdnDtmfCompanyMap.computeIfAbsent(Long.parseLong(companyInfo.getId()), k -> new ArrayList<>()), VdnDtmfInfo.class)
            );
            RedisService.getCompanyInfoManager().put(companyInfo);
        }
        vdnPhoneInfoList.forEach(o -> RedisService.getVdnPhoneManager().put(o));
        playbackInfoList.forEach(o -> RedisService.getPlaybackInfoManager().put(o));
    }

    private void initRouteGroupInfo(List<RouteGroupInfo> routeGroupInfoList) {
        List<RouteGatewayGroup> routeGatewayGroupList = routeGatewayGroupMapper.selectList(Wrappers.emptyWrapper());
        Map<Long, List<RouteGatewayGroup>> routeGatewayGroupMap = routeGatewayGroupList.stream()
                .collect(Collectors.groupingBy(RouteGatewayGroup::getRouteGroupId));
        List<RouteGateway> routeGatewayList = routeGatewayMapper.selectList(Wrappers.emptyWrapper());
        List<RouteGateWayInfo> routeGateWayInfoList = MapstructUtils.convert(routeGatewayList, RouteGateWayInfo.class);
        Map<Long, RouteGateWayInfo> routeGatewayMap = routeGateWayInfoList.stream()
                .collect(Collectors.toMap(RouteGateWayInfo::getId, o -> o));
        for (RouteGroupInfo routeGroupInfo : routeGroupInfoList) {
            List<RouteGatewayGroup> gatewayGroupList = routeGatewayGroupMap.computeIfAbsent(
                    routeGroupInfo.getId(), k -> new ArrayList<>());
            if (gatewayGroupList.isEmpty()) continue;
            List<Long> gatewayIdList = gatewayGroupList.stream().map(RouteGatewayGroup::getGatewayId).collect(Collectors.toList());
            List<RouteGateWayInfo> collect = gatewayIdList.stream().map(routeGatewayMap::get).collect(Collectors.toList());
            if (!collect.isEmpty()) {
                routeGroupInfo.setRouteGateWayInfoList(collect);
            }
        }
    }

    private void initGroupStrategy(
            List<GroupInfo> groupInfoList,
            List<GroupOverFlowInfo> groupOverFlowInfoList,
            List<OverflowFrontInfo> overflowFrontInfoList,
            List<OverflowExpInfo> overflowExpInfoList,
            List<GroupAgentStrategyInfo> groupAgentStrategyInfoList,
            List<GroupStrategyExpInfo> groupStrategyExpInfoList,
            List<GroupMemoryConfigInfo> groupMemoryConfigInfoList,
            List<CompanyPhoneGroup> companyPhoneGroupList,
            List<CompanyPhone> companyPhoneList,
            List<SkillGroupInfo> skillGroupInfoList) {
        RedisService.getGroupInfoManager().delAll();
        if (groupInfoList == null || groupInfoList.isEmpty()) return;

        Map<Long, List<GroupOverFlowInfo>> groupOverFlowInfoMap = groupOverFlowInfoList.stream()
                .collect(Collectors.groupingBy(GroupOverFlowInfo::getGroupId));
        Map<Long, List<OverflowFrontInfo>> overflowFrontInfoMap = overflowFrontInfoList.stream()
                .collect(Collectors.groupingBy(OverflowFrontInfo::getOverflowId));
        Map<Long, List<OverflowExpInfo>> overflowExpInfoMap = overflowExpInfoList.stream()
                .collect(Collectors.groupingBy(OverflowExpInfo::getOverflowId));
        Map<Long, GroupAgentStrategyInfo> groupAgentStrategyInfoMap = groupAgentStrategyInfoList.stream()
                .collect(Collectors.toMap(GroupAgentStrategyInfo::getGroupId, o -> o, (o1, o2) -> o2));
        Map<Long, List<GroupStrategyExpInfo>> groupStrategyExpInfoMap = groupStrategyExpInfoList.stream()
                .collect(Collectors.groupingBy(GroupStrategyExpInfo::getGroupId));
        Map<Long, GroupMemoryConfigInfo> groupMemoryConfigInfoMap = groupMemoryConfigInfoList.stream()
                .collect(Collectors.toMap(GroupMemoryConfigInfo::getGroupId, o -> o, (o1, o2) -> o2));
        Map<Long, List<CompanyPhoneGroup>> companyPhoneGroupMap = companyPhoneGroupList.stream()
                .collect(Collectors.groupingBy(CompanyPhoneGroup::getDisplayId));
        Map<Long, List<SkillGroupInfo>> skillGroupInfoMap = skillGroupInfoList.stream()
                .collect(Collectors.groupingBy(SkillGroupInfo::getGroupId));

        for (GroupInfo groupInfo : groupInfoList) {
            List<GroupOverFlowInfo> overFlowInfoList = groupOverFlowInfoMap.get(Long.valueOf(groupInfo.getId()));
            if (overFlowInfoList == null || overFlowInfoList.isEmpty()) {
                groupInfo.setGroupOverflows(new ArrayList<>());
            } else {
                for (GroupOverFlowInfo info : overFlowInfoList) {
                    info.setOverflowFronts(overflowFrontInfoMap.computeIfAbsent(info.getOverflowId(), k -> new ArrayList<>()));
                    info.setOverflowExps(overflowExpInfoMap.computeIfAbsent(info.getOverflowId(), k -> new ArrayList<>()));
                }
                groupInfo.setGroupOverflows(overFlowInfoList);
            }
            GroupAgentStrategyInfo strategyInfo = groupAgentStrategyInfoMap.get(Long.valueOf(groupInfo.getId()));
            if (strategyInfo != null) {
                strategyInfo.setStrategyExpList(groupStrategyExpInfoMap.computeIfAbsent(Long.valueOf(groupInfo.getId()), k -> new ArrayList<>()));
                groupInfo.setGroupAgentStrategyPo(strategyInfo);
            }
            groupInfo.setGroupMemoryConfig(groupMemoryConfigInfoMap.get(Long.valueOf(groupInfo.getId())));
            List<Long> calledPhoneIdList = companyPhoneGroupMap.computeIfAbsent(groupInfo.getCalledDisplayId(), o -> new ArrayList<>())
                    .stream().map(CompanyPhoneGroup::getPhoneId).collect(Collectors.toList());
            List<Long> callerPhoneIdList = companyPhoneGroupMap.computeIfAbsent(groupInfo.getCallerDisplayId(), o -> new ArrayList<>())
                    .stream().map(CompanyPhoneGroup::getPhoneId).collect(Collectors.toList());
            groupInfo.setCalledDisplays(companyPhoneList.stream().filter(o -> calledPhoneIdList.contains(o.getId())).map(CompanyPhone::getPhone).collect(Collectors.toList()));
            groupInfo.setCallerDisplays(companyPhoneList.stream().filter(o -> callerPhoneIdList.contains(o.getId())).map(CompanyPhone::getPhone).collect(Collectors.toList()));
            groupInfo.setSkills(skillGroupInfoMap.computeIfAbsent(Long.valueOf(groupInfo.getId()), o -> new ArrayList<>()));
        }
        for (GroupInfo groupInfo : groupInfoList) {
            if (groupInfo.getGroupOverflows() == null) continue;
            groupInfo.getGroupOverflows().forEach(overflowConfig -> {
                switch (overflowConfig.getBusyType()) {
                    case 1: overflowConfig.setLineupStrategy(new DefaultLineupStrategy()); break;
                    case 2: overflowConfig.setLineupStrategy(new VipLineupStrategy()); break;
                    case 3: overflowConfig.setLineupStrategy(new CustomLineupStrategy("")); break;
                    default: break;
                }
            });
            if (groupInfo.getGroupAgentStrategyPo() == null) continue;
            Integer agentStrategyType = groupInfo.getGroupAgentStrategyPo().getStrategyType();
            Integer agentStrategyValue = groupInfo.getGroupAgentStrategyPo().getStrategyValue();
            AgentStrategy agentStrategy = null;
            if (agentStrategyType == 1) {
                switch (agentStrategyValue) {
                    case 1: agentStrategy = new LongReadyAssign(); break;
                    case 2: agentStrategy = new TotalReadyTimesAssign(); break;
                    case 3: agentStrategy = new LeastAnswerAssign(); break;
                    case 4: agentStrategy = new LeastTalkAssign(); break;
                    default: break;
                }
            }
            groupInfo.getGroupAgentStrategyPo().setAgentStrategy(agentStrategy);
            RedisService.getGroupInfoManager().put(groupInfo);
        }
    }
}
