package cn.com.nla.common.video.core.sip.utils;

import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.utils.SipUtils;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.ResponseEventExt;
import gov.nist.javax.sip.header.To;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;

import javax.sip.TimeoutEvent;
import javax.sip.message.Message;
import javax.sip.message.Request;
import java.util.EventObject;

@Slf4j
public class SipLogUtils {

    public static void sendMessage(SipServer sipServer, Message message) {
        try {
            if (!sipServer.getVideoProperties().getSipLog()) {
                return;
            }
            String to = "";
            StringBuilder builder = new StringBuilder();
            if(message instanceof Request){
                SIPRequest sipMessage = (SIPRequest) message;
                To toSip = (To) sipMessage.getTo();
                to = toSip.getUserAtHostPort();
            }else {
                SIPResponse sipMessage = (SIPResponse) message;
                To toSip = (To) sipMessage.getTo();
                to = toSip.getUserAtHostPort();
            }
            builder.append("发送：目标--->").append(to).append("\r\n").append(message);
            log.info(builder.toString());
        }catch (Exception e){
            log.error("解析消息错误：",e);
        }
    }


    public static void receiveMessage(SipServer sipServer, EventObject message) {
        try {
            if (!sipServer.getVideoProperties().getSipLog()) {
                return;
            }
            String from ="";
            String to ="";
            Message msg= null;
            if(message instanceof RequestEventExt){
                RequestEventExt requestEventExt = (RequestEventExt) message;
                SIPRequest request = (SIPRequest) requestEventExt.getRequest();
                Address remoteAddress = SipUtils.getRemoteAddressFromRequest(request, sipServer.getVideoProperties().getSipUseSourceIpAsRemoteAddress());
                from = String.format("%s:%s",remoteAddress.getIp(),remoteAddress.getPort());
                to = String.format("%s:%s",request.getLocalAddress(),request.getLocalPort());
                msg = request;
            }else if(message instanceof ResponseEventExt){
                ResponseEventExt responseEventExt = (ResponseEventExt) message;
                SIPResponse response = (SIPResponse) responseEventExt.getResponse();
                from = String.format("%s:%s",responseEventExt.getRemoteIpAddress(),responseEventExt.getRemotePort());
                to = String.format("%s:%s",response.getLocalAddress(),response.getLocalPort());
                msg = response;
            }else if(message instanceof TimeoutEvent){
                TimeoutEvent timeoutEvent = (TimeoutEvent) message;
                SIPRequest request = (SIPRequest) timeoutEvent.getClientTransaction().getRequest();
                Address remoteAddress = SipUtils.getRemoteAddressFromRequest(request, sipServer.getVideoProperties().getSipUseSourceIpAsRemoteAddress());
                from = String.format("%s:%s",remoteAddress.getIp(),remoteAddress.getPort());
                to = String.format("%s:%s",request.getLocalAddress(),request.getLocalPort());
                msg = request;
            }
            StringBuilder builder = new StringBuilder();
            builder.append("接收：来自--->").append(from).append("\r\n").append(msg);
            log.info(builder.toString());
        }catch (Exception e){
            log.error("解析消息错误：",e);
        }
    }


}
