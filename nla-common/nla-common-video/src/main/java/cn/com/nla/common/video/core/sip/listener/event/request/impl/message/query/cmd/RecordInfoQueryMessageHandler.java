package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.cmd;

import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.sip.ChannelSourceInfo;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.service.video.ParentPlatformVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.QueryMessageHandler;
import cn.com.nla.common.video.core.redis.subscribe.record.RecordEndSubscribeHandle;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;

@Slf4j
public class RecordInfoQueryMessageHandler extends SipResponseEvent implements MessageHandler {

    @Autowired
    private RecordEndSubscribeHandle recordEndSubscribeHandle;

    public RecordInfoQueryMessageHandler(QueryMessageHandler handler){
        handler.setMessageHandler(CmdType.RECORD_INFO_QUERY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        //设备不会去查录像
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {

        SIPRequest request = (SIPRequest) evt.getRequest();
        int sn = NumberUtil.isInteger(XmlUtils.getText(element,"SN"))?Integer.parseInt(XmlUtils.getText(element,"SN")):0;
        String channelId = XmlUtils.getText(element,"DeviceID");
        String startTime = XmlUtils.getText(element,"StartTime");
        String endTime = XmlUtils.getText(element,"EndTime");
        int secrecy = NumberUtil.isInteger(XmlUtils.getText(element,"Secrecy"))?Integer.parseInt(XmlUtils.getText(element,"Secrecy")):0;
        String type = StringUtils.isEmpty(XmlUtils.getText(element,"Type"))? "all":XmlUtils.getText(element,"Type");

        ParentPlatformVoService parentPlatformVoService = VideoService.getParentPlatformService();
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        // 确认是直播还是国标， 国标直接请求下级，直播请求录像管理服务
        List<ChannelSourceInfo> channelSources = parentPlatformVoService.getChannelSource(parentPlatformVo.getServerGbId(), channelId);

        if (channelSources.get(0).getCount() > 0) { // 国标
            // 向国标设备请求录像数据
            DeviceVo deviceVo = deviceVoService.findPlatformIdChannelId(parentPlatformVo.getServerGbId(), channelId);
            DeviceChannelVo deviceChannelVo = deviceChannelVoService.findPlatformIdChannelId(parentPlatformVo.getServerGbId(), channelId);
            // 接收录像数据
            recordEndSubscribeHandle.addEndEventHandler(deviceChannelVo.getDeviceId(), channelId, (recordInfo)->{
                try {
                    sipCommanderForPlatform.recordInfo(sipServer, deviceChannelVo, parentPlatformVo, request.getFromTag(), recordInfo,null,null);
                } catch (SipException | InvalidArgumentException | ParseException e) {
                    log.error("[命令发送失败] 国标级联 回复录像数据: {}", e.getMessage());
                }
            });
            try {
                sipCommander.recordInfoQuery(sipServer, deviceVo, channelId, DateUtil.formatDateTime(DateUtil.parse(startTime, DatePattern.UTC_SIMPLE_PATTERN)),
                        DateUtil.formatDateTime(DateUtil.parse(endTime,DatePattern.UTC_SIMPLE_PATTERN)), sn, secrecy, type, (ok -> {
                            // 回复200 OK
                            try {
                                responseAck(request, Response.OK,null);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 录像查询回复: {}", e.getMessage());
                            }
                        }),(error -> {
                            // 查询失败
                            try {
                                responseAck(request, error.getStatusCode(), error.getMsg());
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 录像查询回复: {}", e.getMessage());
                            }
                        }));
            } catch (InvalidArgumentException | ParseException | SipException e) {
                log.error("[命令发送失败] 录像查询: {}", e.getMessage());
            }

        }else if (channelSources.get(1).getCount() > 0) { // 直播流
            // TODO
            try {
                responseAck(request, Response.NOT_IMPLEMENTED,null); // 回复未实现
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 录像查询: {}", e.getMessage());
            }
        }else { // 错误的请求
            try {
                responseAck(request, Response.BAD_REQUEST,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 录像查询: {}", e.getMessage());
            }
        }
    }
}
