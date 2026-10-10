package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.cmd;

import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.QueryMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;

@Slf4j
public class DeviceStatusQueryMessageHandler extends SipResponseEvent implements MessageHandler {

    public DeviceStatusQueryMessageHandler(QueryMessageHandler handler){
        handler.setMessageHandler(CmdType.DEVICE_STATUS_QUERY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        //设备不会去查设备状态
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        log.info("接收到DeviceStatus查询消息");
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 DeviceStatus查询回复200OK: {}", e.getMessage());
        }
        String sn = XmlUtils.getText(element,"SN");
        String channelId = XmlUtils.getText(element, "DeviceID");
        DeviceChannelVo deviceChannelVo = deviceChannelVoService.findPlatformIdChannelId(parentPlatformVo.getServerGbId(), channelId);
        if (deviceChannelVo ==null){
            log.error("[平台没有该通道的使用权限]:platformId"+ parentPlatformVo.getServerGbId()+"  deviceID:"+channelId);
            return;
        }
        try {
            sipCommanderForPlatform.deviceStatusResponse(sipServer, parentPlatformVo,channelId, sn, fromHeader.getTag(), deviceChannelVo.getStatus(),null,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 DeviceStatus查询回复: {}", e.getMessage());
        }
    }
}
