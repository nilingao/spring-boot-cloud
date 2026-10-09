package cn.com.nla.common.freeswitch.client.sip.cmd.build;

import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.utils.SipUtils;
import cn.com.nla.common.freeswitch.common.sip.SipConstant;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.message.MessageFactoryImpl;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SIPRequest 构造器
 */
@Slf4j
public class SIPRequestProvider {


    public static Builder builder(SipServer sipServer, String charset, String requestType, Object content) {
        try {
            return new Builder(sipServer,charset,requestType,content);
        } catch (PeerUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InvalidArgumentException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder{

        /**
         * 消息字符集 UTF-8 丶 gb2312
         */
        private final String charset;
        /**
         * Request.class 中属性
         */
        private final String requestType;
        private final SipServer sipServer;
        private final SipFactory sipFactory;
        private SipURI sipURI;
        private CallIdHeader callIdHeader;
        private CSeqHeader cSeqHeader;
        private FromHeader fromHeader;
        private ToHeader toHeader;
        private List<ViaHeader> viaHeaderList = new ArrayList<>();
        private List<Header> headerList = new ArrayList<>();
        private MaxForwardsHeader maxForwardsHeader;
        private ContentTypeHeader contentTypeHeader;
        private final Object content;

        /**
         * 构建Request构造器
         * @param charset
         * @param requestType
         */
        public Builder(SipServer sipServer,String charset,String requestType,Object content) throws PeerUnavailableException, InvalidArgumentException {
            this.sipServer = sipServer;
            this.sipFactory = sipServer.getSipFactory();
            this.charset = charset;
            this.requestType =requestType;
            this.content =content;
            this.maxForwardsHeader = this.sipFactory.createHeaderFactory().createMaxForwardsHeader(SipConstant.maxForwardsHeader);
        }


        /**
         * 基于给定的用户和主机组件创建SipURL
         * @param deviceGbId 设备国标编号
         * @param hostAddress 设备地址 192.168.1.77:5060
         * @return
         */
        public Builder createSipURI(String deviceGbId,String hostAddress) throws PeerUnavailableException, ParseException {
            return createSipURI(deviceGbId,hostAddress,null);
        }
        public Builder createSipURI(String deviceGbId,String hostAddress,String transport) throws PeerUnavailableException, ParseException {
            this.sipURI = this.sipFactory.createAddressFactory().createSipURI(deviceGbId, hostAddress);
            if(StringUtils.isNotEmpty(transport)){
                this.sipURI.setTransportParam(transport);
            }
            return this;
        }

        /**
         * 构建消息唯一标识
         * @param ip 本机 SIP ip 可为null
         * @param transport TCP链接 或 UDP链接 可为null
         * @param callId 上级平台时需传
         * @return
         */
        public  Builder createCallIdHeader(String ip, String transport,String callId) {
            if(StringUtils.isNotEmpty(callId)){
                try {
                    this.callIdHeader = sipFactory.createHeaderFactory().createCallIdHeader(callId);
                    this.callIdHeader.setCallId(callId);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } catch (PeerUnavailableException e) {
                    throw new RuntimeException(e);
                }
            }else {
                if (ObjectUtils.isEmpty(transport)) {
                    this.callIdHeader = this.sipServer.getUdpSipProvider().getNewCallId();
                    return this;
                }
                SipProviderImpl sipProvider;
                if (ObjectUtils.isEmpty(ip)) {
                    sipProvider = transport.equalsIgnoreCase("TCP") ? this.sipServer.getTcpSipProvider() : this.sipServer.getUdpSipProvider();
                }else {
                    sipProvider = transport.equalsIgnoreCase("TCP") ? this.sipServer.getTcpSipProvider(ip) : this.sipServer.getUdpSipProvider(ip);
                }
                if (sipProvider == null) {
                    sipProvider = this.sipServer.getUdpSipProvider();
                }
                if (sipProvider != null) {
                    this.callIdHeader = sipProvider.getNewCallId();
                }
            }
            return this;
        }
        public  Builder createCallIdHeader(CallIdHeader callIdHeader){
            this.callIdHeader = callIdHeader;
            return this;
        }
        /**
         * 根据新提供的序列号和方法值创建新的CSeqHeader。
         * @param cseq 序列号
         * @return
         */
        public  Builder createCSeqHeader(long cseq) throws PeerUnavailableException, InvalidArgumentException, ParseException {
            this.cSeqHeader = this.sipFactory.createHeaderFactory().createCSeqHeader(cseq, this.requestType);
            return this;
        }
        /**
         * 基于新提供的地址和标记值创建新的FromHeader。
         * @param sipId sip国标编号
         * @param sipDomain 地区标识 国标前10位
         * @param fromTag from标签 可随机数
         */
        public  Builder createFromHeader(String sipId,String sipDomain,String fromTag) throws PeerUnavailableException, ParseException {
            SipURI fromSipURI = this.sipFactory.createAddressFactory().createSipURI(sipId, sipDomain);
            Address fromAddress =  this.sipFactory.createAddressFactory().createAddress(fromSipURI);
            this.fromHeader = this.sipFactory.createHeaderFactory().createFromHeader(fromAddress, fromTag);
            return this;
        }

        public  Builder createFromHeader(FromHeader fromHeader) {
            this.fromHeader = fromHeader;
            return this;
        }

        /**
         * 根据新提供的地址和标记值创建新的ToHeader
         * @param deviceGbId 设备国标编号
         * @param hostAddress 设备ip地址端口
         * @param toTag to标签 可随机数 可为null
         * @return
         */
        public  Builder createToHeader(String deviceGbId,String hostAddress,String toTag) throws PeerUnavailableException, ParseException {
            SipURI toSipURI = this.sipFactory.createAddressFactory().createSipURI(deviceGbId, hostAddress);
            Address toAddress =  this.sipFactory.createAddressFactory().createAddress(toSipURI);
            toHeader = this.sipFactory.createHeaderFactory().createToHeader(toAddress, toTag);
            return this;
        }
        public  Builder createToHeader(ToHeader toHeader) {
            this.toHeader = toHeader;
            return this;
        }

        /**
         *
         * @param ip 设备ip
         * @param port 设备地址
         * @param transport TCP或UDP
         * @return
         */
        public  Builder addViaHeader(String ip, int port, String transport,boolean rPort) throws PeerUnavailableException, InvalidArgumentException, ParseException {
            ViaHeader viaHeader = this.sipFactory.createHeaderFactory().createViaHeader(ip, port, transport, SipUtils.getNewViaTag());
            if(rPort){
                viaHeader.setRPort();
            }
            this.viaHeaderList.add(viaHeader);
            return this;
        }

        /**
         * 基于新提供的maxForwards值创建新的MaxForwardsHeader。
         * @param maxForwardsHeader 最大转发header
         * @return
         */
        public  Builder createMaxForwardsHeader(Integer maxForwardsHeader) throws PeerUnavailableException, InvalidArgumentException {
            if(maxForwardsHeader != null){
                this.maxForwardsHeader = this.sipFactory.createHeaderFactory().createMaxForwardsHeader(maxForwardsHeader);
            }
            return this;
        }

        /**
         *
         * 基于新提供的contentType和contentSubType创建新的ContentTypeHeader
         * @param contentType 内容类型
         * @param contentSubType 内容子类型
         * @return
         */
        public  Builder createContentTypeHeader(String contentType, String contentSubType) throws PeerUnavailableException, ParseException {
            contentTypeHeader = this.sipFactory.createHeaderFactory().createContentTypeHeader(contentType, contentSubType);
            return this;
        }

        public Builder createUserAgentHeader() throws PeerUnavailableException, ParseException {
            List<String> agentParam = new ArrayList<>();
            agentParam.add("Fs-Sip");
            headerList.add(sipFactory.createHeaderFactory().createUserAgentHeader(agentParam));
            return this;
        }

        public Builder createSubjectHeader(String channelId,String ssrc,String sipGbId) throws PeerUnavailableException, ParseException {
            String agentParam = String.format("%s:%s,%s:%s", channelId, ssrc, sipGbId, 0);
            headerList.add(sipFactory.createHeaderFactory().createSubjectHeader(agentParam));
            return this;
        }
        public  Builder createAcceptHeader(String contentType, String contentSubType) throws PeerUnavailableException, ParseException {
            headerList.add(this.sipFactory.createHeaderFactory().createAcceptHeader(contentType, contentSubType));
            return this;
        }
        public Builder createExpiresHeader(Integer expires) throws PeerUnavailableException, InvalidArgumentException {
            headerList.add(sipFactory.createHeaderFactory().createExpiresHeader(expires));
            return this;
        }

        public Builder createAuthorizationHeader(String request,String deviceGbId,String serverGbId,String host,String password,WWWAuthenticateHeader www){
            AuthorizationHeader authorization = createAuthorization(request,sipFactory, deviceGbId, serverGbId, host, password, www);
            if(authorization != null){
                headerList.add(authorization);
            }
            return this;
        }

        public Builder createProxyAuthenticateHeader(String request,String deviceGbId,String serverGbId,String host,String password,WWWAuthenticateHeader www){
            AuthorizationHeader authorization = createProxyAuthenticate(request,sipFactory, deviceGbId, serverGbId, host, password, www);
            if(authorization != null){
                headerList.add(authorization);
            }
            return this;
        }

        public Builder createEventHeader(String eventId,String eventType) throws PeerUnavailableException, ParseException {
            EventHeader eventHeader = sipFactory.createHeaderFactory().createEventHeader(eventType);
            if(!StringUtils.isEmpty(eventId)){
                eventHeader.setEventId(eventId);
            }
            headerList.add(eventHeader);
            return this;
        }

        public Builder createSubscriptionStateHeader(String subscriptionState) throws PeerUnavailableException, ParseException {
            headerList.add(sipFactory.createHeaderFactory().createSubscriptionStateHeader(subscriptionState));
            return this;
        }

        public Builder createContactHeader(String sipGbId,String sipAddress) throws PeerUnavailableException, ParseException {
            if(StringUtils.isEmpty(sipGbId) || StringUtils.isEmpty(sipAddress)){
                throw new RuntimeException("sipGbId is null or sipAddress is null");
            }
            SipURI sipContactUrl = sipFactory.createAddressFactory().createSipURI(sipGbId, sipAddress);
            Address address = sipFactory.createAddressFactory().createAddress(sipContactUrl);
            headerList.add(sipFactory.createHeaderFactory().createContactHeader(address));
            return this;
        }

        public CallIdHeader getCallIdHeader(){
            return this.callIdHeader;
        }


        /**
         * 构造消息发送体
         * @return
         */
        public  SIPRequest buildRequest() {
            if(this.sipFactory == null){
                throw new RuntimeException("SipFactory is null");
            }
            if(this.sipURI == null){
                throw new RuntimeException("SipURI is null");
            }
            if(StringUtils.isEmpty(this.requestType)){
                throw new RuntimeException("SipURI is null");
            }
            if(this.callIdHeader == null){
                throw new RuntimeException("callIdHeader is null");
            }
            if(this.cSeqHeader == null){
                throw new RuntimeException("cSeqHeader is null");
            }
            if(this.fromHeader == null){
                throw new RuntimeException("cSeqHeader is null");
            }
            if(this.toHeader == null){
                throw new RuntimeException("toHeader is null");
            }
            if(this.viaHeaderList == null){
                throw new RuntimeException("viaHeaderList is null");
            }
            if(this.maxForwardsHeader == null){
                throw new RuntimeException("maxForwardsHeader is null");
            }
            MessageFactoryImpl messageFactory = null;
            try {
                messageFactory = (MessageFactoryImpl) this.sipFactory.createMessageFactory();
            } catch (PeerUnavailableException e) {
                throw new RuntimeException(e);
            }
            if(!StringUtils.isEmpty(this.charset)){
                messageFactory.setDefaultContentEncodingCharset(this.charset);
            }
            SIPRequest request = null;
            try {
                request =(SIPRequest) messageFactory.createRequest(this.sipURI, this.requestType, this.callIdHeader, this.cSeqHeader, this.fromHeader, this.toHeader, this.viaHeaderList, this.maxForwardsHeader);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            if(!this.headerList.isEmpty()){
                for (Header header : headerList) {
                    request.addHeader(header);
                }
            }
            if(ObjectUtils.isNotEmpty(this.content) && this.contentTypeHeader != null){
                try {
                    request.setContent(this.content,this.contentTypeHeader);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            return request;
        }
    }


    public static AuthorizationHeader  createAuthorization(String request,SipFactory sipFactory,String deviceGbId,String serverGbId,String host,String password,WWWAuthenticateHeader www){
        return createAuthorization(false,request,sipFactory,deviceGbId,serverGbId,host,password,www);
    }

    public static AuthorizationHeader createProxyAuthenticate(String request, SipFactory sipFactory, String deviceGbId, String serverGbId, String host, String password, WWWAuthenticateHeader www){
        return createAuthorization(true,request,sipFactory,deviceGbId,serverGbId,host,password,www);
    }

    public static AuthorizationHeader  createAuthorization(boolean isProxy,String request,SipFactory sipFactory,String deviceGbId,String serverGbId,String host,String password,WWWAuthenticateHeader www){
        AuthorizationHeader authorizationHeader = null;
        try {
            SipURI requestURI = sipFactory.createAddressFactory().createSipURI(serverGbId,host);
            if(www == null){
                if(isProxy){
                    authorizationHeader = sipFactory.createHeaderFactory().createProxyAuthorizationHeader("Digest");
                }else {
                    authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader("Digest");
                }
                authorizationHeader.setUsername(deviceGbId);
                authorizationHeader.setURI(requestURI);
                authorizationHeader.setAlgorithm("MD5");
                return authorizationHeader;
            }
            String realm = www.getRealm();
            String nonce = www.getNonce();
            String scheme = www.getScheme();
            // 参考 https://blog.csdn.net/y673533511/article/details/88388138
            // qop 保护质量 包含auth（默认的）和auth-int（增加了报文完整性检测）两种策略
            String qop = www.getQop();

            String cNonce = null;
            String nc = "00000001";
            if (qop != null) {
                if ("auth".equalsIgnoreCase(qop)) {
                    // 客户端随机数，这是一个不透明的字符串值，由客户端提供，并且客户端和服务器都会使用，以避免用明文文本。
                    // 这使得双方都可以查验对方的身份，并对消息的完整性提供一些保护
                    cNonce = UUID.randomUUID().toString();

                }else if ("auth-int".equalsIgnoreCase(qop)){
                    // TODO
                }
            }

            String HA1 = DigestUtils.md5DigestAsHex((deviceGbId + ":" + realm + ":" + password).getBytes());
            String HA2=DigestUtils.md5DigestAsHex((request + ":" + requestURI.toString()).getBytes());

            StringBuffer reStr = new StringBuffer(200);
            reStr.append(HA1);
            reStr.append(":");
            reStr.append(nonce);
            reStr.append(":");
            if (qop != null) {
                reStr.append(nc);
                reStr.append(":");
                reStr.append(cNonce);
                reStr.append(":");
                reStr.append(qop);
                reStr.append(":");
            }
            reStr.append(HA2);

            String RESPONSE = DigestUtils.md5DigestAsHex(reStr.toString().getBytes());
            if(isProxy){
                authorizationHeader = sipFactory.createHeaderFactory().createProxyAuthorizationHeader("Digest");
            }else {
                authorizationHeader = sipFactory.createHeaderFactory().createAuthorizationHeader("Digest");
            }
            authorizationHeader.setUsername(deviceGbId);
            authorizationHeader.setRealm(realm);
            authorizationHeader.setNonce(nonce);
            authorizationHeader.setURI(requestURI);
            authorizationHeader.setResponse(RESPONSE);
            authorizationHeader.setAlgorithm("MD5");
            if (qop != null) {
                authorizationHeader.setQop(qop);
                authorizationHeader.setCNonce(cNonce);
                authorizationHeader.setNonceCount(1);
            }
        }catch (PeerUnavailableException | ParseException e){
            log.error(" authorization 解密错误：",e);
            return authorizationHeader;
        }
        return authorizationHeader;
    }
}
