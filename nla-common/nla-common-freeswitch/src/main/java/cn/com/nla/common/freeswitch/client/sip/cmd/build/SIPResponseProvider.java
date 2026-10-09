package cn.com.nla.common.freeswitch.client.sip.cmd.build;

import cn.com.nla.common.freeswitch.client.sip.SipServer;
import cn.com.nla.common.freeswitch.client.sip.utils.SipUtils;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * SIPRequest 构造器
 */
@Slf4j
public class SIPResponseProvider {


    public static Builder builder(SipServer sipServer, int statusCode, String msg, Object content, SIPRequest request) throws InvalidArgumentException, PeerUnavailableException {
        return new Builder(sipServer,statusCode,msg,content,request);
    }

    public static class Builder{
        /**
         * Request.class 中属性
         */
        private final SipServer sipServer;
        private final SipFactory sipFactory;
        private final SIPRequest request;
        private SipURI sipURI;
        private List<Header> headerList = new ArrayList<>();
        private ContentTypeHeader contentTypeHeader;
        private final int statusCode;
        private final String msg;
        private final Object content;



        /**
         * 构建Request构造器
         */
        public Builder(SipServer sipServer,int statusCode, String msg,Object content,SIPRequest request) throws PeerUnavailableException, InvalidArgumentException {
            this.sipServer = sipServer;
            this.sipFactory = sipServer.getSipFactory();
            this.content =content;
            this.request =request;
            this.statusCode =statusCode;
            this.msg =msg;
        }


        /**
         * 基于给定的用户和主机组件创建SipURL
         * @param serverGBId 服务国标编号
         * @param hostAddress 设备地址 192.168.1.77:5060
         * @return
         */
        public Builder createSipURI(String serverGBId,String hostAddress) throws PeerUnavailableException, ParseException {
            // 兼容国标中的使用编码@域名作为RequestURI的情况
            if(this.request == null){
                this.sipURI = this.sipFactory.createAddressFactory().createSipURI(serverGBId, hostAddress);
            }else {
                SipURI requestURI = (SipURI) this.request.getRequestURI();
                if(requestURI != null && requestURI.getPort() > 0){
                    this.sipURI = ((SipURI)this.request.getRequestURI());
                }else {
                    this.sipURI = this.sipFactory.createAddressFactory().createSipURI(serverGBId, hostAddress);
                }
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


        public Builder createExpiresHeader(Integer expires) throws PeerUnavailableException, InvalidArgumentException {
            headerList.add(sipFactory.createHeaderFactory().createExpiresHeader(expires));
            return this;
        }


        public Builder createContactHeader() throws PeerUnavailableException, ParseException {
            if(this.sipURI == null){
                throw new RuntimeException("SipURI is null");
            }
            SipURI sipContactUrl = sipFactory.createAddressFactory().createSipURI(this.sipURI.getUser(), String.format("%s:%s",this.sipURI.getHost(),this.sipURI.getPort()));
            Address address = sipFactory.createAddressFactory().createAddress(sipContactUrl);
            headerList.add(sipFactory.createHeaderFactory().createContactHeader(address));
            return this;
        }

        /**
         * 构造消息发送体
         * @return
         */
        public SIPResponse buildResponse() throws PeerUnavailableException, ParseException {
            if(this.sipFactory == null){
                throw new RuntimeException("SipFactory is null");
            }
            if(this.request == null){
                throw new RuntimeException("SIPRequest is null");
            }
            if(this.request.getToHeader().getTag() == null){
                this.request.getToHeader().setTag(SipUtils.getNewTag());
            }
            SIPResponse response =(SIPResponse) this.sipFactory.createMessageFactory().createResponse(this.statusCode, this.request);
            response.setStatusCode(this.statusCode);
            if(!StringUtils.isEmpty(this.msg)){
                response.setReasonPhrase(this.msg);
            }
            if(!this.headerList.isEmpty()){
                for (Header header : headerList) {
                    response.addHeader(header);
                }
            }
            if(ObjectUtils.isNotEmpty(this.content) && this.contentTypeHeader != null){
                try {
                    response.setContent(this.content,this.contentTypeHeader);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            return response;
        }
    }


}
