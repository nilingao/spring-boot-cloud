package cn.com.nla.common.freeswitch.client.fs.handler.event;

import cn.com.nla.common.freeswitch.client.fs.handler.process.ProcessNextHandler;
import cn.com.nla.common.freeswitch.model.call.CallInfo;
import cn.com.nla.common.freeswitch.model.call.DeviceInfo;
import cn.com.nla.common.freeswitch.model.call.NextCommand;
import cn.com.nla.common.freeswitch.redis.RedisService;
import link.thingscloud.freeswitch.esl.constant.EventNames;
import cn.com.nla.common.freeswitch.esl.EslEventName;
import cn.com.nla.common.freeswitch.esl.EslEventHandler;
import link.thingscloud.freeswitch.esl.transport.event.EslEvent;
import link.thingscloud.freeswitch.esl.util.EslEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 *  放音结束
 */
@Slf4j
@Component
@EslEventName(EventNames.PLAYBACK_STOP)
public class PlaybackStopEventHandler implements EslEventHandler {
    @Resource
    private ProcessNextHandler processNextHandler;
    @Override
    public void handle(String addr, EslEvent event) {
        log.info("进入事件  [放音结束]  PLAYBACK_STOP");
        String uniqueId = EslEventUtil.getUniqueId(event);
        CallInfo callInfo = RedisService.getCallInfoManager().findDeviceId(uniqueId);
        if (callInfo == null) {
            return;
        }
        DeviceInfo deviceInfo = callInfo.getDeviceInfoMap().get(uniqueId);
        if (deviceInfo == null || deviceInfo.getEndTime() != null) {
            return;
        }
        NextCommand nextCommand = callInfo.getNextCommands().isEmpty() ? null : callInfo.getNextCommands().get(0);
        if (nextCommand == null) {
            return;
        }
        processNextHandler.doNextCommand(callInfo, deviceInfo, nextCommand);
        log.info("callId:{} playstop, nextType:{}", callInfo.getCallId(), nextCommand.getNextType());
    }
}
