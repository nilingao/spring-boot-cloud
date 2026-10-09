package cn.com.nla.common.freeswitch.redis;

import cn.com.nla.common.freeswitch.redis.impl.fs.*;
import cn.com.nla.common.freeswitch.redis.impl.sip.*;
import cn.com.nla.common.freeswitch.redis.subscribe.notify.AgentLoginSubscribe;
import cn.hutool.extra.spring.SpringUtil;

public class RedisService {

    public static CallInfoManager getCallInfoManager()              { return SpringUtil.getBean(CallInfoManager.class); }
    public static VdnPhoneManager getVdnPhoneManager()              { return SpringUtil.getBean(VdnPhoneManager.class); }
    public static CompanyInfoManager getCompanyInfoManager()        { return SpringUtil.getBean(CompanyInfoManager.class); }
    public static CompanyConferenceInfoManager getCompanyConferenceInfoManager() { return SpringUtil.getBean(CompanyConferenceInfoManager.class); }
    public static GroupInfoManager getGroupInfoManager()            { return SpringUtil.getBean(GroupInfoManager.class); }
    public static AgentInfoManager getAgentInfoManager()            { return SpringUtil.getBean(AgentInfoManager.class); }
    public static DeviceInfoManager getDeviceInfoManager()          { return SpringUtil.getBean(DeviceInfoManager.class); }
    public static PlaybackInfoManager getPlaybackInfoManager()      { return SpringUtil.getBean(PlaybackInfoManager.class); }

    // SIP 注册与通话状态管理
    public static RegisterServerManager getRegisterServerManager()  { return SpringUtil.getBean(RegisterServerManager.class); }
    public static SipTransactionManager getSipTransactionManager()  { return SpringUtil.getBean(SipTransactionManager.class); }
    public static PlatformRegisterManager getPlatformRegisterManager() { return SpringUtil.getBean(PlatformRegisterManager.class); }
    public static CseqManager getCseqManager()                      { return SpringUtil.getBean(CseqManager.class); }
    public static SsrcTransactionManager getSsrcTransactionManager(){ return SpringUtil.getBean(SsrcTransactionManager.class); }
    public static AgentNotifySubscribeManager getAgentNotifySubscribeManager() { return SpringUtil.getBean(AgentNotifySubscribeManager.class); }

    public static AgentLoginSubscribe getAgentLoginSubscribe()      { return SpringUtil.getBean(AgentLoginSubscribe.class); }
}
