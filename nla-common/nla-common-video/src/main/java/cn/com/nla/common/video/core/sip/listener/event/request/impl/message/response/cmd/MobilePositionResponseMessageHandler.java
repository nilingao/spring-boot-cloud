package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.basic.enums.RespCode;
import cn.com.nla.common.video.basic.model.ProtocolResult;
import cn.com.nla.common.video.basic.enums.CharsetType;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceMobilePositionVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.subscribe.result.DeferredResultHolder;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.service.video.DeviceMobilePositionVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 移动设备位置数据查询回复
 */
@Slf4j
public class MobilePositionResponseMessageHandler extends SipResponseEvent implements MessageHandler {
    @Autowired
    private DeferredResultHolder deferredResultHolder;

    public MobilePositionResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.MOBILE_POSITION_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        SIPRequest request = (SIPRequest) evt.getRequest();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        DeviceMobilePositionVoService deviceMobilePositionVoService = VideoService.getDeviceMobilePositionService();
        element = getRootElement(evt, CharsetType.getName(deviceVo.getCharset()));
        if (element == null) {
            log.warn("[ 移动设备位置数据查询回复 ] content cannot be null, {}", evt.getRequest());
            try {
                responseAck(request, Response.BAD_REQUEST,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 移动设备位置数据查询 BAD_REQUEST: {}", e.getMessage());
            }
            return;
        }
        DeviceMobilePositionVo mobilePosition = new DeviceMobilePositionVo();
        if (!ObjectUtils.isEmpty(deviceVo.getName())) {
            mobilePosition.setDeviceName(deviceVo.getName());
        }
        mobilePosition.setDeviceId(deviceVo.getDeviceId());
        mobilePosition.setChannelId(XmlUtils.getText(element, "DeviceID"));
        mobilePosition.setTime(cn.hutool.core.date.LocalDateTimeUtil.of(DateUtil.parse(XmlUtils.getText(element, "Time"))));
        mobilePosition.setLongitude(Double.parseDouble((XmlUtils.getText(element, "Longitude"))));
        mobilePosition.setLatitude(Double.parseDouble((XmlUtils.getText(element, "Latitude"))));
        if (NumberUtil.isNumber((XmlUtils.getText(element, "Speed")))) {
            mobilePosition.setSpeed(Double.parseDouble((XmlUtils.getText(element, "Speed"))));
        } else {
            mobilePosition.setSpeed(0.0);
        }
        if (NumberUtil.isNumber((XmlUtils.getText(element, "Direction")))) {
            mobilePosition.setDirection(Double.parseDouble((XmlUtils.getText(element, "Direction"))));
        } else {
            mobilePosition.setDirection(0.0);
        }
        if (NumberUtil.isNumber((XmlUtils.getText(element, "Altitude")))) {
            mobilePosition.setAltitude(Double.parseDouble((XmlUtils.getText(element, "Altitude"))));
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
        //回复 200 OK
        try {
            responseAck(request, Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 移动设备位置数据查询 200: {}", e.getMessage());
        }
        //查询回复
        String key = String.format("%s%s", DeferredResultHolder.CALLBACK_CMD_MOBILEPOSITION,deviceVo.getDeviceId());
        deferredResultHolder.invokeAllResult(key, ProtocolResult.result(RespCode.CODE_0.getValue(),null,mobilePosition));
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {

    }
}
