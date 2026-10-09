package cn.com.nla.common.freeswitch.client.fs.handler.event;

import cn.com.nla.common.freeswitch.client.fs.handler.EventNextHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.StrategyHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.strategy.CallStrategyHandler;
import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.enums.fs.*;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.model.call.NextCommand;
import cn.com.nla.common.freeswitch.model.fs.*;
import cn.com.nla.common.freeswitch.model.message.AnswerCallModel;
import cn.com.nla.common.freeswitch.model.message.HangupCallModel;
import cn.com.nla.common.freeswitch.model.notice.CallMessage;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.service.FsService;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.RandomUtil;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 *  设备话机振铃
 */
@Slf4j
@Component
@EslEventName(EventNames.CHANNEL_PARK)
public class ChannelParkEventHandler implements EslEventHandler {

    @Resource
    private EventNextHandler eventNextHandler;

    private final StrategyHandler strategyHandler;
    public ChannelParkEventHandler(InboundClient inboundClient){
        strategyHandler = new CallStrategyHandler(inboundClient);
    }
    @Override
    public void handle(String addr, EslEvent event) {
        String direction =null;
        String hangup = event.getEventHeaders().get("variable_sip_hangup_phrase");//是否挂机,OK
        if (Constant.OK.equals(hangup)) {
            log.info("进入事件 [ 设备话机振铃 ] CHANNEL_PARK");
            return;
        }else {
            direction = EslEventUtil.getCallerDirection(event);
        }
        log.info("进入事件 [ 设备话机振铃 :{} ] CHANNEL_PARK",direction);
        log.info("channel park:{}", event);
        String deviceId = EslEventUtil.getUniqueId(event);
        String sipProtocol = event.getEventHeaders().get("variable_sip_via_protocol");//sip信令协议
        LocalDateTime answerTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(EslEventUtil.getEventDateTimestamp(event))/1000), ZoneId.systemDefault());//接通时间（毫秒值）
        CallInfo callInfo = RedisService.getCallInfoManager().findDeviceId(deviceId);
        DeviceInfo deviceInfo = null;
        if (callInfo == null && DirectionEnum.INBOUND.name().equals(direction.toUpperCase())) {
            inbound(deviceId,addr,event);
            return;
        }
        if (callInfo == null || callInfo.getHangupDir() != null) {
            return;
        }
        if (deviceInfo == null) {
            deviceInfo = callInfo.getDeviceInfoMap().get(deviceId);
            if(deviceInfo == null){
                return;
            }
        }
        if (hangup != null) {
            return;
        }
        if (deviceInfo.getAnswerTime() != null && deviceInfo.getState() != null) {
            return;
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(deviceInfo.getAgentKey());
        if (agentVoInfo == null) {
            return;
        }
        CallMessage ringEntity = new CallMessage();
        ringEntity.setCaller(callInfo.getCaller());
        ringEntity.setCalled(callInfo.getCalled());
        ringEntity.setGroupId(callInfo.getGroupId());
        ringEntity.setCallId(callInfo.getCallId());
        if (deviceInfo.getState() != null) {
            switch (deviceInfo.getState()) {
                case "HOLD":
                    ringEntity.setAgentState(AgentStateEnum.HOLD);
                    agentVoInfo.setAgentState(AgentStateEnum.HOLD);
                    FsService.getSendAgentMessage().sendMessage(AgentStateEnum.HOLD, agentVoInfo,ringEntity);
                case "INSERT":
                    ringEntity.setAgentState(AgentStateEnum.INSERT);
                    agentVoInfo.setAgentState(AgentStateEnum.INSERT);
                    FsService.getSendAgentMessage().sendMessage(AgentStateEnum.OUT_CALLER_RING, agentVoInfo,ringEntity);
                case "LISTEN":
                    ringEntity.setAgentState(AgentStateEnum.LISTEN);
                    agentVoInfo.setAgentState(AgentStateEnum.LISTEN);
                    FsService.getSendAgentMessage().sendMessage(AgentStateEnum.OUT_CALLER_RING, agentVoInfo,ringEntity);
                default:
                    break;
            }
            return;
        }
        if (deviceInfo.getRingStartTime() != null) {
            return;
        }
        deviceInfo.setRingStartTime(answerTime);
        DirectionEnum directionEnum = callInfo.getDirection();
        log.info("callId:{} device:{} park deviceType:{} cdrType:{} direction:{} sipProtocol:{}", deviceInfo.getCallId(), deviceInfo.getDeviceId(), deviceInfo.getDeviceType(), deviceInfo.getCdrType(), direction, sipProtocol);
        ringEntity.setDirection(callInfo.getDirection());
        if (directionEnum.equals(DirectionEnum.OUTBOUND)) {
            outboundCall(callInfo, deviceInfo, agentVoInfo, ringEntity);
        } else if (directionEnum.equals(DirectionEnum.INBOUND)) {
            //呼入振铃
            agentVoInfo.setBeforeState(agentVoInfo.getAgentState());
            agentVoInfo.setBeforeTime(agentVoInfo.getStateTime());
            agentVoInfo.setStateTime(Instant.now().getEpochSecond());
            agentVoInfo.setAgentState(AgentStateEnum.IN_CALL_RING);
            ringEntity.setAgentState(AgentStateEnum.IN_CALL_RING);
            if (agentVoInfo.getHiddenCustomer() == 1) {
                ringEntity.setCaller(ringEntity.getCaller());
            }
        }
        RedisService.getAgentInfoManager().put(agentVoInfo);
    }
    //呼入电话处理
    private void inbound(String uniqueId,String addr, EslEvent event){
        String sipPort = event.getEventHeaders().get("variable_sip_via_port");//硬话机发起呼叫时携带
        String sipContactPort = event.getEventHeaders().get("variable_sip_contact_port");
        String callId = event.getEventHeaders().get("variable_sip_call_id");//通话唯一标识
        if (sipPort == null) {
            //呼入
            inboundCall(addr,uniqueId,event,callId);
        }else if (sipContactPort.equals(sipPort)) {
            //硬话机外呼
            sipOutboundCall(addr,uniqueId,event,callId);
        }
    }
    /**
     * b-腿 呼入
     * @param event
     */
    private void inboundCall(String addr,String deviceId,EslEvent event,String callId) {
        String localMediaIp = event.getEventHeaders().get("variable_local_media_ip");//fs 服务地址
        String caller = event.getEventHeaders().get("variable_sip_from_user");//主叫号码
        String called = event.getEventHeaders().get("Caller-Destination-Number");//被叫号码
        String contactUri = event.getEventHeaders().get("variable_sip_contact_uri");
        String sdp = event.getEventHeaders().get("variable_switch_r_sdp");//获取sip中sdp信息
        //客户号码归属地
        String numberLocaton = "";
        log.info("inbount callId:{} park, caller:{}, called:{}, deviceId:{}, uri:{}", callId, caller, called, deviceId, contactUri);
        VdnPhoneInfo vdnPhoneInfo = RedisService.getVdnPhoneManager().get(called);
        if (vdnPhoneInfo == null) {
            log.error("inbount callId:{} called:{} is not match for vdn", callId, called);
            CallLogInfo callLog = new CallLogInfo();
            callLog.setCallId(callId);
            callLog.setCreateTime(LocalDateTime.now());
            callLog.setUpdateTime(LocalDateTime.now());
            callLog.setEndTime(LocalDateTime.now());
            callLog.setCaller(caller);
            callLog.setCalled(called);
            callLog.setNumberLocation(numberLocaton);
            callLog.setCallType(CallTypeEunm.INBOUND_CALL.name());
            callLog.setMediaHost(addr);
            callLog.setAnswerCount(1);
            callLog.setAnswerFlag(3);
            callLog.setDirection(DirectionEnum.INBOUND.name());
            callLog.setHangupDir(3);
            callLog.setHangupCode(CauseEnums.VDN_ERROR.getHuangupCode());
            callLog.setMonthTime(callLog.getCreateTime() == null ? null : callLog.getCreateTime().format(DateTimeFormatter.ofPattern(DatePattern.SIMPLE_MONTH_PATTERN)));
            FsService.getCallCdrService().saveOrUpdateCallLog(callLog);
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(addr).deviceId(deviceId).build());
            return;
        }
        CallInfo callInfo = CallInfo.builder()
                .callId(callId)
                .callType(CallTypeEunm.INBOUND_CALL)
                .direction(DirectionEnum.INBOUND)
                .callTime(LocalDateTime.now())
                .numberLocation(numberLocaton)
                .sipSdp(sdp)
                //接入号码
                .callerDisplay(called)
                .companyId(vdnPhoneInfo.getCompanyId())
                .mediaHost(addr)
                .ctiHost(localMediaIp)
                .build();

        DeviceInfo deviceInfo = DeviceInfo.builder()
                .callId(callId)
                .deviceId(deviceId)
                .caller(caller)
                .called(called)
                .callerLocation(numberLocaton)
                .callTime(callInfo.getCallTime())
                .deviceType(2)
                .cdrType(1)
                .build();
        CompanyInfo companyInfo = RedisService.getCompanyInfoManager().get(vdnPhoneInfo.getCompanyId());
        callInfo.setHiddenCustomer(companyInfo.getHiddenCustomer());
        callInfo.setCdrNotifyUrl(companyInfo.getNotifyUrl());
        callInfo.getDeviceInfoMap().put(deviceId, deviceInfo);
        callInfo.getDeviceList().add(deviceId);
        callInfo.getNextCommands().add(new NextCommand(deviceId, NextTypeEnum.NEXT_VDN, null));
        RedisService.getCallInfoManager().put(callInfo);
        RedisService.getDeviceInfoManager().putDeviceCallId(deviceId, callId);
        strategyHandler.handler(AnswerCallModel.builder().isActive(true).mediaAddr(addr).deviceId(deviceId).build());
    }

    /**
     * a-腿 呼出
     * @param event
     */
    private void sipOutboundCall(String addr,String deviceId,EslEvent event,String callId) {
        String caller = event.getEventHeaders().get("variable_sip_from_user");//主叫号码
        String called = event.getEventHeaders().get("Caller-Destination-Number");//被叫号码
        String sdp = event.getEventHeaders().get("variable_switch_r_sdp");//获取sip中sdp信息
        AgentVoInfo agent = FsService.getAgentService().getAgentBySip(caller);
        if (agent == null || agent.getGroupId() == null) {
            log.warn("sipOutbound callId:{}  sip:{} called:{}", callId, caller, called);
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(addr).deviceId(deviceId).build());
            return;
        }
        AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(agent.getAgentKey());
        if(agentVoInfo == null){
            RedisService.getAgentInfoManager().put(agent);
        }else {
            agent = agentVoInfo;
        }
        //获取显号
        GroupInfo groupInfo = RedisService.getGroupInfoManager().get(agent.getGroupId());
        if (groupInfo == null || CollectionUtils.isEmpty(groupInfo.getCalledDisplays())) {
            log.warn("callId:{}, agent:{}, group is null", callId, agent.getAgentKey());
            strategyHandler.handler(HangupCallModel.builder().mediaAddr(addr).deviceId(deviceId).build());
            return;
        }
        log.info("sipOutbound callId:{}  sip:{} called:{}", callId, caller, called);
        //主叫显号（被叫如何是外呼则取分组，如何是内呼或会议，取当前客服code ）
        String calledDisplay = groupInfo.getCalledDisplays().get(groupInfo.getCalledDisplays().size()==1?0:RandomUtil.randomInt(0,groupInfo.getCalledDisplays().size()-1));
        CallTypeEunm callTypeEunm = CallTypeEunm.SIP_OUTBOUND_CALL;
        //判定被叫是那种呼叫
        if(called.startsWith(Constant.AGENT_SIP_PREFIX)){//内呼坐席
            called = called.substring(Constant.AGENT_SIP_PREFIX.length());
            callTypeEunm = CallTypeEunm.INNER_CALL;
            calledDisplay = agent.getAgentId();
        }else if(called.startsWith(Constant.CONFERENCE_ID_PREFIX)){//呼叫会议
            called = called.substring(Constant.CONFERENCE_ID_PREFIX.length());
            callTypeEunm = CallTypeEunm.CONFERENCE_CALL;
            calledDisplay = called;
            callId = called;//设置此call编号为会议编号
        }
        CallInfo callInfo  = RedisService.getCallInfoManager().get(callId);
        if(callInfo == null){
            callInfo = CallInfo.builder()
                .sipSdp(sdp)
                .callId(callId)
                .callType(callTypeEunm)
                .conference(callTypeEunm==CallTypeEunm.CONFERENCE_CALL?called:null)
                .direction(DirectionEnum.OUTBOUND)
                .callTime(LocalDateTime.now())
                .caller(caller)
                .called(called)
                .companyId(agent.getCompanyId())
                .mediaHost(addr)
                .callerDisplay(called)
                .calledDisplay(calledDisplay)
                .groupId(groupInfo.getId())
                .agentKey(agent.getAgentKey())
                .build();
        }
        DeviceInfo deviceInfo = DeviceInfo.builder()
                .callId(callId)
                .conference(callTypeEunm==CallTypeEunm.CONFERENCE_CALL?called:null)
                .deviceId(deviceId)
                .agentKey(agent.getAgentKey())
                .caller(caller)
                .called(called)
                .callTime(callInfo.getCallTime())
                .deviceType(1)
                .cdrType(2)
                .build();
        agent.setDeviceId(deviceId);
        CompanyInfo companyInfo = RedisService.getCompanyInfoManager().get(agent.getCompanyId());
        callInfo.setHiddenCustomer(companyInfo.getHiddenCustomer());
        callInfo.setCdrNotifyUrl(companyInfo.getNotifyUrl());
        callInfo.getDeviceInfoMap().put(deviceId, deviceInfo);
        callInfo.getDeviceList().add(deviceId);
        if(callTypeEunm == CallTypeEunm.CONFERENCE_CALL){
            //先应答拨打电话
            callInfo.getNextCommands().add(new NextCommand(deviceInfo.getDeviceId(), NextTypeEnum.NEXT_ANSWER_CALL, deviceInfo.getDeviceId()));
            //再桥接电话与会议
            callInfo.getNextCommands().add(new NextCommand(deviceInfo.getDeviceId(), NextTypeEnum.NEXT_TRANSFER_CONFERENCE_CALL, called));
        }else {
            callInfo.getNextCommands().add(new NextCommand(deviceInfo.getDeviceId(), NextTypeEnum.NEXT_CALL_OTHER, null));
        }
        //直接执行下一步操作
        RedisService.getDeviceInfoManager().putDeviceCallId(deviceId, callId);
        RedisService.getDeviceInfoManager().putCallerCallId(caller, callId);
        eventNextHandler.next(callInfo,event);
        RedisService.getCallInfoManager().put(callInfo);
    }

    /**
     * 外呼
     *
     * @param callInfo
     * @param deviceInfo
     * @param agentVoInfo
     * @param ringEntity
     */
    private void outboundCall(CallInfo callInfo, DeviceInfo deviceInfo, AgentVoInfo agentVoInfo, CallMessage ringEntity) {
        if (1 == deviceInfo.getDeviceType()) {
            CallTypeEunm callType = callInfo.getCallType();
            if (callType == CallTypeEunm.OUTBOUNT_CALL) {
                if (deviceInfo.getCdrType() == 4) {
                    //外呼转接坐席
                    ringEntity.setAgentState(AgentStateEnum.TRANSFER_CALL_RING);
                    if (agentVoInfo.getHiddenCustomer() == 1) {
                        ringEntity.setCalled(ringEntity.getCalled());
                    }
                    FsService.getSendAgentMessage().sendMessage(AgentStateEnum.TRANSFER_CALL_RING, agentVoInfo,ringEntity);
                } else if (deviceInfo.getCdrType() == 5) {
                    //外呼转接坐席
                    ringEntity.setAgentState(AgentStateEnum.CONSULT_CALL_RING);
                    if (agentVoInfo.getHiddenCustomer() == 1) {
                        ringEntity.setCalled(ringEntity.getCalled());
                    }
                    FsService.getSendAgentMessage().sendMessage(AgentStateEnum.CONSULT_CALL_RING, agentVoInfo,ringEntity);

                } else if (deviceInfo.getCdrType() == 2) {
                    //先判断坐席是否接通，如果已经接通，则返回当前接通后的状态
                    if (deviceInfo.getAnswerTime() != null) {
                        AgentStateEnum agentState = agentVoInfo.getAgentState();
                        switch (agentState) {
                            case CONSULT:
                                ringEntity.setAgentState(AgentStateEnum.TRANSFER_CALL);
                                if (agentVoInfo.getHiddenCustomer() == 1) {
                                    ringEntity.setCalled(ringEntity.getCalled());
                                }
                                FsService.getSendAgentMessage().sendMessage(AgentStateEnum.TALKING, agentVoInfo,ringEntity);
                                break;

                            case TRANSFER:

                                break;
                        }
                        return;
                    }
                    //外呼坐席振铃
                    ringEntity.setAgentState(AgentStateEnum.OUT_CALLER_RING);
                    if (agentVoInfo.getHiddenCustomer() == 1) {
                        ringEntity.setCalled(ringEntity.getCalled());
                    }
                    FsService.getSendAgentMessage().sendMessage(AgentStateEnum.OUT_CALLER_RING, agentVoInfo,ringEntity);
                }
            } else if (callType == CallTypeEunm.INNER_CALL) {
                //内呼坐席振铃
                ringEntity.setAgentState(AgentStateEnum.IN_CALL_RING);
            }
        } else {
            //外呼被叫振铃
            ringEntity.setAgentState(AgentStateEnum.OUT_CALLED_RING);
            if (agentVoInfo.getHiddenCustomer() == 1) {
                ringEntity.setCalled(ringEntity.getCalled());
            }
            FsService.getSendAgentMessage().sendMessage(AgentStateEnum.OUT_CALLED_RING, agentVoInfo,ringEntity);
        }
        RedisService.getCallInfoManager().put(callInfo);
    }
}
