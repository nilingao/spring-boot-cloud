package cn.com.nla.common.freeswitch.client.sip.listener.request.impl;

import cn.com.nla.common.freeswitch.client.sip.auth.DigestServerAuthenticationHelper;
import cn.com.nla.common.freeswitch.client.sip.listener.request.AbstractSipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.listener.request.SipRequestEvent;
import cn.com.nla.common.freeswitch.client.sip.properties.SipConfigProperties;
import cn.com.nla.common.freeswitch.client.sip.utils.SipUtils;
import cn.com.nla.common.freeswitch.enums.fs.AgentStateEnum;
import cn.com.nla.common.freeswitch.enums.fs.LoginTypeEnum;
import cn.com.nla.common.freeswitch.enums.sip.CharsetType;
import cn.com.nla.common.freeswitch.enums.sip.StreamModeType;
import cn.com.nla.common.freeswitch.enums.sip.TransportType;
import cn.com.nla.common.freeswitch.model.fs.AgentVoInfo;
import cn.com.nla.common.freeswitch.redis.RedisService;
import cn.com.nla.common.freeswitch.redis.impl.sip.SipTransactionManager;
import cn.com.nla.common.freeswitch.service.FsService;
import cn.com.nla.common.freeswitch.service.freeswitch.AgentVoService;
import cn.com.nla.common.freeswitch.utils.VideoSipDate;
import cn.com.nla.common.freeswitch.vo.sip.Address;
import cn.com.nla.common.freeswitch.vo.sip.SipTransactionInfo;
import gov.nist.javax.sip.RequestEventExt;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.SIPDateHeader;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Calendar;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * SIP命令类型： REGISTER 注册请求 一般未其他sip软电话请求我平台进行交互
 */
@Slf4j
@Component
public class RegisterRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {
    @Override
    public String getMethod() {
        return Request.REGISTER;
    }

    @Override
    public void process(RequestEvent event) {
        AgentVoService agentVoService = FsService.getAgentService();
        SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();
        try {
            RequestEventExt evtExt = (RequestEventExt) event;
            String requestAddress = String.format("%s:%s",evtExt.getRemoteIpAddress(),evtExt.getRemotePort());
            SIPRequest request = (SIPRequest)event.getRequest();
            // 注册标志
            boolean registerFlag = request.getExpires().getExpires() > 0;
            String title = registerFlag ? "[注册请求]": "[注销请求]";
            Address remoteAddress = SipUtils.getRemoteAddressFromRequest(request, videoProperties.getSipUseSourceIpAsRemoteAddress());
            SipConfigProperties sipConfigProperties = sipServer.getSipConfigProperties();
            String routeId = ((SipUri)request.getRequestLine().getUri()).getUser();
            Response response;
            String agentSip = ((SipUri)(((FromHeader) request.getHeader(FromHeader.NAME)).getAddress()).getURI()).getUser();
            AgentVoInfo agentVoInfo = RedisService.getAgentInfoManager().getSip(agentSip);
            if(agentVoInfo == null){
                agentVoInfo = agentVoService.getAgentBySip(agentSip);
            }
            if(agentVoInfo == null){
                // 注册失败
                response = sipServer.getSipFactory().createMessageFactory().createResponse(Response.FORBIDDEN, request);
                response.setReasonPhrase("未获取坐席信息");
                log.info("[{}] 未获取坐席信息 , 回复403: {}",title, requestAddress);
                sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(), response);
                return;
            }
            agentVoInfo.setLoginType(LoginTypeEnum.SOCKET.getType());
            if(agentVoInfo.getAgentState() == null){
                agentVoInfo.setAgentState(AgentStateEnum.LOGIN);
            }
            log.info("[{}] 设备：{}, 开始处理: {}",title, agentVoInfo.getAgentKey(), remoteAddress);
            if (registerFlag) {
                //是否已注册 注册过则续订
                SipTransactionInfo sipTransactionInfo = sipTransactionManager.findDevice(agentVoInfo.getAgentKey());
                if(sipTransactionInfo != null && request.getCallIdHeader().getCallId().equals(sipTransactionInfo.getCallId())){
                    log.info("[{}] 注册续订: {}",title, agentVoInfo.getAgentKey());
                    agentVoInfo.setExpires(request.getExpires().getExpires());
                    agentVoInfo.setRemoteAddress(String.format("%s:%s",remoteAddress.getIp(),remoteAddress.getPort()));
                    agentVoInfo.setRenewTime(LocalDateTime.now());
                    response = getRegisterOkResponse(request);
                    // 判断TCP还是UDP
                    ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                    String transport = reqViaHeader.getTransport();
                    agentVoInfo.setTransport("TCP".equalsIgnoreCase(transport) ? TransportType.TCP.getValue() :TransportType.UDP.getValue());
                    sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(),response);
                    //agentVoService.online(agentVoInfo,new SipTransactionInfo((SIPResponse)response));
                    return;
                }
            }
            //设备密码
            String password =  agentVoInfo.getPasswd();
            AuthorizationHeader authHead = (AuthorizationHeader) request.getHeader(AuthorizationHeader.NAME);
            if (authHead == null && !ObjectUtils.isEmpty(password)) {
                log.info("[{}] 回复401: {}",title, requestAddress);
                response = sipServer.getSipFactory().createMessageFactory().createResponse(Response.UNAUTHORIZED, request);
                new DigestServerAuthenticationHelper().generateChallenge(sipServer.getSipFactory().createHeaderFactory(), response, sipConfigProperties.getDomain());
                //回复未注册消息 401
                sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(),response);
                return;
            }
            // 校验密码是否正确
            boolean passwordCorrect = ObjectUtils.isEmpty(password) || new DigestServerAuthenticationHelper().doAuthenticatePlainTextPassword(request, password);
            if (!passwordCorrect) {
                // 注册失败
                response = sipServer.getSipFactory().createMessageFactory().createResponse(Response.FORBIDDEN, request);
                response.setReasonPhrase("wrong password");
                log.warn("[{}] 密码/SIP服务器ID错误, 回复403: {}",title, requestAddress);
                sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(), response);
                return;
            }
            if (request.getExpires() == null) {//无过期时间回复参数错误
                log.warn("[{}] 无过期时间回复参数错误, 回复400: {}",title, requestAddress);
                response = sipServer.getSipFactory().createMessageFactory().createResponse(Response.BAD_REQUEST, request);
                sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(), response);
                return;
            }
            response = getRegisterOkResponse(request);
            Address remoteAddressFromRequest = SipUtils.getRemoteAddressFromRequest(request, videoProperties.getSipUseSourceIpAsRemoteAddress());
            if (ObjectUtils.isEmpty(agentVoInfo.getStreamMode())) {
                agentVoInfo.setStreamMode(StreamModeType.TCP_PASSIVE.getValue());
            }
            if (ObjectUtils.isEmpty(agentVoInfo.getCharset())) {
                agentVoInfo.setCharset(CharsetType.GB2312.getValue());
            }
            agentVoInfo.setRemoteAddress(remoteAddressFromRequest.getIp().concat(":").concat(String.valueOf(remoteAddressFromRequest.getPort())));
            sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(), response);
            // 注册成功
            if (registerFlag) {
                log.info("[{}] deviceId: {}->{}",title,  agentVoInfo.getAgentKey(), requestAddress);
                // 注册成功
                agentVoInfo.setExpires(request.getExpires().getExpires());
                // 判断TCP还是UDP
                ViaHeader reqViaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);
                String transport = reqViaHeader.getTransport();
                agentVoInfo.setTransport("TCP".equalsIgnoreCase(transport) ? 2 : 1);
                agentVoInfo.setRegisterTime(LocalDateTime.now());
                agentVoInfo.setRenewTime(LocalDateTime.now());
                agentVoService.online(agentVoInfo,new SipTransactionInfo((SIPResponse)response));
            } else {
                log.info("[{}] deviceId: {}->{}",title ,agentVoInfo.getAgentKey(), requestAddress);
                agentVoService.offline(agentVoInfo.getAgentKey());
            }
        }catch (NoSuchAlgorithmException | SipException | ParseException e){
            log.error(e.getMessage());
        }catch (Exception e){
            log.error("[REGISTER请求]，消息处理异常：", e );
        }
    }

    private Response getRegisterOkResponse(Request request) throws ParseException, PeerUnavailableException {
        // 携带授权头并且密码正确
        Response  response = sipServer.getSipFactory().createMessageFactory().createResponse(Response.OK, request);
        // 添加date头
        SIPDateHeader dateHeader = new SIPDateHeader();
        dateHeader.setDate( new VideoSipDate(Calendar.getInstance(Locale.ENGLISH).getTimeInMillis()));
        response.addHeader(dateHeader);
        // 添加Contact头
        response.addHeader(request.getHeader(ContactHeader.NAME));
        // 添加Expires头
        response.addHeader(request.getExpires());
        return response;

    }
}
