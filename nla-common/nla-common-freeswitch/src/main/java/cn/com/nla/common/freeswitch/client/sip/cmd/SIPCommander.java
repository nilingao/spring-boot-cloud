package cn.com.nla.common.freeswitch.client.sip.cmd;

import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.exception.SsrcTransactionNotFoundException;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.subscribe.sip.message.SipSubscribeEvent;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sdp.SdpParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * @description:设备能力接口
 */
public interface SIPCommander {

    /**
     * 视频流停止
     */
    void streamByeCmd(SipServer sipServer, AgentVoInfo deviceVo, String stream, String callId, String typeName, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SsrcTransactionNotFoundException;

    void sendAckMessage(SipServer sipServer, SIPResponse response, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, SipException, ParseException, SdpParseException;
}
