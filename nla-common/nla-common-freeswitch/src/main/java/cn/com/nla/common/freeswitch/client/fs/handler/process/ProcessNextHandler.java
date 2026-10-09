package cn.com.nla.common.freeswitch.client.fs.handler.process;

import cn.com.nla.common.freeswitch.client.fs.handler.StrategyHandler;
import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.enums.fs.CauseEnums;
import cn.com.nla.common.freeswitch.enums.fs.NextTypeEnum;
import cn.com.nla.common.freeswitch.enums.fs.ProcessEnum;
import cn.com.nla.common.freeswitch.model.call.CallDetail;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.model.call.NextCommand;
import cn.com.nla.common.freeswitch.model.fs.*;
import cn.com.nla.common.freeswitch.model.message.HangupCallModel;
import cn.com.nla.common.freeswitch.model.message.MakeCallModel;
import cn.com.nla.common.freeswitch.model.message.PlayBackCallModel;
import cn.com.nla.common.freeswitch.model.message.ReceiveDtmfModel;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.service.FsService;
import cn.hutool.core.util.RandomUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 流程处理相关
 */
@Getter
@Slf4j
public class ProcessNextHandler {

    private final StrategyHandler strategyHandler;

    public ProcessNextHandler(StrategyHandler strategyHandler){
        this.strategyHandler = strategyHandler;
    }

    // 1:技能组,2:ivr,3:坐席,4:外呼,5:流程
    public void next(ProcessEnum type, String bizId, String deviceId, CallInfo callInfo){
        switch (type){
            case GROUP_PROCESS:
                groupProcess(bizId,deviceId,callInfo);
                break;
            case IVR_PROCESS:
                ivrProcess(bizId,deviceId,callInfo);
                break;
            case AGENT_PROCESS:
                agentProcess(bizId,deviceId,callInfo);
                break;
            case CALL_PROCESS:
                callProcess(bizId,deviceId,callInfo);
                break;
            case VDN_PROCESS:
                vdnProcess(bizId,deviceId,callInfo);
                break;
            default:
                log.error("没有此流程类型");
        }

    }
    //下一步处理
    public void doNextCommand(CallInfo callInfo, DeviceInfo deviceInfo, NextCommand nextCommand) {
        if (nextCommand == null) {
            return;
        }
        callInfo.getNextCommands().remove(nextCommand);
        switch (nextCommand.getNextType()) {
            case NEXT_QUEUE_PLAY:
                strategyHandler.handler(PlayBackCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).isDown(false).playPath("queue.wav").build());
                break;
            case NEXT_QUEUE_OVERFLOW_GROUP:
                groupProcess(nextCommand.getNextValue(), deviceInfo.getDeviceId(),callInfo);
                break;
            case NEXT_QUEUE_OVERFLOW_IVR:
                break;

            case NEXT_QUEUE_OVERFLOW_VDN:
                break;
            case NEXT_HANGUP:
                strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).build());
                break;
            case NEXT_VDN:
                break;

            case NEXT_GROUP:
                break;

            case NEXT_IVR:
                break;
            default:
                break;
        }
        callInfo.getDeviceInfoMap().put(deviceInfo.getDeviceId(), deviceInfo);
        RedisService.getCallInfoManager().put(callInfo);
    }

    //电话转IVR(暂未实现)
    private void ivrProcess(String ivrId, String thisDeviceId,CallInfo callInfo){
        RedisService.getCallInfoManager().put(callInfo);
        Map<String, Object> params = new HashMap<>();
        params.put("callId", callInfo.getCallId());
        params.put("deviceId",thisDeviceId);
        params.put("ivrId", ivrId);

    }
    //电话转技能组
    private void groupProcess(String groupId,String thisDeviceId,CallInfo callInfo){
        GroupInfo groupInfo = RedisService.getGroupInfoManager().get(groupId);
        if (groupInfo == null) {
            log.warn("callId:{} join group is null", callInfo.getCallId());
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }
        /**
         * 指定坐席
         */
        if (desiganteAgent(callInfo, groupInfo, thisDeviceId)) {
            return;
        }
        /**
         * 走记忆坐席
         */
        if (memonryAgent(callInfo, groupInfo, thisDeviceId)) {
            return;
        }
        //排队溢出策略
        GroupOverFlowInfo groupOverFlow = FsService.getGroupMemoryInfoService().getEffectiveOverflow(groupInfo);
        if (groupOverFlow == null) {
            log.error("callId:{}, groupName:{} 无有效的溢出策略", callInfo.getCallId(), groupInfo.getName());
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }
        log.info("callId:{}, group:{} overflow:{}", callInfo.getCallId(), groupInfo.getName(), groupOverFlow.getName());
        /**
         * 1:排队,2:溢出,3:挂机
         */
        switch (groupOverFlow.getHandleType()) {
            case 1:
                log.info("group:{} handleType is lineUp, queueTimeout:{}秒, busyType:{}, busyTimeoutType:{}, overflowType:{}, overflowValue:{}, callId:{}", groupInfo.getName(), groupOverFlow.getQueueTimeout(), groupOverFlow.getBusyType(), groupOverFlow.getBusyTimeoutType(), groupOverFlow.getOverflowType(), groupOverFlow.getOverflowValue(), callInfo.getCallId());
                PriorityQueue<CallQueue> callQueues = FsService.getGroupMemoryInfoService().getCallInfoMap().get(groupId);
                if (callQueues == null) {
                    callQueues = new PriorityQueue<CallQueue>();
                }
                callInfo.setQueueStartTime(LocalDateTime.now());
                if (callInfo.getFristQueueTime() == null) {
                    callInfo.setFristQueueTime(callInfo.getQueueStartTime());
                }
                Long queueLevel = groupOverFlow.getLineupStrategy().calculateLevel(callInfo);
                callInfo.setQueueLevel(queueLevel);
                callQueues.add(CallQueue.builder()
                        .priority(callInfo.getQueueLevel())
                        .callId(callInfo.getCallId())
                        .startTime(callInfo.getQueueStartTime())
                        .groupId(groupId)
                        .groupOverflowInfo(groupOverFlow)
                        .deviceId(thisDeviceId)
                        .build()
                );
                FsService.getGroupMemoryInfoService().getCallInfoMap().put(callInfo.getGroupId(), callQueues);
                RedisService.getCallInfoManager().put(callInfo);
                break;

            case 2:
                overFlowHandler(callInfo, thisDeviceId, groupOverFlow);
                break;
            case 3:
                log.info("group:{} handleType is hangup, callId:{}", groupInfo.getName(), callInfo.getCallId());
                //技能组策略挂机
                callInfo.setHangupDir(3);
                callInfo.setHangupCode(CauseEnums.OVERFLOW_TIMEOUT.getHuangupCode());
                strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
                RedisService.getCallInfoManager().put(callInfo);
                break;
            default:
                break;
        }
    }
    //电话转坐席
    private void agentProcess(String agentKey, String thisDeviceId,CallInfo callInfo){
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agentKey);
        if (agentVoInfo == null) {
            log.error("agent:{} agentVoInfo is error, callId:{}", agentKey, callInfo.getCallId());
            return;
        }
        String deviceId = RandomUtil.randomString(16);
        log.info("callId:{} find agent:{}", callInfo.getCallId(), agentVoInfo.getAgentKey());
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setCaller(agentVoInfo.getAgentId());
        deviceInfo.setDisplay(callInfo.getCaller());
        deviceInfo.setCalled(agentVoInfo.getAgentId());
        deviceInfo.setCallTime(LocalDateTime.now());
        deviceInfo.setCallId(callInfo.getCallId());
        deviceInfo.setDeviceId(deviceId);
        deviceInfo.setDeviceType(1);
        deviceInfo.setAgentKey(agentVoInfo.getAgentKey());
        callInfo.getDeviceList().add(deviceId);
        callInfo.getDeviceInfoMap().put(deviceId, deviceInfo);
        callInfo.setAgentKey(agentVoInfo.getAgentKey());
        callInfo.setCalled(agentVoInfo.getCalled());
        callInfo.getNextCommands().add(new NextCommand(deviceId, NextTypeEnum.NEXT_CALL_BRIDGE, thisDeviceId));
        RedisService.getCallInfoManager().put(callInfo);
        RedisService.getDeviceInfoManager().putDeviceCallId(deviceId, callInfo.getCallId());
        RouteGateWayInfo routeGateWayInfo = RedisService.getCompanyInfoManager().getRouteGateWayInfo(callInfo.getCompanyId(), agentVoInfo.getAgentId());
        if (routeGateWayInfo == null) {
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }
        agentVoInfo.setCallId(callInfo.getCallId());
        agentVoInfo.setDeviceId(deviceId);
        strategyHandler.handler(MakeCallModel.builder()
                .deviceId(deviceId)
                .sdp(callInfo.getSipSdp())
                .callId(callInfo.getCallId())
                .display(callInfo.getCaller())
                .called(agentVoInfo.getCalled())
                .originateTimeout(null)
                .sipHeaderList(null)
                .gatewayModel(MakeCallModel.RouteGatewayModel.builder()
                        .mediaHost(routeGateWayInfo.getMediaHost())
                        .mediaPort(routeGateWayInfo.getMediaPort())
                        .callerPrefix(routeGateWayInfo.getCallerPrefix())
                        .calledPrefix(routeGateWayInfo.getCalledPrefix())
                        .profile(routeGateWayInfo.getProfile())
                        .sipHeaderList(Arrays.asList(routeGateWayInfo.getSipHeader1(),routeGateWayInfo.getSipHeader2(),routeGateWayInfo.getSipHeader3()))
                        .build())
                .build());
    }
    //电话转外呼
    private void callProcess( String called, String thisDeviceId,CallInfo callInfo){
        log.info("callId:{} transfer to {}", callInfo.getCallId(), called);
        // 转外呼
        callInfo.setCalled(called);
        String deviceId = RandomUtil.randomString(16);
        RouteGateWayInfo routeGateWayInfo = RedisService.getCompanyInfoManager().getRouteGateWayInfo(callInfo.getCompanyId(), callInfo.getCalled());
        if (routeGateWayInfo == null) {
            log.warn("callId:{} origin error, called:{}", callInfo.getCallId(), callInfo.getCalled());
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }

        DeviceInfo device = DeviceInfo.builder()
                .deviceId(deviceId)
                .deviceType(3)
                .cdrType(2)
                .caller(callInfo.getCaller())
                .called(callInfo.getCalled())
                .callId(callInfo.getCallId())
                .callTime(LocalDateTime.now())
                .display(callInfo.getCaller())
                .build();
        callInfo.getDeviceList().add(deviceId);
        callInfo.getDeviceInfoMap().put(deviceId, device);

        CallDetail transferCall = new CallDetail();
        transferCall.setCallId(callInfo.getCallId());
        transferCall.setStartTime(LocalDateTime.now());
        transferCall.setDetailIndex(callInfo.getCallDetails().size() + 1);
        transferCall.setTransferType(5);
        callInfo.getCallDetails().add(transferCall);
        callInfo.getNextCommands().add(new NextCommand(thisDeviceId, NextTypeEnum.NEXT_CALL_BRIDGE, deviceId));
        strategyHandler.handler(MakeCallModel.builder()
                .deviceId(deviceId)
                .sdp(callInfo.getSipSdp())
                .callId(callInfo.getCallId())
                .display(callInfo.getCaller())
                .called(callInfo.getCalled())
                .originateTimeout(null)
                .sipHeaderList(null)
                .gatewayModel(MakeCallModel.RouteGatewayModel.builder()
                        .mediaHost(routeGateWayInfo.getMediaHost())
                        .mediaPort(routeGateWayInfo.getMediaPort())
                        .callerPrefix(routeGateWayInfo.getCallerPrefix())
                        .calledPrefix(routeGateWayInfo.getCalledPrefix())
                        .profile(routeGateWayInfo.getProfile())
                        .sipHeaderList(Arrays.asList(routeGateWayInfo.getSipHeader1(),routeGateWayInfo.getSipHeader2(),routeGateWayInfo.getSipHeader3()))
                        .build())
                .build());
    }

    //电话进vdn流程
    private void vdnProcess(String vdnId,String thisDeviceId,CallInfo callInfo){
        VdnCodeInfo vdnCodeInfo = RedisService.getCompanyInfoManager().getVdnCodeInfo(callInfo.getCompanyId(), Long.valueOf(vdnId));
        if (vdnCodeInfo == null || vdnCodeInfo.getStatus() == 0) {
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }
        //查询有效日程
        VdnConfigInfo vdnConfigInfo = vdnCodeInfo.getEffectiveSchedule();
        if (vdnConfigInfo == null) {
            log.warn("callId:{} not match schedule, vdn:{}", callInfo.getCallId(), vdnCodeInfo.getName());
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }
        //电话经过vdn
        CallDetail callDetail = new CallDetail();
        callDetail.setCallId(callInfo.getCallId());
        callDetail.setStartTime(LocalDateTime.now());
        callDetail.setDetailIndex(callInfo.getCallDetails().size() + 1);
        callDetail.setTransferType(1);
        callDetail.setTransferId(vdnConfigInfo.getVdnId());
        callInfo.getCallDetails().add(callDetail);
        log.info("vdnSchedule:{} routeType:{} routeValue:{}", vdnConfigInfo.getName(), vdnConfigInfo.getRouteType(), vdnConfigInfo.getRouteValue());
        //1:技能组,2:放音,3:ivr,4:坐席,5:外呼 流程
        switch (vdnConfigInfo.getRouteType()){
            case 1:
                callInfo.setGroupId(vdnConfigInfo.getRouteValue());
                next(ProcessEnum.GROUP_PROCESS,vdnConfigInfo.getRouteValue(),thisDeviceId,callInfo);
                break;
            case 2:
                /**
                 * 普通放音或者按键导航音 1:按键导航,2:技能组,3:ivr,4:路由字码,5:挂机
                 */
                playback(vdnConfigInfo.getRouteValue(),thisDeviceId,callInfo,vdnConfigInfo);
                break;
            case 3:
                /**
                 * 转IVR
                 */
                next(ProcessEnum.IVR_PROCESS,vdnConfigInfo.getRouteValue(),thisDeviceId,callInfo);
                return;

            case 4:
                /**
                 * 转坐席
                 */
                AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(vdnConfigInfo.getRouteValue());
                if (agentVoInfo == null || agentVoInfo.getAgentState() != AgentStateEnum.READY) {
                    strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
                    return;
                }
                next(ProcessEnum.AGENT_PROCESS,vdnConfigInfo.getRouteValue(),thisDeviceId,callInfo);
                return;
            case 5:
                /**
                 * 转外呼
                 */
                next(ProcessEnum.CALL_PROCESS,vdnConfigInfo.getRouteValue(),thisDeviceId,callInfo);
                return;
            default:
                log.warn("vdnCode not match callId:{} , case:{}", callInfo.getCallId(), vdnConfigInfo.getRouteType());
                break;
        }

    }
    //电话进播放流程
    private void playback(String playbackId,String thisDeviceId,CallInfo callInfo,VdnConfigInfo vdnConfigInfo) {
        /**
         * 普通放音或者按键导航音 1:按键导航,2:技能组,3:ivr,4:路由字码,5:挂机
         */
        PlaybackInfo playbackInfo = RedisService.getPlaybackInfoManager().get(playbackId);
        if (playbackInfo == null) {
            log.warn("playback is null, callId:{} , device:{}", callInfo.getCallId(), thisDeviceId);
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
            return;
        }
        DeviceInfo deviceInfo = callInfo.getDeviceInfoMap().get(thisDeviceId);
        //放音类型(1:按键导航,2:技能组,3:ivr,4:路由字码,5:挂机)
        log.info("playback tyep:{}, value:{}", vdnConfigInfo.getPlayType(), vdnConfigInfo.getPlayValue());
        if (vdnConfigInfo.getPlayType() != 1) {
            NextCommand nextCommand = null;
            switch (vdnConfigInfo.getPlayType()) {
                case 2:
                    nextCommand = new NextCommand(thisDeviceId, NextTypeEnum.NEXT_GROUP, vdnConfigInfo.getPlayValue());
                    break;
                case 3:
                    nextCommand = new NextCommand(thisDeviceId, NextTypeEnum.NEXT_IVR, vdnConfigInfo.getPlayValue());
                    break;
                case 4:
                    nextCommand = new NextCommand(thisDeviceId, NextTypeEnum.NEXT_VDN, vdnConfigInfo.getPlayValue());
                    break;
                case 5:
                    nextCommand = new NextCommand(thisDeviceId, NextTypeEnum.NEXT_HANGUP, vdnConfigInfo.getPlayValue());
                    break;
                default:
                    break;
            }
            strategyHandler.handler(PlayBackCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).isDown(false).playPath(playbackInfo.getPlayback()).build());
            doNextCommand(callInfo, deviceInfo, nextCommand);
            return;
        }
        strategyHandler.handler(ReceiveDtmfModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(thisDeviceId).build());
    }

    /**
     * 指定坐席
     */
    private boolean desiganteAgent(CallInfo callInfo, GroupInfo groupInfo, String deviceId) {
        if (!callInfo.getProcessData().containsKey(Constant.DESIGNATE_AGENT)) {
            return false;
        }
        GroupMemoryConfigInfo groupMemoryConfig = groupInfo.getGroupMemoryConfig();
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(callInfo.getProcessData().get(Constant.DESIGNATE_AGENT).toString());
        if (agentVoInfo != null && agentVoInfo.getAgentState() == AgentStateEnum.READY) {
            log.info("callId:{} get desiganteAgent:{} on group:{}", callInfo.getCallId(), agentVoInfo.getAgentKey(), callInfo.getGroupId());
            //呼叫坐席
            callInfo.setQueueEndTime(LocalDateTime.now());
            CallDetail joinGroup = callInfo.getCallDetails().get(callInfo.getCallDetails().size() - 1);
            joinGroup.setEndTime(LocalDateTime.now());
            next(ProcessEnum.AGENT_PROCESS,agentVoInfo.getAgentKey(),deviceId,callInfo);
            FsService.getGroupMemoryInfoService().agentNotReady(agentVoInfo);
            return true;
        }
        //没有空闲坐席，走匹配失败策略
        if (groupMemoryConfig == null) {
            return false;
        }
        switch (groupMemoryConfig.getFailStrategy()) {
            case 1:
                //转其他空闲坐席
                return false;
            case 2:
                //转其他技能组
                next(ProcessEnum.GROUP_PROCESS,groupMemoryConfig.getFailStrategyValue(),deviceId,callInfo);
                return true;
            case 3://vdn
                break;
            case 4://ivr
                break;
            case 5://挂机
                strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceId).build());
                return true;
            default:
                break;
        }
        return false;
    }

    /**
     * 呼入记忆坐席
     */
    private boolean memonryAgent(CallInfo callInfo, GroupInfo groupInfo, String deviceId) {
        GroupMemoryConfigInfo groupMemoryConfig = groupInfo.getGroupMemoryConfig();
        if (groupMemoryConfig == null || groupInfo.getGroupMemoryConfig().getStatus() != 1) {
            return false;
        }
        GroupMemoryInfo groupMemory = FsService.getGroupMemoryInfoService().find(groupInfo.getId(), callInfo.getCaller());
        if (groupMemory == null) {
            switch (groupMemoryConfig.getFailStrategy()) {
                case 1:
                    return false;
                case 2:
                    next(ProcessEnum.GROUP_PROCESS,groupMemoryConfig.getFailStrategyValue(),deviceId,callInfo);
                    return true;
                case 5://挂机
                    strategyHandler.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceId).build());
                    return true;
                default:
                    return false;
            }
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(groupMemory.getAgentKey());
        if (agentVoInfo != null && agentVoInfo.getAgentState() == AgentStateEnum.READY) {
            next(ProcessEnum.AGENT_PROCESS,agentVoInfo.getAgentKey(),deviceId,callInfo);
            FsService.getGroupMemoryInfoService().agentNotReady(agentVoInfo);
            return true;
        }
        //匹配上了记忆坐席
        switch (groupMemoryConfig.getSuccessStrategy()) {
            case 1:
                //一直等待记忆坐席(一直放音)
                return true;
            case 2:
                //超时转空闲坐席(放音一段时间后转其他空闲坐席)
                return true;
            case 3:
                //忙碌转空闲坐席
                break;
            default:
                return false;
        }
        return false;
    }

    /**
     * 溢出策略
     */
    private void overFlowHandler(CallInfo callInfo, String deviceId, GroupOverFlowInfo groupOverFlow){
        log.info("callId:{} handleType is overflow, overflowType:{}, overflowValue:{}", callInfo.getCallId(), groupOverFlow.getOverflowType(), groupOverFlow.getOverflowValue());
        callInfo.setOverflowCount(callInfo.getOverflowCount() + 1);
        /**
         * 溢出(1:group,2:ivr,3:vdn)
         */
        switch (groupOverFlow.getOverflowType()) {
            case 1:
                log.info("callId:{} overflow to group:{}", callInfo.getCallId(), callInfo.getCallId());
                next(ProcessEnum.GROUP_PROCESS,groupOverFlow.getOverflowValue(),deviceId,callInfo);
                break;
            case 2:
                log.info("callId:{} overflow to ivr:{}", callInfo.getCallId(), callInfo.getCallId());
                next(ProcessEnum.IVR_PROCESS,groupOverFlow.getOverflowValue(),deviceId,callInfo);
                break;

            case 3:
                log.info("callId:{} overflow to vdn:{}", callInfo.getCallId(), groupOverFlow.getOverflowValue());
                next(ProcessEnum.VDN_PROCESS,groupOverFlow.getOverflowValue(),deviceId,callInfo);
                break;
            default:
                break;
        }
    }
}
