package cn.com.nla.common.video.core.sip.listener.event.request.impl;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.common.CatalogEventConstant;
import cn.com.nla.common.video.basic.common.CmdTypeConstant;
import cn.com.nla.common.video.basic.enums.CharsetType;
import cn.com.nla.common.video.basic.vo.video.DeviceAlarmVo;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceMobilePositionVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.*;
import cn.com.nla.common.video.core.sip.listener.event.request.AbstractSipRequestEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.SipRequestEvent;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

/**
 * SIP命令类型： NOTIFY请求,这是作为上级发送订阅请求后，设备才会响应的
 */
@Slf4j
public class NotifyRequestProcessor extends AbstractSipRequestEvent implements SipRequestEvent {



    @Override
    public String getMethod() {
        return Request.NOTIFY;
    }



    @Override
    public void process(RequestEvent event) {
        //回复订阅已接收
        try {
            responseAck((SIPRequest) event.getRequest(), Response.OK, null);
        }catch (SipException | InvalidArgumentException | ParseException e) {
            e.printStackTrace();
        }

        Element rootElement = getRootElement(event);
        if (rootElement == null) {
            log.error("处理NOTIFY消息时未获取到消息体,{}", event.getRequest());
            return;
        }

        try {
            String cmd = XmlUtils.getText(rootElement, "CmdType");
            if (CmdTypeConstant.CATALOG.equals(cmd)) {
                log.info("接收到Catalog通知");
                processNotifyCatalogList(event);
            } else if (CmdTypeConstant.ALARM.equals(cmd)) {
                log.info("接收到Alarm通知");
                processNotifyAlarm(event);
            } else if (CmdTypeConstant.MOBILE_POSITION.equals(cmd)) {
                log.info("接收到MobilePosition通知");
                processNotifyMobilePosition(event);
            } else {
                log.info("接收到消息：" + cmd);
            }
        }catch (Exception e){
            log.error("[NOTIFY请求]，消息处理异常：", e );
        }
    }

    /**
     * 处理catalog设备目录列表Notify
     * @param event
     */
    private void processNotifyCatalogList(RequestEvent event){
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        PlatformCatalogVoService platformCatalogVoService = VideoService.getPlatformCatalogService();
        FromHeader fromHeader = (FromHeader) event.getRequest().getHeader(FromHeader.NAME);
        String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);
        DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
        if(deviceVo == null || deviceVo.getOnline() == ConstEnum.Flag.NO.getValue()){
            log.warn("[收到目录订阅]：{}, 但是设备已经离线", (deviceVo != null ? deviceVo.getDeviceId():"" ));
            return;
        }
        Element rootElement = getRootElement(event, CharsetType.getName(deviceVo.getCharset()));
        if (rootElement == null) {
            log.warn("[ 收到目录订阅 ] content cannot be null, {}", event.getRequest());
            return;
        }
        Element deviceModel = XmlUtil.getElement(rootElement, "DeviceList");
        if(deviceModel == null){
            log.warn("[ 收到目录订阅 ] DeviceList is null");
            return;
        }
        List<Element> deviceList = XmlUtil.getElements(deviceModel, "Item");
        if (deviceList.isEmpty()) {
            log.warn("[ 收到目录订阅 ] DeviceList is null");
            return;
        }
        for (Element element : deviceList) {
            String channel = XmlUtils.getText(element, "DeviceID");
            if(StringUtils.isEmpty(channel)){
                continue;
            }
            String channelEvent = XmlUtils.getText(element, "Event");
            if(StringUtils.isEmpty(channelEvent)){
                log.warn("[收到目录订阅]：{}, 但是Event为空, 设为默认值 ADD", deviceVo.getDeviceId());
                channelEvent = CatalogEventConstant.ADD;
            }
            //根据xml解析通道信息
            DeviceChannelVo deviceChannelVo = XmlUtils.channelContentHandler(element, deviceVo, channelEvent);
            if(deviceChannelVo == null){
                log.error("[收到目录订阅]：{}, 但是解析 DeviceChannel is null", deviceVo.getDeviceId());
                continue;
            }
            log.info("[收到目录订阅]：{}/{}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
            switch (channelEvent){
                case CatalogEventConstant.ON:
                    // 上线
                    log.info("[收到通道上线通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.deviceChannelOnline(deviceId, deviceChannelVo.getChannelId(),true);
                    break;
                case CatalogEventConstant.OFF:
                    // 下线
                    log.info("[收到通道下线通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.deviceChannelOnline(deviceId, deviceChannelVo.getChannelId(),false);
                    break;
                case CatalogEventConstant.VLOST:
                    // 视频丢失
                    log.info("[收到通道视频丢失通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.deviceChannelOnline(deviceId, deviceChannelVo.getChannelId(),false);
                    break;
                case CatalogEventConstant.DEFECT:
                    // 故障
                    log.info("[收到通道视频故障通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.deviceChannelOnline(deviceId, deviceChannelVo.getChannelId(),false);
                    break;
                case CatalogEventConstant.ADD:
                    //增加
                    log.info("[收到增加通道通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.save(deviceChannelVo);
                    break;
                case CatalogEventConstant.DEL:
                    // 删除
                    log.info("[收到删除通道通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.del(deviceId, deviceChannelVo.getChannelId());
                    break;
                case CatalogEventConstant.UPDATE:
                    // 更新
                    log.info("[收到更新通道通知] 来自设备: {}, 通道 {}", deviceVo.getDeviceId(), deviceChannelVo.getChannelId());
                    deviceChannelVoService.save(deviceChannelVo);
                    break;
                default:
                    log.error("[收到目录订阅]：{}, 但是解析 解析事件不存在 event ：{}", deviceVo.getDeviceId(),channelEvent);
            }
            //数据处理
            platformCatalogVoService.handleCatalogEvent(channelEvent,null, Collections.singletonList(deviceChannelVo),null,null);
        }
    }

    /***
     * 处理alarm设备报警Notify
     * @param event
     */
    private void processNotifyAlarm(RequestEvent event){
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        DeviceMobilePositionVoService deviceMobilePositionVoService = VideoService.getDeviceMobilePositionService();
        DeviceAlarmVoService deviceAlarmVoService = VideoService.getDeviceAlarmService();

        FromHeader fromHeader = (FromHeader) event.getRequest().getHeader(FromHeader.NAME);
        String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

        Element rootElement = getRootElement(event);
        if(rootElement == null){
            log.error("处理alarm设备报警Notify时未获取到消息体{}", event.getRequest());
            return;
        }
        String channel = XmlUtils.getText(rootElement, "DeviceID");
        if(StringUtils.isEmpty(channel)){
            log.warn("[ NotifyAlarm ] 未找到通道信息：{}", deviceId);
            return;
        }
        if(StringUtils.equals(channel,deviceId)){
            DeviceChannelVo lastDevice = deviceChannelVoService.findLastDevice(deviceId);
            if(lastDevice != null){
                channel = lastDevice.getChannelId();
            }
        }
        DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
        if (deviceVo == null) {
            log.warn("[ NotifyAlarm ] 未找到设备：{}", deviceId);
            return;
        }
        rootElement = getRootElement(event, CharsetType.getName(deviceVo.getCharset()));
        if (rootElement == null) {
            log.warn("[ NotifyAlarm ] content cannot be null, {}", event.getRequest());
            return;
        }
        DeviceAlarmVo deviceAlarmVo = DeviceAlarmVo.builder()
                .deviceId(deviceId)
                .channelId(channel)
                .alarmPriority(Integer.valueOf(XmlUtils.getText(rootElement, "AlarmPriority")))
                .alarmMethod(Integer.valueOf(XmlUtils.getText(rootElement, "AlarmMethod")))
                .alarmTime(cn.hutool.core.date.LocalDateTimeUtil.of(DateUtil.parse(XmlUtils.getText(rootElement, "AlarmTime"))))
                .alarmDescription(XmlUtils.getText(rootElement, "AlarmDescription", ""))
                .longitude(Double.parseDouble(XmlUtils.getText(rootElement, "Longitude", "0.00")))
                .latitude(Double.parseDouble(XmlUtils.getText(rootElement, "Latitude", "0.00")))
                .build();

        if(deviceAlarmVo.getAlarmMethod() == 4){
            //设备移动信息
            DeviceMobilePositionVo deviceMobilePositionVo = DeviceMobilePositionVo.builder()
                    .deviceId(deviceId)
                    .channelId(channel)
                    .deviceName(deviceVo.getName())
                    .time(deviceAlarmVo.getAlarmTime())
                    .latitude(deviceAlarmVo.getLatitude())
                    .longitude(deviceAlarmVo.getLongitude())
                    .reportSource("GPS Alarm")
                    .build();
            //更新设备位置
            DeviceChannelVo build = DeviceChannelVo.builder()
                    .deviceId(deviceId)
                    .channelId(channel)
                    .latitude(deviceAlarmVo.getLatitude())
                    .longitude(deviceAlarmVo.getLongitude())
                    .gpsTime(deviceAlarmVo.getAlarmTime())
                    .build();
            build = build.initGps(deviceVo.getGeoCoordSys());

            deviceMobilePositionVo.setLongitudeWgs84(build.getLongitudeWgs84());
            deviceMobilePositionVo.setLatitudeWgs84(build.getLatitudeWgs84());
            deviceMobilePositionVo.setLongitudeGcj02(build.getLongitudeGcj02());
            deviceMobilePositionVo.setLatitudeGcj02(build.getLatitudeGcj02());

            if(videoProperties.getSavePositionHistory()){
                deviceMobilePositionVoService.save(deviceMobilePositionVo);
            }
            deviceChannelVoService.updateMobilePosition(build);
        }
        if(deviceVo.getSubscribeCycleForAlarm() > 0){
            deviceAlarmVoService.insert(deviceAlarmVo);
        }
    }

    /**
     * 处理MobilePosition移动位置Notify
     * @param event
     */
    private void processNotifyMobilePosition(RequestEvent event){
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        DeviceMobilePositionVoService deviceMobilePositionVoService = VideoService.getDeviceMobilePositionService();

        FromHeader fromHeader = (FromHeader) event.getRequest().getHeader(FromHeader.NAME);
        String deviceId = SipUtils.getUserIdFromFromHeader(fromHeader);

        Element rootElement = getRootElement(event);
        if(rootElement == null){
            log.error("处理MobilePosition移动位置Notify时未获取到消息体,{}", event.getRequest());
            return;
        }
        String channel = XmlUtils.getText(rootElement, "DeviceID");
        if(StringUtils.isEmpty(channel)){
            log.warn("[ NotifyAlarm ] 未找到通道信息：{}", deviceId);
            return;
        }
        DeviceVo deviceVo = deviceVoService.findDeviceGbId(deviceId);
        if (deviceVo == null) {
            log.warn("[ NotifyAlarm ] 未找到设备：{}", deviceId);
            return;
        }
        DeviceMobilePositionVo deviceMobilePositionVo = DeviceMobilePositionVo.builder()
                .deviceId(deviceVo.getDeviceId())
                .deviceName(deviceVo.getName())
                .channelId(channel)
                .time(cn.hutool.core.date.LocalDateTimeUtil.of(DateUtil.parse(XmlUtils.getText(rootElement, "Time"), DatePattern.UTC_SIMPLE_FORMAT)))
                .longitude(Double.parseDouble(XmlUtils.getText(rootElement, "Longitude", "0.00")))
                .latitude(Double.parseDouble(XmlUtils.getText(rootElement, "Latitude", "0.00")))
                .speed(Double.parseDouble(XmlUtils.getText(rootElement, "Speed", "0.00")))
                .direction(Double.parseDouble(XmlUtils.getText(rootElement, "Direction", "0.00")))
                .altitude(Double.parseDouble(XmlUtils.getText(rootElement, "Altitude", "0.00")))
                .reportSource("Mobile Position")
                .build();
        log.info("[收到移动位置订阅通知]：{}/{}->{}.{}", deviceMobilePositionVo.getDeviceId(), deviceMobilePositionVo.getChannelId(),
                deviceMobilePositionVo.getLongitude(), deviceMobilePositionVo.getLatitude());
        DeviceChannelVo build = DeviceChannelVo.builder()
                .deviceId(deviceId)
                .channelId(channel)
                .latitude(deviceMobilePositionVo.getLatitude())
                .longitude(deviceMobilePositionVo.getLongitude())
                .gpsTime(deviceMobilePositionVo.getTime())
                .build();
        build = build.initGps(deviceVo.getGeoCoordSys());

        deviceMobilePositionVo.setLongitudeWgs84(build.getLongitudeWgs84());
        deviceMobilePositionVo.setLatitudeWgs84(build.getLatitudeWgs84());
        deviceMobilePositionVo.setLongitudeGcj02(build.getLongitudeGcj02());
        deviceMobilePositionVo.setLatitudeGcj02(build.getLatitudeGcj02());

        if(videoProperties.getSavePositionHistory()){
            deviceMobilePositionVoService.save(deviceMobilePositionVo);
        }
        deviceChannelVoService.updateMobilePosition(build);
    }
}
