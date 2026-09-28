package cn.com.tzy.springbootstarterfreeswitch.client.sip.cmd;

import cn.com.tzy.springbootstarterfreeswitch.client.sip.SipServer;
import cn.com.tzy.springbootstarterfreeswitch.model.fs.AgentVoInfo;
import cn.com.tzy.springbootstarterfreeswitch.redis.subscribe.sip.message.SipSubscribeEvent;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.WWWAuthenticateHeader;
import java.text.ParseException;

public interface SIPCommanderForPlatform {

    /** 向 FreeSWITCH 注册（SIP REGISTER） */
    void register(SipServer sipServer, AgentVoInfo agentVoInfo, WWWAuthenticateHeader www,
                  boolean isRegister, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws SipException, InvalidArgumentException, ParseException;

    /** 向 FreeSWITCH 注销（SIP UNREGISTER） */
    void unregister(SipServer sipServer, AgentVoInfo agentVoInfo,
                    SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws InvalidArgumentException, ParseException, SipException;

    /** 向 FreeSWITCH 发送 SIP OPTIONS 心跳 */
    String keepalive(SipServer sipServer, AgentVoInfo agentVoInfo,
                     SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws SipException, InvalidArgumentException, ParseException;

    /** 向 FreeSWITCH 发送 BYE（挂机） */
    void streamByeCmd(SipServer sipServer, AgentVoInfo agentVoInfo,
                      String audioStream, String videoStream, String callId, String typeName,
                      SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws SipException, InvalidArgumentException, ParseException;

    /** presence 订阅 / 取消订阅 */
    SIPRequest presenceSubscribe(SipServer sipServer, AgentVoInfo deviceVo, SIPRequest request,
                                 SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException;

    /**
     * 纯 SIP 模式发起呼叫，不依赖 ZLM。
     * SDP 由调用方构建（可直接透传客户端 SDP）。
     */
    SIPRequest callPhoneSip(SipServer sipServer, AgentVoInfo agentVoInfo, String sdp, String caller,
                            SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException;

    /** 407 代理认证后重新发起 INVITE */
    SIPRequest callPhone(SipServer sipServer, AgentVoInfo agentVoInfo,
                         ProxyAuthenticateHeader header, SIPRequest sipRequest, SIPResponse response)
            throws InvalidArgumentException, SipException, ParseException;
}
