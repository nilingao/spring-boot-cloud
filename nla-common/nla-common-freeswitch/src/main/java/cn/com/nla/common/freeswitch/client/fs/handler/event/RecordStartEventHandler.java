package cn.com.nla.common.freeswitch.client.fs.handler.event;

import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 *  录音开始
 */
@Slf4j
@Component
@EslEventName(EventNames.RECORD_START)
public class RecordStartEventHandler  implements EslEventHandler {
    @Override
    public void handle(String addr, EslEvent event) {
        log.info("进入事件 [录音开始] RECORD_START");
        String uniqueId = EslEventUtil.getUniqueId(event);
        LocalDateTime answerTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(EslEventUtil.getEventDateTimestamp(event))/1000), ZoneId.systemDefault());//接通时间（毫秒值）
        CallInfo callInfo = RedisService.getCallInfoManager().findDeviceId(uniqueId);
        if (callInfo == null) {
            return;
        }
        DeviceInfo deviceInfo = callInfo.getDeviceInfoMap().get(uniqueId);
        deviceInfo.setRecordStartTime(answerTime);
    }
}
