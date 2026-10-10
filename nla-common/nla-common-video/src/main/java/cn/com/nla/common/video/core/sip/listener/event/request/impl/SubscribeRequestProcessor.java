package cn.com.nla.common.video.core.sip.listener.event.request.impl;

import cn.com.nla.common.video.basic.common.CmdTypeConstant;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.demo.NotifySubscribeInfo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.AbstractSipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.com.nla.common.video.core.utils.XmlUtils;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.ExpiresHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 订阅请求事件
 */
@Slf4j
public class SubscribeRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {
    @Override
    public String getMethod() {
        return Request.SUBSCRIBE;
    }

    @Override
    public void process(RequestEvent event) {
        SIPRequest request = (SIPRequest) event.getRequest();
        Element rootElement = getRootElement(event);
        try {
            if (rootElement == null) {
                log.error("处理SUBSCRIBE请求  未获取到消息体{}", event.getRequest());
                responseAck(request, Response.BAD_REQUEST,"接受deviceControl命令错误 未获取到当前类型");
                return;
            }
            String cmd = XmlUtils.getText(rootElement, "CmdType");
            if(CmdTypeConstant.MOBILE_POSITION.equals(cmd)){
                processNotifyMobilePosition(request,rootElement);
            }else if(CmdTypeConstant.CATALOG.equals(cmd)){
                processNotifyCatalogList(request,rootElement);
            }else if(CmdTypeConstant.ALARM.equals(cmd)){
                processNotifyAlarm(request,rootElement);
            }else {
                log.info("接收到消息：" + cmd);
                Response response = sipServer.getSipFactory().createMessageFactory().createResponse(200, request);
                if(response != null){
                    ExpiresHeader expiresHeader = sipServer.getSipFactory().createHeaderFactory().createExpiresHeader(30);
                    response.setExpires(expiresHeader);
                }
                log.info("response : " + response);
                sipMessageHandle.handleMessage(request.getLocalAddress().getHostAddress(),response);
            }
        }catch (SipException | ParseException | InvalidArgumentException e){
            log.error("订阅请求事件发生错误，error:",e);
        }catch (Exception e){
            log.error("[订阅请求事件发生错误]，消息处理异常：", e );
        }
    }

    /**
     * 处理移动位置订阅
     */
    private void processNotifyMobilePosition(SIPRequest request, Element rootElement) {
        if (request == null) {
            return;
        }
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        String platformId = SipUtils.getUserIdFromFromHeader(request);
        String deviceId = XmlUtils.getText(rootElement, "DeviceID");//实际是通道编号
        String sn = XmlUtils.getText(rootElement, "SN");
        ParentPlatformVo parentPlatformVo = parentPlatformVoService.getParentPlatformByServerGbId(platformId);
        if (parentPlatformVo == null) {
            return;
        }
        NotifySubscribeInfo notifySubscribeInfo = new NotifySubscribeInfo(request,deviceId);
        log.info("[回复上级的移动位置订阅请求]: {}", platformId);
        StringBuilder resultXml = new StringBuilder(200);
        resultXml.append("<?xml version=\"1.0\" ?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>MobilePosition</CmdType>\r\n")
                .append("<SN>").append(sn).append("</SN>\r\n")
                .append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
                .append("<Result>OK</Result>\r\n")
                .append("</Response>\r\n");

        if (notifySubscribeInfo.getExpires() > 0) {
            // GPS上报时间间隔
            String interval = XmlUtils.getText(rootElement, "Interval");
            if (interval == null) {
                notifySubscribeInfo.setGpsInterval(5);
            }else {
                notifySubscribeInfo.setGpsInterval(Integer.parseInt(interval));
            }
            notifySubscribeInfo.setSn(sn);
        }

        try {
            SIPResponse response = responseXmlAck(request, resultXml.toString(), parentPlatformVo, notifySubscribeInfo.getExpires());
            if (notifySubscribeInfo.getExpires() >= 0) {
                notifySubscribeInfo.setResponse(response);
                RedisService.getPlatformNotifySubscribeManager().putMobilePositionSubscribe(platformId, notifySubscribeInfo);
            }else {
                RedisService.getPlatformNotifySubscribeManager().removeMobilePositionSubscribe(platformId);
            }
        } catch (SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * 处理目录订阅
     */
    private void processNotifyCatalogList(SIPRequest request, Element rootElement) {
        if (request == null) {
            return;
        }
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        String platformId = SipUtils.getUserIdFromFromHeader(request);
        String deviceId = XmlUtils.getText(rootElement, "DeviceID");
        ParentPlatformVo platform = parentPlatformVoService.getParentPlatformByServerGbId(platformId);
        if (platform == null){
            return;
        }
        NotifySubscribeInfo subscribeInfo = new NotifySubscribeInfo(request, deviceId);

        String sn = XmlUtils.getText(rootElement, "SN");
        log.info("[回复上级的目录订阅请求]: {}/{}", platformId, deviceId);
        StringBuilder resultXml = new StringBuilder(200);
        resultXml.append("<?xml version=\"1.0\" ?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>Catalog</CmdType>\r\n")
                .append("<SN>").append(sn).append("</SN>\r\n")
                .append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
                .append("<Result>OK</Result>\r\n")
                .append("</Response>\r\n");


        try {
            SIPResponse response = responseXmlAck(request, resultXml.toString(), platform, subscribeInfo.getExpires());
            if (subscribeInfo.getExpires() > 0) {
                subscribeInfo.setResponse(response);
                RedisService.getPlatformNotifySubscribeManager().putCatalogSubscribe(platformId, subscribeInfo);
            }else if (subscribeInfo.getExpires() == 0) {
                RedisService.getPlatformNotifySubscribeManager().removeCatalogSubscribe(platformId);
            }
        } catch (SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * 处理报警订阅
     */
    private void processNotifyAlarm(SIPRequest request, Element rootElement) {
        if (request == null) {
            return;
        }
        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        String platformId = SipUtils.getUserIdFromFromHeader(request);
        String deviceId = XmlUtils.getText(rootElement, "DeviceID");
        ParentPlatformVo platform = parentPlatformVoService.getParentPlatformByServerGbId(platformId);
        if (platform == null){
            return;
        }
        NotifySubscribeInfo subscribeInfo = new NotifySubscribeInfo(request, deviceId);
        String sn = XmlUtils.getText(rootElement, "SN");
        log.info("[回复上级的目录订阅请求]: {}/{}", platformId, deviceId);
        StringBuilder resultXml = new StringBuilder(200);
        resultXml.append("<?xml version=\"1.0\" ?>\r\n")
                .append("<Response>\r\n")
                .append("<CmdType>Alarm</CmdType>\r\n")
                .append("<SN>").append(sn).append("</SN>\r\n")
                .append("<DeviceID>").append(deviceId).append("</DeviceID>\r\n")
                .append("<Result>OK</Result>\r\n")
                .append("</Response>\r\n");
        try {
            SIPResponse response = responseXmlAck(request, resultXml.toString(), platform, subscribeInfo.getExpires());
            if (subscribeInfo.getExpires() > 0) {
                subscribeInfo.setResponse(response);
                RedisService.getPlatformNotifySubscribeManager().putAlarmSubscribe(platformId, subscribeInfo);
            }else if (subscribeInfo.getExpires() == 0) {
                RedisService.getPlatformNotifySubscribeManager().removeAlarmSubscribe(platformId);
            }
        } catch (SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }
    }
}
