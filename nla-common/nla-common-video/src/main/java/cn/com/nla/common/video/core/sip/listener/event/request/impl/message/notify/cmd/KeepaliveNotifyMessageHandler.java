package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.cmd;

import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.core.utils.DynamicTask;
import cn.com.nla.common.video.basic.common.VideoConstant;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.notify.NotifyMessageHandler;
import cn.com.nla.common.video.core.utils.SipUtils;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

/**
 * 状态信息(心跳)报送
 */
@Slf4j
public class KeepaliveNotifyMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private DynamicTask dynamicTask;

    public KeepaliveNotifyMessageHandler(NotifyMessageHandler handler){
        handler.setMessageHandler(CmdType.KEEPALIVE_NOTIFY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        if (deviceVo == null) {
            // 未注册的设备不做处理
            return;
        }
        DateTime date = DateUtil.date();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        SIPRequest request = (SIPRequest) evt.getRequest();
        log.info("[收到心跳]， device: {}", deviceVo.getDeviceId());
        // 回复200 OK
        try {
            responseAck(request, Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 心跳回复: {}", e.getMessage());
        }
        Address remoteAddressInfo = SipUtils.getRemoteAddressFromRequest(request, videoProperties.getSipUseSourceIpAsRemoteAddress());
        if (!deviceVo.getIp().equalsIgnoreCase(remoteAddressInfo.getIp()) || deviceVo.getPort() != remoteAddressInfo.getPort()) {
            log.info("[心跳] 设备{}地址变化, 远程地址为: {}:{}", deviceVo.getDeviceId(), remoteAddressInfo.getIp(), remoteAddressInfo.getPort());
            deviceVo.setPort(remoteAddressInfo.getPort());
            deviceVo.setHostAddress(remoteAddressInfo.getIp().concat(":").concat(String.valueOf(remoteAddressInfo.getPort())));
            deviceVo.setIp(remoteAddressInfo.getIp());
            deviceVo.setLocalIp(request.getLocalAddress().getHostAddress());
            // 设备地址变化会引起目录订阅任务失效，需要重新添加
            if (RedisService.getDeviceNotifySubscribeManager().getCatalogSubscribe(deviceVo.getDeviceId())) {
                RedisService.getDeviceNotifySubscribeManager().removeCatalogSubscribe(deviceVo);
                RedisService.getDeviceNotifySubscribeManager().addCatalogSubscribe(deviceVo,"设备地址变化订阅");
            }
            if(RedisService.getDeviceNotifySubscribeManager().getMobilePositionSubscribe(deviceVo.getDeviceId())){
                RedisService.getDeviceNotifySubscribeManager().removeMobilePositionSubscribe(deviceVo);
                RedisService.getDeviceNotifySubscribeManager().addMobilePositionSubscribe(deviceVo,"设备地址变化订阅");
            }
            if(RedisService.getDeviceNotifySubscribeManager().getAlarmSubscribe(deviceVo.getDeviceId())){
                RedisService.getDeviceNotifySubscribeManager().removeAlarmSubscribe(deviceVo);
                RedisService.getDeviceNotifySubscribeManager().addAlarmSubscribe(deviceVo,"设备地址变化订阅");
            }
        }
        if (deviceVo.getKeepaliveTime() == null) {
            deviceVo.setHeartBeatInterval(60);
        }
        deviceVo.setKeepaliveTime(cn.hutool.core.date.LocalDateTimeUtil.of(date));
        if (deviceVo.getOnline() == ConstEnum.Flag.YES.getValue()) {
            if (deviceVo.getSubscribeCycleForCatalog() > 0 && !RedisService.getDeviceNotifySubscribeManager().getCatalogSubscribe(deviceVo.getDeviceId())) {
                // 查询在线设备那些开启了订阅，为设备开启定时的目录订阅
                RedisService.getDeviceNotifySubscribeManager().addCatalogSubscribe(deviceVo,"设备在线订阅");
            }
            if (deviceVo.getSubscribeCycleForMobilePosition() > 0 && !RedisService.getDeviceNotifySubscribeManager().getMobilePositionSubscribe(deviceVo.getDeviceId())) {
                RedisService.getDeviceNotifySubscribeManager().addMobilePositionSubscribe(deviceVo,"设备在线订阅");
            }
            if (deviceVo.getSubscribeCycleForAlarm() > 0 && !RedisService.getDeviceNotifySubscribeManager().getAlarmSubscribe(deviceVo.getDeviceId())) {
                RedisService.getDeviceNotifySubscribeManager().addAlarmSubscribe(deviceVo,"设备在线订阅");
            }
            deviceVoService.save(deviceVo);
        }else{
            deviceVoService.online(deviceVo,sipServer,sipCommander,videoProperties,null,"设备心跳");
        }
        // 刷新过期任务,如果三次心跳失败，则设置设备离线
        dynamicTask.startDelay(String.format("%s_%s", VideoConstant.REGISTER_EXPIRE_TASK_KEY_PREFIX, deviceVo.getDeviceId()), deviceVo.getHeartBeatInterval()*Math.max(deviceVo.getHeartBeatCount(),2),()-> deviceVoService.offline(deviceVo.getDeviceId(),"设备心跳-设备过期任务"));
        //缓存设备注册服务
        RedisService.getRegisterServerManager().putDevice(deviceVo.getDeviceId(),deviceVo.getHeartBeatInterval()+ VideoConstant.DELAY_TIME * 2 ,Address.builder().gbId(deviceVo.getDeviceId()).ip(sipServer.getAdvertisedIp(null)).port(sipServer.getSipConfigProperties().getPort()).build());
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        // 个别平台保活不回复200OK会判定离线
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 心跳回复: {}", e.getMessage());
        }
    }
}
