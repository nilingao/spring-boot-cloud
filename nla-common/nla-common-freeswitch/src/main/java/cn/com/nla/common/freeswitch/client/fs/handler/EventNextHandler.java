package cn.com.nla.common.freeswitch.client.fs.handler;

import cn.com.nla.common.freeswitch.client.fs.handler.process.ProcessNextHandler;
import cn.com.nla.common.freeswitch.client.fs.handler.strategy.CallStrategyHandler;
import cn.com.nla.common.freeswitch.enums.fs.CallTypeEunm;
import cn.com.nla.common.freeswitch.enums.fs.NextTypeEnum;
import cn.com.nla.common.freeswitch.enums.fs.ProcessEnum;
import cn.com.nla.common.freeswitch.model.call.CallDetail;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.model.call.NextCommand;
import cn.com.nla.common.freeswitch.model.fs.*;
import cn.com.nla.common.freeswitch.model.message.*;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.service.FsService;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.RandomUtil;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.EslEventUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 下一步处理类
 */
@Slf4j
@Component
public class EventNextHandler {

    @Getter
    @Value("${fs.record.path:/usr/local/freeswitch}")
    protected String recordPath;
    @Getter
    @Value("${fs.record.file:wav}")
    protected String recordFile;
    @Resource
    private ProcessNextHandler processNextHandler;
    @Getter
    private final CallStrategyHandler callHandlerStrategy;
    public EventNextHandler(InboundClient inboundClient){
        callHandlerStrategy = new CallStrategyHandler(inboundClient);
    }

    public void next(CallInfo callInfo, EslEvent event){
        String uniqueId = EslEventUtil.getUniqueId(event);
        NextCommand nextCommand = callInfo.getNextCommands().isEmpty() ? null : callInfo.getNextCommands().get(0);
        if (nextCommand == null) {
            return;
        }
        //准备执行时删除这次处理的命令
        callInfo.getNextCommands().remove(nextCommand);
        switch (nextCommand.getNextType()) {
            case NEXT_CONSULT:
                //咨询通话（坐席转接）
                callOther(2,callInfo, callInfo.getDeviceInfoMap().get(uniqueId),event);
                break;
            case NEXT_TRANSFER_SUCCESS:
                //转接呼叫完成开始转接
                transferCall(callInfo, nextCommand, event);
                break;
            case NEXT_TRANSFER_CONFERENCE_CALL:
                //电话转接会议
                transferConferenceCall(callInfo, nextCommand, event);
                break;
            case NEXT_VDN:
                //进vdn
                matchVdnCode(event, callInfo, callInfo.getDeviceInfoMap().get(uniqueId));
                break;
            case NEXT_CALL_OTHER:
                //呼叫另外一侧
                callOther(1,callInfo, callInfo.getDeviceInfoMap().get(uniqueId),event);
                break;
            case NEXT_CALL_BRIDGE:
                //桥接 正常打电话
                callBridge(callInfo, callInfo.getDeviceInfoMap().get(uniqueId), nextCommand, event);
                break;
            case NEXT_ANSWER_CALL:
                //电话应答
                answerCall(callInfo, callInfo.getDeviceInfoMap().get(uniqueId), nextCommand, event);
                break;
            //另一部分
            default:
                log.warn("can not match command :{}, callId:{}", nextCommand.getNextType(), callInfo.getCallId());
                break;
        }
    }

    /**
     * 呼入电话进入vdn
     */
    private void matchVdnCode(EslEvent event, CallInfo callInfo, DeviceInfo deviceInfo) {
        String caller = EslEventUtil.getCallerCallerIdNumber(event);//主叫号码
        String called = EslEventUtil.getCallerDestinationNumber(event);//被叫号码
        VdnPhoneInfo vdnPhoneInfo = RedisService.getVdnPhoneManager().get(callInfo.getCallerDisplay());
        log.info("inbount caller:{} called:{} for vdnId:{}", caller, called, vdnPhoneInfo.getVdnId());
        CompanyInfo companyInfo = RedisService.getCompanyInfoManager().get(vdnPhoneInfo.getCompanyId());
        if (companyInfo == null || companyInfo.getStatus() == 0) {
            log.info("vdnPhone is not match:{}  {} ", caller, called);
            callHandlerStrategy.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).build());
            return;
        }
        processNextHandler.next(ProcessEnum.VDN_PROCESS, String.valueOf(vdnPhoneInfo.getVdnId()),deviceInfo.getDeviceId(),callInfo);
    }
    /**
     * 呼叫另外一侧
     * @param type 1.正常呼叫 2.转接呼叫
     */
    public void callOther(Integer type,CallInfo callInfo, DeviceInfo deviceInfo, EslEvent event){
        //被叫号码
        String called = callInfo.getCalled();
        String sdp = event.getEventHeaders().get("variable_switch_r_sdp");//获取sip中sdp信息
        //主叫组
        GroupInfo groupInfo = RedisService.getGroupInfoManager().get(callInfo.getGroupId());
        if(groupInfo == null){
            log.warn("呼叫另外一侧 未获取技能组信息 CallId：{}",callInfo.getCallId());
            callHandlerStrategy.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).build());
            return;
        } else if (groupInfo.getRecordType() == 1 && StringUtils.isEmpty(deviceInfo.getRecord())) {
            //振铃录音
            String record = String.format("%s/%s/%s_%s_%s.%s",recordPath,LocalDateTime.now().format(DateTimeFormatter.ofPattern(DatePattern.PURE_DATE_PATTERN)),callInfo.getCallId(),deviceInfo.getDeviceId(),Instant.now().getEpochSecond(),recordFile);
            callHandlerStrategy.handler(RecordCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(callInfo.getDeviceList().get(0)).playPath(record).build());
            deviceInfo.setRecord(record);
            deviceInfo.setRecordStartTime(deviceInfo.getAnswerTime());
            callInfo.getDeviceInfoMap().put(deviceInfo.getDeviceId(), deviceInfo);
        }
        String deviceId = RandomUtil.randomString(32);
        log.info("呼另外一侧电话: callId:{}  display:{}  called:{}  deviceId:{} ", callInfo.getCallId(), callInfo.getCalledDisplay(), callInfo.getCalled(), deviceId);
        //坐席内呼
        if (callInfo.getCallType() == CallTypeEunm.INNER_CALL) {
            AgentVoInfo agentVoInfo = FsService.getAgentService().getAgentByCompanyCode(callInfo.getCompanyId(),called);
            if(agentVoInfo== null){
                log.warn("呼叫另外一侧 未获取被叫客服 CallId：{}",callInfo.getCallId());
                callHandlerStrategy.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).build());
                return;
            }
            agentVoInfo = RedisService.getAgentInfoManager().get(agentVoInfo.getAgentKey());
            if(agentVoInfo== null){
                log.warn("呼叫另外一侧 被叫客服未上线 CallId：{}",callInfo.getCallId());
                callHandlerStrategy.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).build());
                return;
            }
            agentVoInfo.setCallId(callInfo.getCallId());
            agentVoInfo.setDeviceId(deviceId);
            RedisService.getAgentInfoManager().put(agentVoInfo);
        }
        RouteGateWayInfo routeGateWayInfo = RedisService.getCompanyInfoManager().getRouteGateWayInfo(callInfo.getCompanyId(), called);
        if (routeGateWayInfo == null) {
            log.warn("呼叫另外一侧 未获取被叫网关路由 callId:{}, called:{}", callInfo.getCallId(), callInfo.getCalled());
            callHandlerStrategy.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(deviceInfo.getDeviceId()).build());
            return;
        }
        callInfo.getDeviceList().add(deviceId);//存储被叫uuid
        DeviceInfo deviceInfo1 = new DeviceInfo();
        //1:坐席,2:客户,3:外线
        deviceInfo1.setDeviceType(callInfo.getCallType() == CallTypeEunm.INNER_CALL ? 1 : 2);
        //1:呼入,2:外呼,3:内呼,4:转接,5:咨询,6:监听,7:强插
        deviceInfo1.setCdrType(callInfo.getCallType() == CallTypeEunm.INNER_CALL ? 3 : 2);
        deviceInfo1.setCallId(callInfo.getCallId());
        deviceInfo1.setCalled(called);
        deviceInfo1.setDisplay(callInfo.getCalledDisplay());
        deviceInfo1.setCaller(callInfo.getCalledDisplay());
        deviceInfo1.setDeviceId(deviceId);
        deviceInfo1.setCallTime(LocalDateTime.now());
        deviceInfo1.setAgentKey(callInfo.getAgentKey());
        if(type == 1){
            //话机通后先应答拨打电话
            callInfo.getNextCommands().add(new NextCommand(deviceInfo.getDeviceId(), NextTypeEnum.NEXT_ANSWER_CALL, deviceInfo.getDeviceId()));
            //后桥接两端通道
            callInfo.getNextCommands().add(new NextCommand(deviceInfo.getDeviceId(), NextTypeEnum.NEXT_CALL_BRIDGE, deviceInfo1.getDeviceId()));
        }else if(type == 2){
            //语音通后开始转接
            callInfo.getNextCommands().add(new NextCommand(deviceInfo.getDeviceId(), NextTypeEnum.NEXT_TRANSFER_SUCCESS, deviceInfo1.getDeviceId()));
        }
        callInfo.getDeviceInfoMap().put(deviceId, deviceInfo1);
        RedisService.getDeviceInfoManager().putDeviceCallId(deviceId, callInfo.getCallId());
        /**
         * 呼叫外线，设置超时时间
         */
        callHandlerStrategy.handler(MakeCallModel.builder()
                .deviceId(deviceId)
                .sdp(sdp)
                .callId(callInfo.getCallId())
                .display(callInfo.getCaller())//主叫getCalledDisplay
                .calledDisplay(callInfo.getCalledDisplay())
                .called(called)//被叫
                .originateTimeout(groupInfo.getCallTimeOut())
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
    /**
     * 桥接电话
     *
     * @param callInfo
     * @param deviceInfo
     * @param nextCommand
     * @param event
     */
    private void callBridge(CallInfo callInfo, DeviceInfo deviceInfo, NextCommand nextCommand, EslEvent event) {
        log.info("开始桥接电话: callId:{} caller:{} called:{} device1:{}, device2:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCalled(), nextCommand.getDeviceId(), nextCommand.getNextValue());
        DeviceInfo deviceInfo1 = callInfo.getDeviceInfoMap().get(nextCommand.getDeviceId());
        DeviceInfo deviceInfo2 = callInfo.getDeviceInfoMap().get(nextCommand.getNextValue());
        LocalDateTime answerTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(EslEventUtil.getEventDateTimestamp(event))/1000), ZoneId.systemDefault());//接通时间（毫秒值）
        if (deviceInfo1.getBridgeTime() == null) {
            deviceInfo1.setBridgeTime(answerTime);
        }
        if (deviceInfo2.getBridgeTime() == null) {
            deviceInfo2.setBridgeTime(answerTime);
        }
        callHandlerStrategy.handler(BridgeCallModel.builder().mediaAddr(callInfo.getMediaHost()).oneDeviceId(nextCommand.getDeviceId()).twoDeviceId(nextCommand.getNextValue()).build());
        /**
         * 呼入电话，坐席接听后，需要桥接
         */
        if (callInfo.getCallType() == CallTypeEunm.INBOUND_CALL) {
            if (callInfo.getQueueStartTime() != null && callInfo.getQueueEndTime() == null && deviceInfo.getDeviceType() == 1) {
                callInfo.setQueueEndTime(deviceInfo.getAnswerTime());
                if (!CollectionUtils.isEmpty(callInfo.getCallDetails())) {
                    CallDetail callDetail = callInfo.getCallDetails().get(callInfo.getCallDetails().size() - 1);
                    if (callDetail != null) {
                        callDetail.setEndTime(deviceInfo.getAnswerTime());
                    }
                }
                //更新坐席应答次数
                AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().get(deviceInfo.getAgentKey());
                agentVoInfo.setTotalAnswerTimes(agentVoInfo.getTotalRingTimes() + 1);
                RedisService.getAgentInfoManager().put(agentVoInfo);
            }
        }
    }

    /**
     * 转接 坐席丶外线
     */
    public void transferCall(CallInfo callInfo, NextCommand nextCommand, EslEvent event) {
        String uniqueId = EslEventUtil.getUniqueId(event);
        long eventDate = Long.parseLong(EslEventUtil.getEventDateTimestamp(event)) / 1000;
        callHandlerStrategy.handler(TransferCallModel.builder()
                .mediaAddr(callInfo.getMediaHost())
                .recordFile(recordFile)
                .recordPath(recordPath)
                .eventDate(eventDate)
                .callInfo(callInfo)
                .oldDeviceId(nextCommand.getDeviceId())
                .formDeviceId(nextCommand.getNextValue())
                .toDeviceId(uniqueId)
                .build()
        );
        callInfo.getNextCommands().add(NextCommand.builder().nextType(NextTypeEnum.NORNAL).build());
    }

    /**
     * 电话转接会议
     * @param callInfo
     * @param nextCommand
     * @param event
     */
    private void transferConferenceCall(CallInfo callInfo, NextCommand nextCommand, EslEvent event) {
        //会议室时直接挂载
        CompanyConferenceInfo companyConferenceInfo = RedisService.getCompanyConferenceInfoManager().get(callInfo.getCompanyId(), nextCommand.getNextValue());
        if(companyConferenceInfo== null){
            log.warn("未获取会议室 CallId：{}",callInfo.getCallId());
            callHandlerStrategy.handler(HangupCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(nextCommand.getDeviceId()).build());
            return;
        }
        //转接会议
        long eventDate = Long.parseLong(EslEventUtil.getEventDateTimestamp(event)) / 1000;
        callHandlerStrategy.handler(TransferCallModel.builder()
                .mediaAddr(callInfo.getMediaHost())
                .isConference(true)
                .conferenceCode(companyConferenceInfo.getCode())
                .conferencePwd(companyConferenceInfo.getPassword())
                .recordFile(recordFile)
                .recordPath(recordPath)
                .eventDate(eventDate)
                .callInfo(callInfo)
                .oldDeviceId(null)
                .formDeviceId(nextCommand.getDeviceId())
                .toDeviceId(nextCommand.getDeviceId())
                .build());
    }
    //电话应答
    private void answerCall(CallInfo callInfo, DeviceInfo deviceInfo, NextCommand nextCommand, EslEvent event) {
        log.info("话机应答: callId:{} caller:{} called:{} device1:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCalled(), nextCommand.getDeviceId());
        callHandlerStrategy.handler(AnswerCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(nextCommand.getDeviceId()).build());
    }
}
