package cn.com.nla.common.video.core.config.runner;

import cn.com.nla.common.video.basic.vo.media.MediaRestResult;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.SendRtpManager;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.service.video.MediaServerVoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 服务器启动设备注册
 */
@Slf4j
@Order(40)
public class SipDeviceRunner implements CommandLineRunner {

    @Autowired
    private DynamicTask dynamicTask;
    @Autowired
    private SipServer sipServer;
    @Autowired
    private SIPCommanderForPlatform sipCommanderForPlatform;

    @Override
    public void run(String... args) throws Exception {
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        SendRtpManager sendRtpManager = RedisService.getSendRtpManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        // 设备离线定时任务
        dynamicTask.startCron("device-offline-5m", 300,()->{
            DeviceVoService deviceVoService = VideoService.getDeviceService();
            List<DeviceVo> allDeviceVo = deviceVoService.getAllOnlineDevice();
            for (DeviceVo deviceVo : allDeviceVo) {
                if(deviceVo.keepalive()){
                    deviceVoService.offline(deviceVo.getDeviceId(),"设备离线定时任务");
                }
            }
        });
        //查找有国标推流全部关闭
        List<SendRtp> sendRtpList = sendRtpManager.queryAllSendRTPServer();
        for (SendRtp sendRtp : sendRtpList) {
            MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(sendRtp.getMediaServerId());
            sendRtpManager.deleteSendRTPServer(sendRtp.getPlatformId(),sendRtp.getChannelId(),sendRtp.getStreamId(),sendRtp.getCallId());
            if(mediaServerVo != null){
                ssrcConfigManager.releaseSsrc(mediaServerVo.getId(),sendRtp.getSsrc());
                MediaRestResult result = MediaClient.stopSendRtp(mediaServerVo, "__defaultVhost__", sendRtp.getApp(), sendRtp.getStreamId(), sendRtp.getSsrc());
                if(result.getCode() == 0){
                    ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(sendRtp.getPlatform());
                    if(parentPlatformVo != null){
                        sipCommanderForPlatform.streamByeCmd(sipServer, parentPlatformVo,sendRtp,null,null);
                    }
                }
            }
        }
    }
}
