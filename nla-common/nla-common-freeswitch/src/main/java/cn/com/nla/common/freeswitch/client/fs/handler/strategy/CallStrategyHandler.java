package cn.com.nla.common.freeswitch.client.fs.handler.strategy;

import cn.com.nla.common.freeswitch.enums.ConstEnum;
import cn.com.nla.common.freeswitch.enums.RespCode;
import cn.com.nla.common.freeswitch.common.fs.Constant;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.exception.BusinessException;
import cn.com.nla.common.freeswitch.model.MessageModel;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.model.fs.GroupInfo;
import cn.com.nla.common.freeswitch.model.message.*;
import cn.com.nla.common.freeswitch.client.fs.handler.StrategyHandler;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.utils.FreeswitchUtils;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import link.thingscloud.freeswitch.esl.InboundClient;
import link.thingscloud.freeswitch.esl.constant.Constants;
import link.thingscloud.freeswitch.esl.transport.SendMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 拨打电话相关
 */
@Slf4j
public class CallStrategyHandler extends StrategyHandler {
    private final static String videoCode = "^^:OPUS:G722:PCMU:PCMA:H264:VP8";//视频通话
    private final static String audioCode = "^^:OPUS:G722:PCMU:PCMA";//音频通话

    public CallStrategyHandler(InboundClient inboundClient) {
        super(inboundClient);
    }

    @Override
    public void handler(MessageModel model){
       if(model instanceof AnswerCallModel){
           log.info("应答电话参数 : model：{}",model);
           answerCallHandler((AnswerCallModel)model);
       }else if(model instanceof BridgeCallModel){
           log.info("桥接处理参数 : model：{}",model);
           bridgeCallHandler((BridgeCallModel)model);
       }else if (model instanceof HangupCallModel) {
           log.info("挂断电话参数 : model：{}",model);
           hangupCallHandler((HangupCallModel)model);
       }else if (model instanceof MakeCallModel) {
           log.info("外呼消息参数 : model：{}",model);
           makeCallHandler((MakeCallModel)model);
       }else if (model instanceof PlayBackCallModel) {
           log.info("语音播放参数 : model：{}",model);
           playBackCallHandler((PlayBackCallModel)model);
       }else if (model instanceof ReceiveDtmfModel) {
           log.info("播放按键参数 : model：{}",model);
           receiveDtmfHandler((ReceiveDtmfModel)model);
       }else if (model instanceof RecordCallModel) {
           log.info("录音参数 : model：{}",model);
           recordCallHandler((RecordCallModel)model);
       }else if (model instanceof TransferCallModel) {
           log.info("转接电话参数 : model：{}",model);
           transferCallHandler((TransferCallModel)model);
       } else {
           log.error("暂未有此消息处理 : model：{}",model);
       }
    }


    /**
     * 应答电话处理
     */
    private void answerCallHandler(AnswerCallModel answerCallModel){
        if(StringUtils.isEmpty(answerCallModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址");
        }else if(StringUtils.isEmpty(answerCallModel.getDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话设备");
        }
        //发送应答事件
        if(answerCallModel.isActive()){
            inboundClient.sendSyncApiCommand(answerCallModel.getMediaAddr(), Constant.UUID_PHONE_EVENT,String.format("%s talk",answerCallModel.getDeviceId()));
        }else {
            inboundClient.sendMessage(answerCallModel.getMediaAddr(), new SendMsg(answerCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.ANSWER));
        }
    }
    /**
     * 桥接处理
     * 1.先 originate 呼叫 A
     * 2.再 originate 呼叫 B
     */
    private void bridgeCallHandler(BridgeCallModel bridgeCallModel){
        if(StringUtils.isEmpty(bridgeCallModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址 ");
        }else if(StringUtils.isEmpty(bridgeCallModel.getOneDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话通话设备 ");
        }else if(StringUtils.isEmpty(bridgeCallModel.getTwoDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话桥接设备 ");
        }
        inboundClient.sendMessage(bridgeCallModel.getMediaAddr(), new SendMsg(bridgeCallModel.getOneDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.PARK_AFTER_BRIDGE).addGenericLine("async","true"));
        inboundClient.sendMessage(bridgeCallModel.getMediaAddr(), new SendMsg(bridgeCallModel.getOneDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.HANGUP_AFTER_BRIDGE).addGenericLine("async","true"));
        inboundClient.sendMessage(bridgeCallModel.getMediaAddr(), new SendMsg(bridgeCallModel.getTwoDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.HANGUP_AFTER_BRIDGE).addGenericLine("async","true"));
        inboundClient.sendMessage(bridgeCallModel.getMediaAddr(), new SendMsg(bridgeCallModel.getTwoDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.PARK_AFTER_BRIDGE).addGenericLine("async","true"));
        //开始桥接
        inboundClient.sendSyncApiCommand(bridgeCallModel.getMediaAddr(), Constants.UUID_BRIDGE,String.format("%s %s",bridgeCallModel.getOneDeviceId(),bridgeCallModel.getTwoDeviceId()));
    }

    /**
     * 挂断电话处理
     */
    private void hangupCallHandler(HangupCallModel hangupCallModel) {
        if(StringUtils.isEmpty(hangupCallModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址");
        }else if(StringUtils.isEmpty(hangupCallModel.getDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话设备");
        }
        if(hangupCallModel.isUuidKill()){
            inboundClient.sendMessage(hangupCallModel.getMediaAddr(), new SendMsg(hangupCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.HANGUP).addExecuteAppArg(Constant.NORMAL_CLEARING));
        }else {
            inboundClient.sendSyncApiCommand(hangupCallModel.getMediaAddr(),Constant.UUID_KILL,hangupCallModel.getDeviceId());
        }
    }

    /**
     * 外呼消息处理
     */
    private void makeCallHandler(MakeCallModel makeCallModel) {
        //网关信息
        MakeCallModel.RouteGatewayModel gatewayModel = makeCallModel.getGatewayModel();
        if(gatewayModel == null){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取发送 未获取网关信息 ");
        }
        if (StringUtils.isBlank(gatewayModel.getMediaHost())) {
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取发送 host ");
        }
        //主叫
        String display = makeCallModel.getDisplay();
        if (StringUtils.isNotBlank(gatewayModel.getCallerPrefix())) {
            display = gatewayModel.getCallerPrefix() + display;
        }
        //被叫
        String called = makeCallModel.getCalled() + Constant.AT + gatewayModel.getMediaHost() + Constant.CO + gatewayModel.getMediaPort ();
        if (StringUtils.isNotBlank(gatewayModel.getCalledPrefix())) {
            called = gatewayModel.getCalledPrefix() + called;
        }
        //添加传入sip请求头
        List<String> sipHeaders = new ArrayList<>();
        if(makeCallModel.getSipHeaderList() != null && !makeCallModel.getSipHeaderList().isEmpty()){
            sipHeaders.addAll(makeCallModel.getSipHeaderList());
        }
        //添加网关的sip请求头
        if(gatewayModel.getSipHeaderList() != null && !gatewayModel.getSipHeaderList().isEmpty()){
            sipHeaders.addAll(gatewayModel.getSipHeaderList());
        }

        Map<String, Object> sipParams = new HashMap<>();
        sipParams.put("callId", makeCallModel.getCallId());//暂不知道作用
        sipParams.put("deviceId",makeCallModel.getDeviceId());//暂不知道作用
        sipParams.put("caller", display);
        sipParams.put("called", called);

        Map<String, Object> fsParams=new HashMap<>();
        fsParams.put("return_ring_ready",true);
        fsParams.put("sip_sticky_contact",true);// 该参数主要用于固定住contact地址，防止在sip协商过程中被对方的内网地址所改写。
        fsParams.put("sip_contact_user",display);
        fsParams.put("ring_asr",true);
        fsParams.put("absolute_codec_string",(StringUtils.isNoneEmpty(makeCallModel.getSdp()) && makeCallModel.getSdp().contains("m=video"))?videoCode:audioCode);//判断给 音频或视频 编码
        fsParams.put("origination_caller_id_number",display);
        fsParams.put("origination_caller_id_name",makeCallModel.getCalledDisplay());
        fsParams.put("origination_uuid",makeCallModel.getDeviceId());
        if(makeCallModel.getOriginateTimeout() !=null){
            fsParams.put("originate_timeout", makeCallModel.getOriginateTimeout());
        }
        //添加sip请求头信息
        if(!sipHeaders.isEmpty()){
            Map<String, String> collect = sipHeaders.stream().filter(StringUtils::isNotBlank).map(o -> Constant.SIP_HEADER + FreeswitchUtils.expression(o, sipParams)).collect(Collectors.toMap(o -> o.split(Constant.EQ)[0], o -> o.split(Constant.EQ)[1]));
            fsParams.putAll(collect);
        }
        StringBuffer builder = new StringBuffer();
        builder.append(fsParams.toString().replaceAll(" ", ""));
        builder.append(Constant.SOFIA + Constant.SK).append(gatewayModel.getProfile()).append(Constant.SK);
        builder.append(called);
        builder.append(Constant.PARK);
        //随机获取一个Fs链接地址
        String addr = inboundClient.option().serverAddrOption().random();
        log.info("外呼消息处理 发送参数: addr：{}，command：{}，arg：{}",addr,Constant.ORIGINATE,builder.toString());
        inboundClient.sendAsyncApiCommand(addr, Constant.ORIGINATE, builder.toString());
    }

    /**
     * 语音播放处理
     */
    private void playBackCallHandler(PlayBackCallModel playBackCallModel) {
        if(StringUtils.isEmpty(playBackCallModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址");
        }else if(StringUtils.isEmpty(playBackCallModel.getDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话设备");
        }
        if(!playBackCallModel.isDown()){//打开
            if(StringUtils.isEmpty(playBackCallModel.getPlayPath())){
                throw new BusinessException(RespCode.CODE_2.getValue(),"未获取语音文件地址");
            }
            inboundClient.sendMessage(playBackCallModel.getMediaAddr(), new SendMsg(playBackCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.PLAYBACK_TERMINATORS));
            inboundClient.sendMessage(playBackCallModel.getMediaAddr(), new SendMsg(playBackCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.PLAYBACK_DELIMITER));
            inboundClient.sendMessage(playBackCallModel.getMediaAddr(), new SendMsg(playBackCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.TTS_ENGINE));
            inboundClient.sendMessage(playBackCallModel.getMediaAddr(), new SendMsg(playBackCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.PLAYBACK).addExecuteAppArg(playBackCallModel.getPlayPath()));
        }else {//关闭
            inboundClient.sendMessage(playBackCallModel.getMediaAddr(),new SendMsg(playBackCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.BREAK));
        }
    }
    /**
     * 播放按键导航音处理
     */
    private void receiveDtmfHandler(ReceiveDtmfModel receiveDtmfModel) {
        if(StringUtils.isEmpty(receiveDtmfModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址");
        }else if(StringUtils.isEmpty(receiveDtmfModel.getDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话设备");
        }
        //开始处理
        inboundClient.sendMessage(receiveDtmfModel.getMediaAddr(), new SendMsg(receiveDtmfModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg("playback_delimiter=!"));
        inboundClient.sendMessage(receiveDtmfModel.getMediaAddr(), new SendMsg(receiveDtmfModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName("play_and_get_digits").addExecuteAppArg("3 3 2 5000 # 1295e6a58f9e2115332666.wav silence_stream://250 SYMWRD_DTMF_RETURN [\\*0-9#]+ 3000"));
    }

    /**
     * 录音处理
     */
    private void recordCallHandler(RecordCallModel recordCallModel) {
        if(StringUtils.isEmpty(recordCallModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址");
        }else if(StringUtils.isEmpty(recordCallModel.getDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话设备");
        }else if(StringUtils.isEmpty(recordCallModel.getPlayPath())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取录音地址");
        }
        //设置8kHz采样率
        inboundClient.sendMessage(recordCallModel.getMediaAddr(), new SendMsg(recordCallModel.getDeviceId()).addCallCommand(Constant.EXECUTE).addExecuteAppName(Constant.SET).addExecuteAppArg(Constant.RECORD_SAMPLE_RATE+recordCallModel.getSampleRate()));
        //双声道录音,默认是单声道录音
        inboundClient.sendSyncApiCommand(recordCallModel.getMediaAddr(), Constants.UUID_SETVAR,String.format("%s%s",recordCallModel.getDeviceId(),Constant.RECORD_STEREO));
        //开始录音
        inboundClient.sendSyncApiCommand(recordCallModel.getMediaAddr(), Constants.UUID_RECORD,String.format("%s %s %s", recordCallModel.getDeviceId(), Constant.START, recordCallModel.getPlayPath()));
    }


    /**
     * 转接电话处理
     */
    public void transferCallHandler(TransferCallModel transferCallModel) {
        if(StringUtils.isEmpty(transferCallModel.getMediaAddr())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取通话fs设备地址");
        }else if(StringUtils.isEmpty(transferCallModel.getFormDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取现通话设备1");
        }
        else if(StringUtils.isEmpty(transferCallModel.getToDeviceId())){
            throw new BusinessException(RespCode.CODE_2.getValue(),"未获取现通话设备2");
        }
        CallInfo callInfo = transferCallModel.getCallInfo();
        //此命令将 transferCallModel.getFormDeviceId() 转接的电话挂起，等待程序执行操作
        // both 通话双方都转到第三方 bleg 的通话的对方转到第三方,<uuid>自己会挂断
        if(transferCallModel.isConference()){
            String format = String.format("%s conference:%s@default+%s inline ", transferCallModel.getFormDeviceId(),transferCallModel.getConferenceCode(),transferCallModel.getConferencePwd());
            //发起转接
            inboundClient.sendSyncApiCommand(transferCallModel.getMediaAddr(), Constants.UUID_TRANSFER,format);
        }else {
            String format = String.format("%s -both 'set:hangup_after_bridge=false,set:park_after_bridge=true,park:' inline ", transferCallModel.getFormDeviceId());
            //发起拆线
            inboundClient.sendSyncApiCommand(transferCallModel.getMediaAddr(), Constants.UUID_TRANSFER,format);
            log.info("开始桥接电话: callId:{} caller:{} called:{} device1:{}, device2:{}", callInfo.getCallId(), callInfo.getCaller(), callInfo.getCalled(), transferCallModel.getFormDeviceId(), transferCallModel.getToDeviceId());
            bridgeCallHandler(BridgeCallModel.builder().mediaAddr(callInfo.getMediaHost()).oneDeviceId(transferCallModel.getFormDeviceId()).twoDeviceId(transferCallModel.getToDeviceId()).build());
        }
        //挂机原设备
        if(StringUtils.isNotBlank(transferCallModel.getOldDeviceId())){
            //最后挂断原有电话
            hangupCallHandler(HangupCallModel.builder().mediaAddr(transferCallModel.getMediaAddr()).deviceId(transferCallModel.getOldDeviceId()).isUuidKill(false).build());
        }
        //是否有录音文件，有就播放
        if(StringUtils.isNoneEmpty(callInfo.getGroupId())){
            GroupInfo groupInfo = RedisService.getGroupInfoManager().get(callInfo.getGroupId());
            if(groupInfo != null  && ConstEnum.Flag.YES.getValue() == groupInfo.getRecordType()){
                DeviceInfo deviceInfo = callInfo.getDeviceInfoMap().get(transferCallModel.getToDeviceId());
                if(deviceInfo != null){
                    String record = String.format("%s/%s/%s_%s_%s.%s",transferCallModel.getRecordPath(), LocalDateTime.now().format(DateTimeFormatter.ofPattern(DatePattern.PURE_DATE_PATTERN)),callInfo.getCallId(),transferCallModel.getToDeviceId(), Instant.now().getEpochSecond(),transferCallModel.getRecordFile());
                    recordCallHandler(RecordCallModel.builder().mediaAddr(callInfo.getMediaHost()).deviceId(transferCallModel.getToDeviceId()).playPath(record).build());
                    deviceInfo.setRecord(record);
                    deviceInfo.setRecordTime(transferCallModel.getEventDate()== null?DateUtil.currentSeconds():transferCallModel.getEventDate());
                }
            }
        }
        //是否播放回音
        if(StringUtils.isBlank(callInfo.getConference())){
            playBackCallHandler(PlayBackCallModel.builder().mediaAddr(transferCallModel.getMediaAddr()).deviceId(transferCallModel.getFormDeviceId()).playPath("hold.wav").build());
            DeviceInfo consultDevice = callInfo.getDeviceInfoMap().get(transferCallModel.getFormDeviceId());
            consultDevice.setState(AgentStateEnum.HOLD.name());
        }
    }
}
