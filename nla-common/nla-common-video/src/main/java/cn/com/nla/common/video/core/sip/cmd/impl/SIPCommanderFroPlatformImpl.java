package cn.com.nla.common.video.core.sip.cmd.impl;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.basic.enums.CharsetType;
import cn.com.nla.common.video.basic.enums.GbIdConstant;
import cn.com.nla.common.video.basic.enums.TransportType;
import cn.com.nla.common.video.basic.vo.sip.PlatformRegisterInfo;
import cn.com.nla.common.video.basic.vo.sip.RecordInfo;
import cn.com.nla.common.video.basic.vo.sip.RecordItem;
import cn.com.nla.common.video.basic.vo.sip.SendRtp;
import cn.com.nla.common.video.basic.vo.video.*;
import cn.com.nla.common.video.core.demo.NotifySubscribeInfo;
import cn.com.nla.common.video.core.demo.SipTransactionInfo;
import cn.com.nla.common.video.core.properties.SipConfigProperties;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.PlatformRegisterManager;
import cn.com.nla.common.video.core.redis.impl.SipTransactionManager;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeHandle;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommanderForPlatform;
import cn.com.nla.common.video.core.sip.cmd.SipSendMessage;
import cn.com.nla.common.video.core.sip.cmd.build.SIPRequestProvider;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.header.CallIdHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SIPCommanderFroPlatformImpl implements SIPCommanderForPlatform {
    @Autowired
    private DynamicTask dynamicTask;


    @Override
    public void unregister(SipServer sipServer, ParentPlatformVo parentPlatformVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        register(sipServer, parentPlatformVo,  null, false,okEvent,errorEvent);
    }

    @Override
    public void register(SipServer sipServer, ParentPlatformVo parentPlatformVo, @Nullable WWWAuthenticateHeader www, boolean isRegister, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        SIPRequest request;
        SipTransactionManager sipTransactionManager = RedisService.getSipTransactionManager();
        SipTransactionInfo sipTransactionInfo = sipTransactionManager.findParentPlatform(parentPlatformVo.getServerGbId());
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        String callId = null;
        String fromTag = SipUtils.getNewFromTag();
        String toTag = null;
        if (sipTransactionInfo != null ) {
            if (StringUtils.isNotEmpty(sipTransactionInfo.getCallId())) {
                callId = sipTransactionInfo.getCallId();
            }
            if (StringUtils.isNotEmpty(sipTransactionInfo.getFromTag())) {
                fromTag = sipTransactionInfo.getFromTag();
            }
            if (StringUtils.isNotEmpty(sipTransactionInfo.getToTag())) {
                toTag = sipTransactionInfo.getToTag();
            }
        }
        PlatformRegisterManager platformRegisterManager = RedisService.getPlatformRegisterManager();
        if (www == null) {
            request =(SIPRequest) SIPRequestProvider.builder(sipServer, null, Request.REGISTER, null)
                    .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                    .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                    .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), callId)
                    .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), fromTag)
                    .createToHeader(parentPlatformVo.getServerGbId(), parentPlatformVo.getServerGbDomain(), toTag)
                    .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                    .createUserAgentHeader()
                    .createContactHeader(parentPlatformVo.getDeviceGbId(), String.format("%s:%s", parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort()))
                    .createExpiresHeader(isRegister ? parentPlatformVo.getExpires() : 0)
                    .buildRequest();
            CallIdHeader callIdHeader = request.getCallIdHeader();
            //存储注册缓存
            platformRegisterManager.updatePlatformRegisterInfo(callIdHeader.getCallId(), PlatformRegisterInfo.builder().platformId(parentPlatformVo.getServerGbId()).register(isRegister).build());

        }else {
            request = (SIPRequest) SIPRequestProvider.builder(sipServer, null, Request.REGISTER, null)
                    .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                    .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                    .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), callId)
                    .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), fromTag)
                    .createToHeader(parentPlatformVo.getServerGbId(), parentPlatformVo.getServerGbDomain(), toTag)
                    .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                    .createUserAgentHeader()
                    .createContactHeader(parentPlatformVo.getDeviceGbId(), String.format("%s:%s", parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort()))
                    .createExpiresHeader(isRegister ? parentPlatformVo.getExpires() : 0)
                    .createAuthorizationHeader(Request.REGISTER,StringUtils.isEmpty(parentPlatformVo.getUsername())? parentPlatformVo.getDeviceGbId(): parentPlatformVo.getUsername(), parentPlatformVo.getServerGbId(),String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), parentPlatformVo.getPassword(),www)
                    .buildRequest();
        }
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent, error->{
            log.info("向上级平台 [ {} ] {}发生错误： {} ", parentPlatformVo.getServerGbId(),isRegister?"注册":"注销",error.getMsg());
            if(isRegister){
                CallIdHeader callIdHeader = request.getCallIdHeader();
                platformRegisterManager.delPlatformRegisterInfo(callIdHeader.getCallId());
            }
            if(errorEvent != null){
                errorEvent.response(error);
            }
        });
    }

    @Override
    public String keepalive(SipServer sipServer, ParentPlatformVo parentPlatformVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
            String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
            StringBuffer keepaliveXml = new StringBuffer(200);
            keepaliveXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
            keepaliveXml.append("<Notify>\r\n");
            keepaliveXml.append("<CmdType>Keepalive</CmdType>\r\n");
            keepaliveXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
            keepaliveXml.append("<DeviceID>" + parentPlatformVo.getDeviceGbId() + "</DeviceID>\r\n");
            keepaliveXml.append("<Status>OK</Status>\r\n");
            keepaliveXml.append("</Notify>\r\n");
        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, keepaliveXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);
        CallIdHeader header = (CallIdHeader)request.getHeader(CallIdHeader.NAME);
        return header.getCallId();
    }

    /**
     * 向上级回复通道信息
     * @param channel 通道信息
     * @param parentPlatformVo 平台信息
     * @return
     */
    @Override
    public void catalogQuery(SipServer sipServer, DeviceChannelVo channel, ParentPlatformVo parentPlatformVo, String sn, String fromTag, int size, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        if ( parentPlatformVo ==null) {
            return ;
        }
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        List<DeviceChannelVo> channels = new ArrayList<>();
        if (channel != null) {
            channels.add(channel);
        }
        String catalogXml = getCatalogXml(channels, sn, parentPlatformVo, size);

        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, catalogXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);
    }

    @Override
    public void catalogQuery(SipServer sipServer, List<DeviceChannelVo> channels, ParentPlatformVo parentPlatformVo, String sn, String fromTag, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        if ( parentPlatformVo ==null) {
            return ;
        }
        sendCatalogResponse(sipServer,channels, parentPlatformVo, sn, fromTag, 0,true,okEvent,errorEvent);
    }
    private String getCatalogXml(List<DeviceChannelVo> channels, String sn, ParentPlatformVo parentPlatformVo, int size) {
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet +"\"?>\r\n");
        catalogXml.append("<Response>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" +sn + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatformVo.getDeviceGbId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>" + size + "</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\"" + channels.size() +"\">\r\n");
        if (channels.size() > 0) {
            for (DeviceChannelVo channel : channels) {
                if (parentPlatformVo.getServerGbId().equals(channel.getParentId())) {
                    channel.setParentId(parentPlatformVo.getDeviceGbId());
                }
                catalogXml.append("<Item>\r\n");
                // 行政区划分组只需要这两项就可以
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
                if (channel.getChannelId().length() <= 8) {
                    catalogXml.append("</Item>\r\n");
                    continue;
                }
                if (channel.getChannelId().length() != 20) {
                    catalogXml.append("</Item>\r\n");
                    log.warn("[编号长度异常] {} 长度错误，请使用20位长度的国标编号,当前长度：{}", channel.getChannelId(), channel.getChannelId().length());
                    catalogXml.append("</Item>\r\n");
                    continue;
                }
                if (channel.getSecrecy() != null) {
                    catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                }else {
                    catalogXml.append("<Parental>1</Parental>\r\n");
                }
                //类型编码  GbIdConstant.Type
                GbIdConstant.Type type = GbIdConstant.Type.getType(Integer.parseInt(channel.getChannelId().substring(10, 13)));
                switch (type){
                    case TYPE_200:
                        if (channel.getCivilCode() != null) {
                            catalogXml.append("<CivilCode>"+channel.getCivilCode()+"</CivilCode>\r\n");
                        }else {
                            catalogXml.append("<CivilCode></CivilCode>\r\n");
                        }

                        catalogXml.append("<RegisterWay>1</RegisterWay>\r\n");
                        catalogXml.append("<Secrecy>0</Secrecy>\r\n");
                        break;
                    case TYPE_215:
                        if (StringUtils.isNotEmpty(channel.getParentId())) {
                            catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                        }
                        break;
                    case TYPE_216:
                        if (StringUtils.isNotEmpty(channel.getParentId())) {
                            catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                        }else {
                            catalogXml.append("<ParentID></ParentID>\r\n");
                        }
                        if (StringUtils.isNotEmpty(channel.getBusinessGroupId())) {
                            catalogXml.append("<BusinessGroupID>" + channel.getBusinessGroupId() + "</BusinessGroupID>\r\n");
                        }else {
                            catalogXml.append("<BusinessGroupID></BusinessGroupID>\r\n");
                        }
                        break;
                    default:
                        // 通道项
                        if (channel.getManufacture() != null) {
                            catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
                        }else {
                            catalogXml.append("<Manufacturer></Manufacturer>\r\n");
                        }
                        if (channel.getSecrecy() != null) {
                            catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                        }else {
                            catalogXml.append("<Secrecy></Secrecy>\r\n");
                        }
                        catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                        if (channel.getModel() != null) {
                            catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
                        }else {
                            catalogXml.append("<Model></Model>\r\n");
                        }
                        if (channel.getOwner() != null) {
                            catalogXml.append("<Owner>" + channel.getOwner()+ "</Owner>\r\n");
                        }else {
                            catalogXml.append("<Owner></Owner>\r\n");
                        }
                        if (channel.getCivilCode() != null) {
                            catalogXml.append("<CivilCode>" + channel.getCivilCode() + "</CivilCode>\r\n");
                        }else {
                            catalogXml.append("<CivilCode></CivilCode>\r\n");
                        }
                        if (channel.getAddress() == null) {
                            catalogXml.append("<Address></Address>\r\n");
                        }else {
                            catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                        }
                        if (StringUtils.isNotEmpty(channel.getParentId())) {
                            catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                        }else {
                            catalogXml.append("<ParentID></ParentID>\r\n");
                        }
                        if (StringUtils.isNotEmpty(channel.getBlock())) {
                            catalogXml.append("<Block>" + channel.getBlock() + "</Block>\r\n");
                        }else {
                            catalogXml.append("<Block></Block>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getSafetyWay())) {
                            catalogXml.append("<SafetyWay>" + channel.getSafetyWay() + "</SafetyWay>\r\n");
                        }else {
                            catalogXml.append("<SafetyWay>0</SafetyWay>\r\n");
                        }
                        if (StringUtils.isNotEmpty(channel.getCertNum())) {
                            catalogXml.append("<CertNum>" + channel.getCertNum() + "</CertNum>\r\n");
                        }else {
                            catalogXml.append("<CertNum></CertNum>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getCertifiable())) {
                            catalogXml.append("<Certifiable>" + channel.getCertifiable() + "</Certifiable>\r\n");
                        }else {
                            catalogXml.append("<Certifiable>0</Certifiable>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getErrCode())) {
                            catalogXml.append("<ErrCode>" + channel.getErrCode() + "</ErrCode>\r\n");
                        }else {
                            catalogXml.append("<ErrCode>0</ErrCode>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getEndTime())) {
                            catalogXml.append("<EndTime>" + channel.getEndTime() + "</EndTime>\r\n");
                        }else {
                            catalogXml.append("<EndTime></EndTime>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getSecrecy())) {
                            catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                        }else {
                            catalogXml.append("<Secrecy></Secrecy>\r\n");
                        }
                        if (StringUtils.isNotEmpty(channel.getIpAddress())) {
                            catalogXml.append("<IPAddress>" + channel.getIpAddress() + "</IPAddress>\r\n");
                        }else {
                            catalogXml.append("<IPAddress></IPAddress>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getPort())) {
                            catalogXml.append("<Port>" + channel.getPort() + "</Port>\r\n");
                        }else {
                            catalogXml.append("<Port>0</Port>\r\n");
                        }

                        if (StringUtils.isNotEmpty(channel.getPassword())) {
                            catalogXml.append("<Password>" + channel.getPassword() + "</Password>\r\n");
                        }else {
                            catalogXml.append("<Password></Password>\r\n");
                        }
                        if (ObjectUtils.isNotEmpty(channel.getPtzType())) {
                            catalogXml.append("<PTZType>" + channel.getPtzType() + "</PTZType>\r\n");
                        }else {
                            catalogXml.append("<PTZType>0</PTZType>\r\n");
                        }
                        catalogXml.append("<Status>" + (channel.getStatus() == ConstEnum.Flag.NO.getValue() ? "OFF" : "ON") + "</Status>\r\n");

                        catalogXml.append("<Longitude>" + (channel.getLongitudeWgs84() != null && channel.getLongitudeWgs84() != 0? channel.getLongitudeWgs84():channel.getLongitude()) + "</Longitude>\r\n");
                        catalogXml.append("<Latitude>" + (channel.getLatitudeWgs84() != null && channel.getLatitudeWgs84() != 0? channel.getLatitudeWgs84():channel.getLatitude()) + "</Latitude>\r\n");
                }
                catalogXml.append("</Item>\r\n");
            }
        }

        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Response>\r\n");
        return catalogXml.toString();
    }

    private void sendCatalogResponse(SipServer sipServer, List<DeviceChannelVo> channels, ParentPlatformVo parentPlatformVo, String sn, String fromTag, int index, boolean sendAfterResponse, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        if (index >= channels.size()) {
            return;
        }
        List<DeviceChannelVo> deviceChannelVos;
        if (index + parentPlatformVo.getCatalogGroup() < channels.size()) {
            deviceChannelVos = channels.subList(index, index + parentPlatformVo.getCatalogGroup());
        }else {
            deviceChannelVos = channels.subList(index, channels.size());
        }
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        String catalogXml = getCatalogXml(deviceChannelVos, sn, parentPlatformVo, channels.size());

        SIPRequest request =  (SIPRequest)SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, catalogXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), fromTag)
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        log.info("[命令发送] 国标级联{} 目录查询回复: 共{}条，已发送{}条", parentPlatformVo.getServerGbId(), channels.size(), Math.min(index + parentPlatformVo.getCatalogGroup(), channels.size()));
//        log.debug("[命令发送] 国标级联 catalogXml:{}",catalogXml);
        String timeoutTaskKey = "catalog_task_" + parentPlatformVo.getServerGbId() + sn;
        String callId = request.getCallIdHeader().getCallId();
        if (sendAfterResponse) {
            // 默认按照收到200回复后发送下一条， 如果超时收不到回复，就以30毫秒的间隔直接发送。
            //获取订阅工厂
            SipSubscribeHandle sipSubscribeHandle = sipServer.getSubscribeManager();
            dynamicTask.startDelay(timeoutTaskKey, 20, ()->{
                sipSubscribeHandle.removeAllSubscribe(callId);
                int indexNext = index + parentPlatformVo.getCatalogGroup();
                try {
                    sendCatalogResponse(sipServer,channels, parentPlatformVo, sn, fromTag, indexNext, false,null,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
                }
            });
            SipSendMessage.sendMessage(sipServer, parentPlatformVo, request, ok->{
                dynamicTask.stop(timeoutTaskKey);
                int indexNext = index + parentPlatformVo.getCatalogGroup();
                try {
                    sendCatalogResponse(sipServer,channels, parentPlatformVo, sn, fromTag, indexNext, true,null,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
                }
            },error->{
                log.error("[目录推送失败] 国标级联 platform : {}, code: {}, msg: {}, 停止发送", parentPlatformVo.getServerGbId(), error.getStatusCode(), error.getMsg());
                dynamicTask.stop(timeoutTaskKey);
                errorEvent.response(error);
            });
        }else {
            dynamicTask.startDelay(timeoutTaskKey, 20, ()->{
                int indexNext = index + parentPlatformVo.getCatalogGroup();
                try {
                    sendCatalogResponse(sipServer,channels, parentPlatformVo, sn, fromTag, indexNext, false,null,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
                }
            });
            SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent, error->{
                log.error("[目录推送失败] 国标级联 platform : {}, code: {}, msg: {}, 停止发送", parentPlatformVo.getServerGbId(), error.getStatusCode(), error.getMsg());
                dynamicTask.stop(timeoutTaskKey);
                errorEvent.response(error);
            });
        }
    }

    /**
     * 向上级回复DeviceInfo查询信息
     * @param parentPlatformVo 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceInfoResponse(SipServer sipServer, ParentPlatformVo parentPlatformVo, DeviceVo deviceVo, String sn, String fromTag, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatformVo == null) {
            return;
        }
        String deviceId = deviceVo == null ? parentPlatformVo.getDeviceGbId() : deviceVo.getDeviceId();
        String deviceName = deviceVo == null ? parentPlatformVo.getName() : deviceVo.getName();
        String manufacturer = deviceVo == null ? "spring-boot-cloud" : deviceVo.getManufacturer();
        String model = deviceVo == null ? "platform" : deviceVo.getModel();
        String firmware = deviceVo == null ? "1.0.0" : deviceVo.getFirmware();
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer deviceInfoXml = new StringBuffer(600);
        deviceInfoXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceInfoXml.append("<Response>\r\n");
        deviceInfoXml.append("<CmdType>DeviceInfo</CmdType>\r\n");
        deviceInfoXml.append("<SN>" +sn + "</SN>\r\n");
        deviceInfoXml.append("<DeviceID>" + deviceId + "</DeviceID>\r\n");
        deviceInfoXml.append("<DeviceName>" + deviceName + "</DeviceName>\r\n");
        deviceInfoXml.append("<Manufacturer>"+manufacturer+"</Manufacturer>\r\n");
        deviceInfoXml.append("<Model>"+model+"</Model>\r\n");
        deviceInfoXml.append("<Firmware>"+firmware+"</Firmware>\r\n");
        deviceInfoXml.append("<Result>OK</Result>\r\n");
        deviceInfoXml.append("</Response>\r\n");
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, deviceInfoXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);
    }

    /**
     * 向上级回复DeviceStatus查询信息
     * @param parentPlatformVo 平台信息
     * @param sn
     * @param fromTag
     * @return
     */
    @Override
    public void deviceStatusResponse(SipServer sipServer, ParentPlatformVo parentPlatformVo, String channelId, String sn, String fromTag, int status, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatformVo == null) {
            return ;
        }
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        String statusStr = (status==1)?"ONLINE":"OFFLINE";
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Response>\r\n");
        deviceStatusXml.append("<CmdType>DeviceStatus</CmdType>\r\n");
        deviceStatusXml.append("<SN>" +sn + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + channelId + "</DeviceID>\r\n");
        deviceStatusXml.append("<Result>OK</Result>\r\n");
        deviceStatusXml.append("<Online>" + statusStr + "</Online>\r\n");
        deviceStatusXml.append("<Status>OK</Status>\r\n");
        deviceStatusXml.append("</Response>\r\n");

        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, deviceStatusXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);
    }

    @Override
    public void sendNotifyMobilePosition(SipServer sipServer, ParentPlatformVo parentPlatformVo, DeviceMobilePositionVo deviceMobilePositionVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, SipException {
        if (parentPlatformVo == null) {
            return;
        }
        NotifySubscribeInfo catalogSubscribe = RedisService.getPlatformNotifySubscribeManager().getMobilePositionSubscribe(parentPlatformVo.getServerGbId());
        if(catalogSubscribe == null){
            log.debug("[发送 移动位置订阅] {},未获取上级订阅通知", parentPlatformVo.getServerGbId());
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("[发送 移动位置订阅] {}/{}->{},{}", parentPlatformVo.getServerGbId(), deviceMobilePositionVo.getChannelId(), deviceMobilePositionVo.getLatitude(), deviceMobilePositionVo.getLongitude());
        }
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Notify>\r\n");
        deviceStatusXml.append("<CmdType>MobilePosition</CmdType>\r\n");
        deviceStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + deviceMobilePositionVo.getChannelId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<Time>" + DateUtil.format(deviceMobilePositionVo.getUpdateTime(), DatePattern.UTC_SIMPLE_PATTERN) + "</Time>\r\n");
        deviceStatusXml.append("<Longitude>" + deviceMobilePositionVo.getLongitude() + "</Longitude>\r\n");
        deviceStatusXml.append("<Latitude>" + deviceMobilePositionVo.getLatitude() + "</Latitude>\r\n");
        deviceStatusXml.append("<Speed>" + deviceMobilePositionVo.getSpeed() + "</Speed>\r\n");
        deviceStatusXml.append("<Direction>" + deviceMobilePositionVo.getDirection() + "</Direction>\r\n");
        deviceStatusXml.append("<Altitude>" + deviceMobilePositionVo.getAltitude() + "</Altitude>\r\n");
        deviceStatusXml.append("</Notify>\r\n");

       sendNotify(sipServer, parentPlatformVo, deviceStatusXml.toString(),catalogSubscribe,okEvent,errorEvent);

    }

    @Override
    public void sendAlarmMessage(SipServer sipServer, ParentPlatformVo parentPlatformVo, DeviceAlarmVo deviceAlarmVo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        if (parentPlatformVo == null) {
            return;
        }
        NotifySubscribeInfo catalogSubscribe = RedisService.getPlatformNotifySubscribeManager().getAlarmSubscribe(parentPlatformVo.getServerGbId());
        if(catalogSubscribe == null){
            log.debug("[发送 报警订阅] {},未获取上级报警订阅通知", parentPlatformVo.getServerGbId());
            return;
        }
        log.info("[发送报警通知] {}/{}->{},{}: {}", parentPlatformVo.getServerGbId(), deviceAlarmVo.getChannelId(),
                deviceAlarmVo.getLongitude(), deviceAlarmVo.getLatitude(), JSONUtil.toJsonStr(deviceAlarmVo));
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer deviceStatusXml = new StringBuffer(600);
        deviceStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        deviceStatusXml.append("<Notify>\r\n");
        deviceStatusXml.append("<CmdType>Alarm</CmdType>\r\n");
        deviceStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        deviceStatusXml.append("<DeviceID>" + deviceAlarmVo.getChannelId() + "</DeviceID>\r\n");
        deviceStatusXml.append("<AlarmPriority>" + deviceAlarmVo.getAlarmPriority() + "</AlarmPriority>\r\n");
        deviceStatusXml.append("<AlarmMethod>" + deviceAlarmVo.getAlarmMethod() + "</AlarmMethod>\r\n");
        deviceStatusXml.append("<AlarmTime>" + deviceAlarmVo.getAlarmTime() + "</AlarmTime>\r\n");
        deviceStatusXml.append("<AlarmDescription>" + deviceAlarmVo.getAlarmDescription() + "</AlarmDescription>\r\n");
        deviceStatusXml.append("<Longitude>" + deviceAlarmVo.getLongitude() + "</Longitude>\r\n");
        deviceStatusXml.append("<Latitude>" + deviceAlarmVo.getLatitude() + "</Latitude>\r\n");
        deviceStatusXml.append("<info>\r\n");
        deviceStatusXml.append("<AlarmType>" + deviceAlarmVo.getAlarmType() + "</AlarmType>\r\n");
        deviceStatusXml.append("</info>\r\n");
        deviceStatusXml.append("</Notify>\r\n");
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, deviceStatusXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);

    }

    @Override
    public void sendNotifyForCatalogAddOrUpdate(SipServer sipServer, String type, ParentPlatformVo parentPlatformVo, List<DeviceChannelVo> deviceChannelVos, NotifySubscribeInfo catalogSubscribe, Integer index, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatformVo == null || deviceChannelVos == null || deviceChannelVos.size() == 0) {
            return;
        }
        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannelVos.size()) {
            return;
        }
        List<DeviceChannelVo> channels;
        if (index + parentPlatformVo.getCatalogGroup() < deviceChannelVos.size()) {
            channels = deviceChannelVos.subList(index, index + parentPlatformVo.getCatalogGroup());
        }else {
            channels = deviceChannelVos.subList(index, deviceChannelVos.size());
        }
        String catalogXmlContent = getCatalogXmlContentForCatalogAddOrUpdate(parentPlatformVo, channels, deviceChannelVos.size(), type);
        sendNotify(sipServer, parentPlatformVo, catalogXmlContent,catalogSubscribe,okEvent,errorEvent);
    }

    private void sendNotify(SipServer sipServer, ParentPlatformVo parentPlatformVo, String catalogXmlContent, NotifySubscribeInfo catalogSubscribe, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, ParseException, InvalidArgumentException {
        Request request = SIPRequestProvider.builder(sipServer, CharsetType.GB2312.getName(), Request.NOTIFY, catalogXmlContent)
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(null, null, catalogSubscribe==null?null:catalogSubscribe.getRequest().getCallIdHeader().getCallId())
                .createFromHeader(parentPlatformVo.getDeviceGbId(), String.format("%s:%s", parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort()), catalogSubscribe==null?null:catalogSubscribe.getResponse().getToTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), catalogSubscribe== null?null:catalogSubscribe.getRequest().getFromTag())
                .createEventHeader(catalogSubscribe==null?null:catalogSubscribe.getEventId(),catalogSubscribe==null?null:catalogSubscribe.getEventType())
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createSubscriptionStateHeader("active")
                .createContactHeader(parentPlatformVo.getDeviceGbId(),String.format("%s:%s", parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort()))
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);
    }

    private  String getCatalogXmlContentForCatalogAddOrUpdate(ParentPlatformVo parentPlatformVo, List<DeviceChannelVo> channels, int sumNum, String type) {
        StringBuffer catalogXml = new StringBuffer(600);

        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        catalogXml.append("<Notify>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatformVo.getDeviceGbId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>1</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\"" + channels.size() + "\">\r\n");
        if (channels.size() > 0) {
            for (DeviceChannelVo channel : channels) {
                if (parentPlatformVo.getServerGbId().equals(channel.getParentId())) {
                    channel.setParentId(parentPlatformVo.getDeviceGbId());
                }
                catalogXml.append("<Item>\r\n");
                // 行政区划分组只需要这两项就可以
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Name>" + channel.getName() + "</Name>\r\n");
                if (channel.getParentId() != null) {
                    // 业务分组加上这一项即可，提高兼容性，
                    catalogXml.append("<ParentID>" + channel.getParentId() + "</ParentID>\r\n");
                }
                if (channel.getChannelId().length() == 20 && Integer.parseInt(channel.getChannelId().substring(10, 13)) == 216) {
                    // 虚拟组织增加BusinessGroupID字段
                    catalogXml.append("<BusinessGroupID>" + channel.getParentId() + "</BusinessGroupID>\r\n");
                }
                catalogXml.append("<Parental>" + channel.getParental() + "</Parental>\r\n");
                if (channel.getParental() == 0) {
                    // 通道项
                    catalogXml.append("<Manufacturer>" + channel.getManufacture() + "</Manufacturer>\r\n");
                    catalogXml.append("<Secrecy>" + channel.getSecrecy() + "</Secrecy>\r\n");
                    catalogXml.append("<RegisterWay>" + channel.getRegisterWay() + "</RegisterWay>\r\n");
                    catalogXml.append("<Status>" + (channel.getStatus() == 0 ? "OFF" : "ON") + "</Status>\r\n");

                    if (channel.getChannelType() != 2) {  // 业务分组/虚拟组织/行政区划 不设置以下属性
                        catalogXml.append("<Model>" + channel.getModel() + "</Model>\r\n");
                        catalogXml.append("<Owner> " + channel.getOwner()+ "</Owner>\r\n");
                        catalogXml.append("<CivilCode>" + channel.getCivilCode() + "</CivilCode>\r\n");
                        catalogXml.append("<Address>" + channel.getAddress() + "</Address>\r\n");
                    }
//                    if (!"presence".equals(subscribeInfo.getEventType())) {
//                        catalogXml.append("<Event>" + type + "</Event>\r\n");
//                    }

                }
                catalogXml.append("</Item>\r\n");
            }
        }
        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Notify>\r\n");
        return catalogXml.toString();
    }

    @Override
    public void sendNotifyForCatalogOther(SipServer sipServer, String type, ParentPlatformVo parentPlatformVo, List<DeviceChannelVo> deviceChannelVos, NotifySubscribeInfo catalogSubscribe, Integer index, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws InvalidArgumentException, ParseException, NoSuchFieldException, SipException, IllegalAccessException {
        if (parentPlatformVo == null
                || deviceChannelVos == null
                || deviceChannelVos.size() == 0) {
            log.warn("[缺少必要参数]");
            return;
        }

        if (index == null) {
            index = 0;
        }
        if (index >= deviceChannelVos.size()) {
            return;
        }
        List<DeviceChannelVo> channels;
        if (index + parentPlatformVo.getCatalogGroup() < deviceChannelVos.size()) {
            channels = deviceChannelVos.subList(index, index + parentPlatformVo.getCatalogGroup());
        }else {
            channels = deviceChannelVos.subList(index, deviceChannelVos.size());
        }
        String catalogXmlContent = getCatalogXmlContentForCatalogOther(parentPlatformVo, channels, type);
        sendNotify(sipServer, parentPlatformVo, catalogXmlContent,catalogSubscribe,okEvent,errorEvent);
    }

    private String getCatalogXmlContentForCatalogOther(ParentPlatformVo parentPlatformVo, List<DeviceChannelVo> channels, String type) {

        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer catalogXml = new StringBuffer(600);
        catalogXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        catalogXml.append("<Notify>\r\n");
        catalogXml.append("<CmdType>Catalog</CmdType>\r\n");
        catalogXml.append("<SN>" + (int) ((Math.random() * 9 + 1) * 100000) + "</SN>\r\n");
        catalogXml.append("<DeviceID>" + parentPlatformVo.getDeviceGbId() + "</DeviceID>\r\n");
        catalogXml.append("<SumNum>1</SumNum>\r\n");
        catalogXml.append("<DeviceList Num=\" " + channels.size() + " \">\r\n");
        if (channels.size() > 0) {
            for (DeviceChannelVo channel : channels) {
                if (parentPlatformVo.getServerGbId().equals(channel.getParentId())) {
                    channel.setParentId(parentPlatformVo.getDeviceGbId());
                }
                catalogXml.append("<Item>\r\n");
                catalogXml.append("<DeviceID>" + channel.getChannelId() + "</DeviceID>\r\n");
                catalogXml.append("<Event>" + type + "</Event>\r\n");
                catalogXml.append("</Item>\r\n");
            }
        }
        catalogXml.append("</DeviceList>\r\n");
        catalogXml.append("</Notify>\r\n");
        return catalogXml.toString();
    }
    @Override
    public void recordInfo(SipServer sipServer, DeviceChannelVo deviceChannelVo, ParentPlatformVo parentPlatformVo, String fromTag, RecordInfo recordInfo, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)  throws SipException, InvalidArgumentException, ParseException {
        if ( parentPlatformVo ==null) {
            return ;
        }
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer recordXml = new StringBuffer(600);
        recordXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        recordXml.append("<Response>\r\n");
        recordXml.append("<CmdType>RecordInfo</CmdType>\r\n");
        recordXml.append("<SN>" +recordInfo.getSn() + "</SN>\r\n");
        recordXml.append("<DeviceID>" + recordInfo.getChannelId() + "</DeviceID>\r\n");
        recordXml.append("<SumNum>" + recordInfo.getSumNum() + "</SumNum>\r\n");
        if (recordInfo.getRecordList() == null ) {
            recordXml.append("<RecordList Num=\"0\">\r\n");
        }else {
            recordXml.append("<RecordList Num=\"" + recordInfo.getRecordList().size()+"\">\r\n");
            if (recordInfo.getRecordList().size() > 0) {
                for (RecordItem recordItem : recordInfo.getRecordList()) {
                    recordXml.append("<Item>\r\n");
                    if (deviceChannelVo != null) {
                        recordXml.append("<DeviceID>" + recordItem.getDeviceId() + "</DeviceID>\r\n");
                        recordXml.append("<Name>" + recordItem.getName() + "</Name>\r\n");
                        recordXml.append("<StartTime>" + DateUtil.format(DateUtil.parse(recordItem.getStartTime()),DatePattern.UTC_SIMPLE_FORMAT) + "</StartTime>\r\n");
                        recordXml.append("<EndTime>" + DateUtil.format(DateUtil.parse(recordItem.getEndTime()),DatePattern.UTC_SIMPLE_FORMAT) + "</EndTime>\r\n");
                        recordXml.append("<Secrecy>" + recordItem.getSecrecy() + "</Secrecy>\r\n");
                        recordXml.append("<Type>" + recordItem.getType() + "</Type>\r\n");
                        if (StringUtils.isNotEmpty(recordItem.getFileSize())) {
                            recordXml.append("<FileSize>" + recordItem.getFileSize() + "</FileSize>\r\n");
                        }
                        if (StringUtils.isNotEmpty(recordItem.getFilePath())) {
                            recordXml.append("<FilePath>" + recordItem.getFilePath() + "</FilePath>\r\n");
                        }
                    }
                    recordXml.append("</Item>\r\n");
                }
            }
        }

        recordXml.append("</RecordList>\r\n");
        recordXml.append("</Response>\r\n");

        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, recordXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), null)
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);

    }

    @Override
    public void sendMediaStatusNotify(SipServer sipServer, ParentPlatformVo parentPlatformVo, SendRtp sendRtpItem, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent) throws SipException, InvalidArgumentException, ParseException {
        if (sendRtpItem == null || parentPlatformVo == null) {
            return;
        }
        SipConfigProperties deviceSipConfig = sipServer.getSipConfigProperties();
        String characterSet = CharsetType.getName(parentPlatformVo.getCharacterSet());
        StringBuffer mediaStatusXml = new StringBuffer(200);
        mediaStatusXml.append("<?xml version=\"1.0\" encoding=\"" + characterSet + "\"?>\r\n");
        mediaStatusXml.append("<Notify>\r\n");
        mediaStatusXml.append("<CmdType>MediaStatus</CmdType>\r\n");
        mediaStatusXml.append("<SN>" + (int)((Math.random()*9+1)*100000) + "</SN>\r\n");
        mediaStatusXml.append("<DeviceID>" + sendRtpItem.getChannelId() + "</DeviceID>\r\n");
        mediaStatusXml.append("<NotifyType>121</NotifyType>\r\n");
        mediaStatusXml.append("</Notify>\r\n");

        Request request = SIPRequestProvider.builder(sipServer, CharsetType.getName(parentPlatformVo.getCharacterSet()), Request.MESSAGE, mediaStatusXml.toString())
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(parentPlatformVo.getDeviceIp(), TransportType.getName(parentPlatformVo.getTransport()), sendRtpItem.getCallId())
                .createFromHeader(parentPlatformVo.getDeviceGbId(), deviceSipConfig.getDomain(), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContentTypeHeader("Application", "MANSCDP+xml")
                .buildRequest();
        SipSendMessage.sendMessage(sipServer, parentPlatformVo, request,okEvent,errorEvent);
    }

    @Override
    public void streamByeCmd(SipServer sipServer, ParentPlatformVo parentPlatformVo, SendRtp sendRtpItem, SipSubscribeEvent okEvent, SipSubscribeEvent errorEvent)  throws SipException, InvalidArgumentException, ParseException {
        if (sendRtpItem == null ) {
            log.info("[向上级发送BYE]， sendRtpItem 为NULL");
            return;
        }
        if (parentPlatformVo == null) {
            log.info("[向上级发送BYE]， platform 为NULL");
            return;
        }
        log.info("[向上级发送BYE]， {}/{}", parentPlatformVo.getServerGbId(), sendRtpItem.getChannelId());
        Request request = SIPRequestProvider.builder(sipServer, CharsetType.GB2312.getName(), Request.BYE, null)
                .createSipURI(parentPlatformVo.getServerGbId(), String.format("%s:%s", parentPlatformVo.getServerIp(), parentPlatformVo.getServerPort()))
                .addViaHeader(parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort(), TransportType.getName(parentPlatformVo.getTransport()), true)
                .createCallIdHeader(null, null, sendRtpItem.getCallId())
                .createFromHeader(parentPlatformVo.getDeviceGbId(), String.format("%s:%s", parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort()), SipUtils.getNewFromTag())
                .createToHeader(parentPlatformVo.getServerGbId(), parentPlatformVo.getServerGbDomain(), null)
                .createCSeqHeader(RedisService.getCseqManager().getCSEQ())
                .createUserAgentHeader()
                .createContactHeader(parentPlatformVo.getDeviceGbId(),String.format("%s:%s", parentPlatformVo.getDeviceIp(), parentPlatformVo.getDevicePort()))
                .buildRequest();
        if (request == null) {
            log.warn("[向上级发送bye]：无法创建 byeRequest");
            return;
        }
        SipSendMessage.sendMessage(sipServer, parentPlatformVo,request,okEvent,errorEvent);
    }
}
