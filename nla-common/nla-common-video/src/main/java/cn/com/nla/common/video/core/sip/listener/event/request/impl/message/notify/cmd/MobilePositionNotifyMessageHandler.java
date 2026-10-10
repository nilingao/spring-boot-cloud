package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd;

import cn.com.nla.common.video.basic.enums.CharsetType;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceMobilePositionVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.service.video.DeviceMobilePositionVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.NotifyMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.Date;

/**
 * 移动设备位置数据通知，设备主动发起，不需要上级订阅
 */
@Slf4j
public class MobilePositionNotifyMessageHandler extends SipResponseEvent implements MessageHandler {

    public MobilePositionNotifyMessageHandler(NotifyMessageHandler handler){
        handler.setMessageHandler(CmdType.MOBILE_POSITION_NOTIFY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 移动位置通知回复: {}", e.getMessage());
        }
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        DeviceMobilePositionVoService deviceMobilePositionVoService = VideoService.getDeviceMobilePositionService();
        try {
            Element rootElementAfterCharset = getRootElement(evt, CharsetType.getName(deviceVo.getCharset()));
            if (rootElementAfterCharset == null) {
                log.warn("[移动位置通知] {}处理失败，未识别到信息体", deviceVo.getDeviceId());
                return;
            }
            DeviceMobilePositionVo mobilePosition = new DeviceMobilePositionVo();
            if (!ObjectUtils.isEmpty(deviceVo.getName())) {
                mobilePosition.setDeviceName(deviceVo.getName());
            }
            mobilePosition.setDeviceId(deviceVo.getDeviceId());
            mobilePosition.setChannelId(XmlUtils.getText(rootElementAfterCharset, "DeviceID"));
            mobilePosition.setTime(cn.hutool.core.date.LocalDateTimeUtil.of(StringUtils.isNotEmpty(XmlUtils.getText(rootElementAfterCharset, "Time"))?DateUtil.parse(XmlUtils.getText(rootElementAfterCharset, "Time")):new Date()));
            mobilePosition.setLongitude(Double.parseDouble(XmlUtils.getText(rootElementAfterCharset, "Longitude")));
            mobilePosition.setLatitude(Double.parseDouble(XmlUtils.getText(rootElementAfterCharset, "Latitude")));
            if (NumberUtil.isNumber(XmlUtils.getText(rootElementAfterCharset, "Speed"))) {
                mobilePosition.setSpeed(Double.parseDouble(XmlUtils.getText(rootElementAfterCharset, "Speed")));
            } else {
                mobilePosition.setSpeed(0.0);
            }
            if (NumberUtil.isNumber(XmlUtils.getText(rootElementAfterCharset, "Direction"))) {
                mobilePosition.setDirection(Double.parseDouble(XmlUtils.getText(rootElementAfterCharset, "Direction")));
            } else {
                mobilePosition.setDirection(0.0);
            }
            if (NumberUtil.isNumber(XmlUtils.getText(rootElementAfterCharset, "Altitude"))) {
                mobilePosition.setAltitude(Double.parseDouble(XmlUtils.getText(rootElementAfterCharset, "Altitude")));
            } else {
                mobilePosition.setAltitude(0.0);
            }
            mobilePosition.setReportSource("Mobile Position");

            // 更新device channel 的经纬度
            DeviceChannelVo deviceChannelVo = new DeviceChannelVo();
            deviceChannelVo.setDeviceId(deviceVo.getDeviceId());
            deviceChannelVo.setChannelId(mobilePosition.getChannelId());
            deviceChannelVo.setLongitude(mobilePosition.getLongitude());
            deviceChannelVo.setLatitude(mobilePosition.getLatitude());
            deviceChannelVo.setGpsTime(mobilePosition.getTime());
            deviceChannelVo.initGps(deviceVo.getGeoCoordSys());
            mobilePosition.setLongitudeWgs84(deviceChannelVo.getLongitudeWgs84());
            mobilePosition.setLatitudeWgs84(deviceChannelVo.getLatitudeWgs84());
            mobilePosition.setLongitudeGcj02(deviceChannelVo.getLongitudeGcj02());
            mobilePosition.setLatitudeGcj02(deviceChannelVo.getLatitudeGcj02());

            if (videoProperties.getSavePositionHistory()) {
                deviceMobilePositionVoService.save(mobilePosition);
            }
            deviceChannelVoService.updateMobilePosition(deviceChannelVo);
        } catch (Exception e) {
            log.warn("[移动位置通知] 发现未处理的异常, \r\n{}", evt.getRequest());
            log.error("[移动位置通知] 异常内容： ", e);
        }

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        // 不会收到上级平台的移动设备位置通知
    }
}
