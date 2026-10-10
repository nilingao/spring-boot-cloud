package cn.com.nla.common.video.core.service.video;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.DeviceMobilePositionVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.demo.NotifySubscribeInfo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

@Slf4j
public abstract class DeviceMobilePositionVoService {

    @Autowired
    protected SipServer sipServer;
    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;
    public abstract void save(DeviceMobilePositionVo deviceMobilePositionVo);

    public abstract DeviceMobilePositionVo findLastChannelId(String channelId);
    /**
     * 发送位置订阅通知
     * @param platformId
     * @param info
     */
   public void sendNotifyMobilePosition(String platformId, NotifySubscribeInfo info){
       ParentPlatformVo platformVo = VideoService.getParentPlatformService().getParentPlatformByServerGbId(platformId);
       if (platformVo == null || platformVo.getStatus() == ConstEnum.Flag.NO.getValue()) {
           return;
       }
       DeviceMobilePositionVo deviceMobilePositionVo = findLastChannelId(info.getId());
       if(deviceMobilePositionVo == null){
           log.info("[发送设备位置通知]： 未获取位置信息 channelId ：{}",info.getId());
           return;
       }
       try {
           sipCommanderForPlatform.sendNotifyMobilePosition(sipServer,platformVo,deviceMobilePositionVo,null,null);
       } catch (InvalidArgumentException |ParseException | NoSuchFieldException | SipException | IllegalAccessException e) {
           log.error("[发送设备位置通知]： 发送SIP消息失败 channelId ：{}:",info.getId(),e);
       }

   }


}
