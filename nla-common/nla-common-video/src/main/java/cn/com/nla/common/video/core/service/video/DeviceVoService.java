package cn.com.nla.common.video.core.service.video;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.basic.vo.sip.SyncStatus;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.MediaServerVo;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import cn.com.nla.common.video.core.demo.SsrcTransaction;
import cn.com.nla.common.video.core.media.client.MediaClient;
import cn.com.nla.common.video.core.properties.VideoProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.InviteStreamManager;
import cn.com.nla.common.video.core.redis.impl.SipTransactionManager;
import cn.com.nla.common.video.core.redis.impl.SsrcConfigManager;
import cn.com.nla.common.video.core.redis.impl.SsrcTransactionManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class DeviceVoService {
    @Autowired
    private SipServer localSipServer;

    public abstract List<DeviceVo> getAllOnlineDevice();
    public abstract DeviceVo findDeviceGbId(String deviceGbId);
    public abstract DeviceVo findPlatformIdChannelId(String platformId, String channelId);
    public abstract DeviceVo findDeviceInfoPlatformIdChannelId(String platformId, String channelId);

    public abstract List<DeviceVo> queryDeviceWithAsMessageChannel();

    public abstract int save(DeviceVo deviceVo);
    public abstract void updateStatus(Long id,boolean status);
    public abstract void updateZlm(String deviceGbId,String mediaServerId);

    /**
     * 设备上线
     * @param deviceVo
     */
    public void online(DeviceVo deviceVo, SipServer sipServer, SIPCommander sipCommander, VideoProperties videoProperties, SipTransactionInfo sipTransactionInfo,String reason) {
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();
        InviteStreamManager inviteStreamManager = RedisService.getInviteStreamManager();
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        log.info("[设备上线] {},deviceId：{}->{}:{}", reason,deviceVo.getDeviceId(), deviceVo.getIp(), deviceVo.getPort());
        if ( null  == deviceVo.getHeartBeatInterval() || 0 == deviceVo.getHeartBeatInterval()) {
            // 默认心跳间隔60
            deviceVo.setHeartBeatInterval(60);
        }
        if ( null  == deviceVo.getHeartBeatCount() || 0 == deviceVo.getHeartBeatCount()) {
            // 默认心跳间隔60
            deviceVo.setHeartBeatCount(2);
        }
        if ( null  == deviceVo.getPositionCapability()) {
            deviceVo.setPositionCapability(0);
        }
        if (sipTransactionInfo != null) {
            sipTransactionManager.putDevice(deviceVo.getDeviceId(),sipTransactionInfo);
        }
        deviceVo.setKeepaliveTime(java.time.LocalDateTime.now());
        DeviceVo deviceVoGb = this.findDeviceGbId(deviceVo.getDeviceId());
        //缓存设备注册服务
        RedisService.getRegisterServerManager().putDevice(deviceVo.getDeviceId(),deviceVo.getHeartBeatInterval()+ VideoConstant.DELAY_TIME * 2 , Address.builder().gbId(deviceVo.getDeviceId()).ip(localSipServer.getAdvertisedIp(null)).port(localSipServer.getSipConfigProperties().getPort()).build());
        if(deviceVoGb == null){
            deviceVo.setOnline(ConstEnum.Flag.YES.getValue());
            deviceVo.setRegisterTime(java.time.LocalDateTime.now());
            log.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", deviceVo.getDeviceId());
            this.save(deviceVo);
            try {
                sipCommander.deviceInfoQuery(sipServer, deviceVo,null,null);
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
            }
            sync(sipServer,sipCommander, deviceVo);
        }else {
            inviteStreamManager.clearInviteInfo(deviceVo.getDeviceId());
            if(deviceVo.getOnline() == ConstEnum.Flag.NO.getValue()){
                log.info("[设备上线,首次注册]: {}，查询设备信息以及通道信息", deviceVo.getDeviceId());
                deviceVo.setOnline(ConstEnum.Flag.YES.getValue());
                deviceVo.setRegisterTime(java.time.LocalDateTime.now());
                this.save(deviceVo);
                if(videoProperties.getSyncChannelOnDeviceOnline()){
                    log.info("[设备上线,离线状态下重新注册]: {}，查询设备信息以及通道信息", deviceVo.getDeviceId());
                    try {
                        sipCommander.deviceInfoQuery(sipServer, deviceVo,null,null);
                    } catch (InvalidArgumentException | SipException | ParseException e) {
                        log.error("[命令发送失败] 查询设备信息: {}", e.getMessage());
                    }
                    sync(sipServer,sipCommander, deviceVo);
                }
            }else {
                if (deviceChannelVoService.queryAllChannels(deviceVo.getDeviceId()).size() == 0) {
                    log.info("[设备上线]: {}，通道数为0,查询通道信息", deviceVo.getDeviceId());
                    sync(sipServer,sipCommander, deviceVo);
                }
                this.save(deviceVo);
            }
        }
        // 上线添加订阅
        if (deviceVo.getSubscribeCycleForCatalog() > 0) {
            // 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
            RedisService.getDeviceNotifySubscribeManager().addCatalogSubscribe(deviceVo,"设备上线订阅");
        }
        if (deviceVo.getSubscribeCycleForMobilePosition() > 0) {
            RedisService.getDeviceNotifySubscribeManager().addMobilePositionSubscribe(deviceVo,"设备上线订阅");
        }
        if (deviceVo.getSubscribeCycleForAlarm() > 0) {
            RedisService.getDeviceNotifySubscribeManager().addAlarmSubscribe(deviceVo,"设备上线订阅");
        }
        String key = String.format("%s_%s", VideoConstant.REGISTER_EXPIRE_TASK_KEY_PREFIX, deviceVo.getDeviceId());
        //设备过期任务
        dynamicTask.startDelay(key, deviceVo.getHeartBeatInterval()*Math.max(deviceVo.getHeartBeatCount(),2),()->offline(deviceVo.getDeviceId(),"设备上线-设备过期任务"));
    }

    /**
     * 设备下线
     * @param deviceId
     */
    public void offline(String deviceId,String reason) {
        DynamicTask dynamicTask = SpringUtil.getBean(DynamicTask.class);
        MediaServerVoService mediaServerVoService = VideoService.getMediaServerService();
        SsrcTransactionManager ssrcTransactionManager = RedisService.getSsrcTransactionManager();
        SsrcConfigManager ssrcConfigManager = RedisService.getSsrcConfigManager();
        log.info("[设备离线]， {},device：{}",reason, deviceId);
        DeviceVo deviceVo = this.findDeviceGbId(deviceId);
        if (deviceVo == null) {
            log.warn("[设备离线]：未获取设备信息 deviceId ：{}",deviceId);
            return;
        }
        log.info("[设备离线] device：{}， 心跳间隔： {}，心跳超时次数： {}， 上次心跳时间：{}， 上次注册时间： {}", deviceId,deviceVo.getHeartBeatInterval(), deviceVo.getHeartBeatCount(), deviceVo.getKeepaliveTime(), deviceVo.getRegisterTime());
        String key = String.format("%s_%s", VideoConstant.REGISTER_EXPIRE_TASK_KEY_PREFIX, deviceVo.getDeviceId());
        dynamicTask.stop(key);
        this.updateStatus(deviceVo.getId(),false);
        VideoService.getDeviceChannelService().deviceChannelOnline(deviceVo.getDeviceId(),null,false);
        // 离线释放所有ssrc
        List<SsrcTransaction> ssrcTransactions = ssrcTransactionManager.getParamAll(deviceId, null, null, null,null);
        if (ssrcTransactions != null && ssrcTransactions.size() > 0) {
            for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
                MediaServerVo mediaServerVo = mediaServerVoService.findOnLineMediaServerId(ssrcTransaction.getMediaServerId());
                if(mediaServerVo != null){
                    ssrcConfigManager.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
                    MediaClient.closeRtpServer(mediaServerVo, ssrcTransaction.getStream());
                    ssrcTransactionManager.remove(deviceId, ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
                }
            }
        }
        // 移除订阅
        RedisService.getDeviceNotifySubscribeManager().removeCatalogSubscribe(deviceVo);
        RedisService.getDeviceNotifySubscribeManager().removeMobilePositionSubscribe(deviceVo);
        RedisService.getDeviceNotifySubscribeManager().removeAlarmSubscribe(deviceVo);
        RedisService.getRegisterServerManager().delDevice(deviceId);
    }

    /**
     * 异步同步通道
     * @param deviceVo
     */
    public boolean sync(SipServer sipServer, SIPCommander sipCommander, DeviceVo deviceVo){
        if (RedisService.getCatalogDataManager().isSyncRunning(deviceVo.getDeviceId())) {
            log.info("开启同步时发现同步已经存在");
            return true;
        }
        int sn = (int)((Math.random()*9+1)*100000);
        RedisService.getCatalogDataManager().addReady(deviceVo, sn);
        try {
            sipCommander.catalogQuery(sipServer, deviceVo, sn,null, error -> {
                String errorMsg = String.format("同步通道失败，错误码： %s, %s", error.getStatusCode(), error.getMsg());
                RedisService.getCatalogDataManager().setChannelSyncEnd(deviceVo.getDeviceId(), errorMsg);
            });
            return true;
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[同步通道], 信令发送失败：{}", e.getMessage() );
            String errorMsg = String.format("同步通道失败，信令发送失败： %s", e.getMessage());
            RedisService.getCatalogDataManager().setChannelSyncEnd(deviceVo.getDeviceId(), errorMsg);
            return false;
        }
    }

    /**
     * 获取设备同步状态
     * @param deviceId
     */
    public SyncStatus getChannelSyncStatus(String deviceId){
        return RedisService.getCatalogDataManager().getSyncStatus(deviceId);
    }

}
