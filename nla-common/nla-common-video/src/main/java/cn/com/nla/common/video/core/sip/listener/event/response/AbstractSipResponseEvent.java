package cn.com.nla.common.video.core.sip.listener.event.response;

import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;

import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractSipResponseEvent implements SipResponseEvent{

    @Autowired
    protected SipServer sipServer;
    @Autowired
    protected SIPCommander sipCommander;
    @Autowired
    protected SIPCommanderForPlatform sipCommanderForPlatform;

}
