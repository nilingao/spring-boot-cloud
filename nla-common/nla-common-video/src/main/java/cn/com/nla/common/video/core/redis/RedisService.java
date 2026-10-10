package cn.com.nla.common.video.core.redis;

import cn.com.nla.common.video.core.redis.impl.*;
import cn.hutool.extra.spring.SpringUtil;

/**
 * redis缓存
 */
public class RedisService {

    public static SsrcConfigManager getSsrcConfigManager(){return SpringUtil.getBean(SsrcConfigManager.class);}
    public static SsrcTransactionManager getSsrcTransactionManager(){return SpringUtil.getBean(SsrcTransactionManager.class);}
    public static SendRtpManager getSendRtpManager(){return SpringUtil.getBean(SendRtpManager.class);}
    public static SipTransactionManager getSipTransactionManager(){return SpringUtil.getBean(SipTransactionManager.class);}
    public static PlatformRegisterManager getPlatformRegisterManager(){return SpringUtil.getBean(PlatformRegisterManager.class);}
    public static MediaServerManager getMediaServerManager(){return SpringUtil.getBean(MediaServerManager.class);}
    public static InviteStreamManager getInviteStreamManager(){return SpringUtil.getBean(InviteStreamManager.class);}
    public static CseqManager getCseqManager(){return SpringUtil.getBean(CseqManager.class);}
    public static RegisterServerManager getRegisterServerManager(){return SpringUtil.getBean(RegisterServerManager.class);}
    public static CatalogDataManager getCatalogDataManager(){return SpringUtil.getBean(CatalogDataManager.class);}
    public static DeviceNotifySubscribeManager getDeviceNotifySubscribeManager(){return SpringUtil.getBean(DeviceNotifySubscribeManager.class);}
    public static PlatformNotifySubscribeManager getPlatformNotifySubscribeManager(){return SpringUtil.getBean(PlatformNotifySubscribeManager.class);}
    public static StreamChangedManager getStreamChangedManager(){return SpringUtil.getBean(StreamChangedManager.class);}
    public static RecordMp4Manager getRecordMp4Manager(){return SpringUtil.getBean(RecordMp4Manager.class);}

}
