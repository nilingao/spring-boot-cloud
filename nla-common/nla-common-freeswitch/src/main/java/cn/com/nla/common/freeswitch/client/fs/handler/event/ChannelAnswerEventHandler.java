package cn.com.nla.common.freeswitch.client.fs.handler.event;

import cn.com.nla.common.freeswitch.client.fs.handler.EventNextHandler;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 设备应答处理类
 */
@Slf4j
@Component
@EslEventName(EventNames.CHANNEL_ANSWER)
public class ChannelAnswerEventHandler implements EslEventHandler {

    @Resource
    private EventNextHandler eventNextHandler;
    @Override
    public void handle(String addr, EslEvent event) {
        log.info("进入事件 [设备应答处理类] CHANNEL_ANSWER");
        String uniqueId = EslEventUtil.getUniqueId(event);
        LocalDateTime answerTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(EslEventUtil.getEventDateTimestamp(event))/1000), ZoneId.systemDefault());//接通时间（毫秒值）
        CallInfo callInfo = RedisService.getCallInfoManager().findDeviceId(uniqueId);
        if(callInfo == null){
            log.warn("未获取应答消息");
            return;
        }
        //获取处理设备
        DeviceInfo deviceInfo = callInfo.getDeviceInfoMap().get(uniqueId);
        if(deviceInfo == null){
            log.warn("未获取处理设备");
            return;
        }
        //接听时间也是振铃结束时间
        deviceInfo.setAnswerTime(answerTime);
        deviceInfo.setRingEndTime(answerTime);
        callInfo.setAnswerCount(callInfo.getAnswerCount() + 1);
        if (StringUtils.isBlank(callInfo.getMediaHost())) {
            callInfo.setMediaHost(addr);
        }
        //下一波执行命令
        eventNextHandler.next(callInfo,event);
        RedisService.getCallInfoManager().put(callInfo);
    }
}