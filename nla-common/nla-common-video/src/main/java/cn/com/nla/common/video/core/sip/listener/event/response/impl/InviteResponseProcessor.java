package cn.com.nla.common.video.core.sip.listener.event.response.impl;

import cn.com.nla.common.video.core.demo.Gb28181Sdp;
import cn.com.nla.common.video.core.sip.listener.event.response.AbstractSipResponseEvent;
import cn.com.nla.common.video.core.utils.SipUtils;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;

import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * @description: 处理INVITE响应
 */
@Slf4j
public class InviteResponseProcessor extends AbstractSipResponseEvent {

    @Override
    public String getMethod() {
        return Request.INVITE;
    }

    @Override
    public void process(ResponseEvent evt) {
        try {
            SIPResponse response = (SIPResponse)evt.getResponse();
            int statusCode = response.getStatusCode();
            // trying不会回复
            if (statusCode == Response.TRYING) {
            }
            // 成功响应
            // 下发ack
            if (statusCode == Response.OK) {
                ResponseEventExt event = (ResponseEventExt)evt;
                String contentString = new String(response.getRawContent());
                Gb28181Sdp gb28181Sdp = SipUtils.parseSDP(contentString);
                SessionDescription sdp = gb28181Sdp.getBaseSdb();
               sipCommander.sendAckMessage(sipServer,sdp,event,response,null,error->{
                   log.error("[点播回复ACK]，异常：{}",error.getMsg());
               });
            }
        } catch (InvalidArgumentException | ParseException | SipException | SdpParseException e) {
            log.error("[点播回复ACK]，异常：", e );
        } catch (Exception e){
            log.error("[点播回复ACK]，消息处理异常：", e );
        }
    }
}
