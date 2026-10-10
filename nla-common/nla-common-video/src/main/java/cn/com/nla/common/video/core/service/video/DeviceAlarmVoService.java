package cn.com.nla.common.video.core.service.video;

import cn.com.nla.common.video.basic.vo.video.DeviceAlarmVo;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;

@Slf4j
public abstract class DeviceAlarmVoService {

    public abstract void insert(DeviceAlarmVo deviceAlarmVo);
    /**
     * 发送报警消息
     * @param deviceAlarmVo 报警消息
     */
    public void sendAlarmMessage(SipServer sipServer, SIPCommander sipCommander, SIPCommanderForPlatform sipCommanderForPlatform, VideoProperties videoProperties, DeviceAlarmVo deviceAlarmVo){
        if(StringUtils.isNotEmpty(deviceAlarmVo.getChannelId())){
            DeviceChannelVo deviceChannelVo = VideoService.getDeviceChannelService().findChannelId(deviceAlarmVo.getChannelId());
            if(sipServer.getSipConfigProperties().getId().equals(deviceAlarmVo.getDeviceId()) && deviceChannelVo!= null){
                try {
                    sipCommander.sendAlarmMessage(sipServer,VideoService.getDeviceService().findDeviceGbId(deviceChannelVo.getDeviceId()), deviceAlarmVo,null,null);
                } catch (InvalidArgumentException | SipException | ParseException e) {
                    log.error("[命令发送失败] 发送报警: {}", e.getMessage());
                }
            }else if(deviceChannelVo!= null){
                List<ParentPlatformVo> platformGbChannel = VideoService.getParentPlatformService().findPlatformGbChannel(deviceAlarmVo.getChannelId());
                if(platformGbChannel == null || platformGbChannel.isEmpty()){
                    return;
                }
                for (ParentPlatformVo parentPlatformVo : platformGbChannel) {
                    try {
                        sipCommanderForPlatform.sendAlarmMessage(sipServer,parentPlatformVo, deviceAlarmVo,null,null);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[命令发送失败] 发送报警: {}", e.getMessage());
                    }
                }
            }
        }else {
            if(videoProperties.getSendToPlatformsWhenIdLost()){
                List<ParentPlatformVo> parentPlatformVos = VideoService.getParentPlatformService().queryEnableParentPlatformList();
                for (ParentPlatformVo parentPlatformVo : parentPlatformVos) {
                    try {
                        deviceAlarmVo.setChannelId(parentPlatformVo.getDeviceGbId());
                        sipCommanderForPlatform.sendAlarmMessage(sipServer,parentPlatformVo, deviceAlarmVo,null,null);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 国标级联 发送报警: {}", e.getMessage());
                    }
                }
            }else {
                List<ParentPlatformVo> parentPlatformVos = VideoService.getParentPlatformService().queryEnablePlatformListWithAsMessageChannel();
                for (ParentPlatformVo parentPlatformVo : parentPlatformVos) {
                    try {
                        deviceAlarmVo.setChannelId(parentPlatformVo.getDeviceGbId());
                        sipCommanderForPlatform.sendAlarmMessage(sipServer,parentPlatformVo, deviceAlarmVo,null,null);
                    } catch (SipException | InvalidArgumentException | ParseException e) {
                        log.error("[命令发送失败] 国标级联 发送报警: {}", e.getMessage());
                    }
                }

            }
            List<DeviceVo> deviceVoList =   VideoService.getDeviceService().queryDeviceWithAsMessageChannel();
            for (DeviceVo device : deviceVoList) {
                try {
                    deviceAlarmVo.setChannelId(device.getDeviceId());
                    sipCommander.sendAlarmMessage(sipServer,device, deviceAlarmVo,null,null);
                } catch (InvalidArgumentException | SipException | ParseException e) {
                    log.error("[命令发送失败] 发送报警: {}", e.getMessage());
                }
            }
        }
    }

}
