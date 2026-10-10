package cn.com.nla.common.video.core.service;

import cn.com.nla.common.video.core.service.video.*;
import cn.hutool.extra.spring.SpringUtil;

public class VideoService {

    public static DeviceVoService getDeviceService(){
        return SpringUtil.getBean(DeviceVoService.class);
    }

    public static DeviceChannelVoService getDeviceChannelService(){return SpringUtil.getBean(DeviceChannelVoService.class);}

    public static GbStreamVoService getGbStreamService(){return SpringUtil.getBean(GbStreamVoService.class);}

    public static StreamProxyVoService getStreamProxyService(){return SpringUtil.getBean(StreamProxyVoService.class);}

    public static StreamPushVoService getStreamPushService(){return SpringUtil.getBean(StreamPushVoService.class);}

    public static PlatformCatalogVoService getPlatformCatalogService(){return SpringUtil.getBean(PlatformCatalogVoService.class);}

    public static ParentPlatformVoService getParentPlatformService(){return SpringUtil.getBean(ParentPlatformVoService.class);}

    public static MediaServerVoService getMediaServerService(){return SpringUtil.getBean(MediaServerVoService.class);}

    public static DeviceMobilePositionVoService getDeviceMobilePositionService(){return SpringUtil.getBean(DeviceMobilePositionVoService.class);}

    public static DeviceAlarmVoService getDeviceAlarmService(){return SpringUtil.getBean(DeviceAlarmVoService.class);}
}
