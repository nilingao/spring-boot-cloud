package cn.com.nla.common.freeswitch.client.sip.listener.response;


import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommander;
import cn.com.nla.common.freeswitch.client.sip.cmd.SIPCommanderForPlatform;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;

@Slf4j
public abstract class AbstractSipResponseEvent implements SipResponseEvent{

    @Resource
    protected SipServer sipServer;
    @Resource
    protected SIPCommander sipCommander;
    @Resource
    protected SIPCommanderForPlatform sipCommanderForPlatform;

}
