package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.cmd;

import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.query.QueryMessageHandler;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceChannelVoService;
import cn.com.nla.common.video.core.service.video.GbStreamVoService;
import cn.com.nla.common.video.core.service.video.PlatformCatalogVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 目录查询
 */
@Slf4j
public class CatalogQueryMessageHandler extends SipResponseEvent implements MessageHandler {

    public CatalogQueryMessageHandler(QueryMessageHandler handler){
        handler.setMessageHandler(CmdType.CATALOG_QUERY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        //设备没有目录查询
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        try {
            // 回复200 OK
            responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 目录查询回复200OK: {}", e.getMessage());
        }
        DeviceChannelVoService deviceChannelVoService = VideoService.getDeviceChannelService();
        GbStreamVoService gbStreamVoService = VideoService.getGbStreamService();
        PlatformCatalogVoService platformCatalogVoService = VideoService.getPlatformCatalogService();

        String sn = XmlUtils.getText(element,"SN");
        // 目录与设备通道的关系
        List<DeviceChannelVo> deviceChannelVoInPlatforms = deviceChannelVoService.queryChannelWithCatalog(parentPlatformVo.getServerGbId());
        // 目录与拉流推流之间的关系
        List<DeviceChannelVo> gbStreams = gbStreamVoService.queryGbStreamListInPlatform(parentPlatformVo.getServerGbId(),null);
        // 目录与上级平台的关系
        List<DeviceChannelVo> catalogs =  platformCatalogVoService.queryCatalogInPlatform(parentPlatformVo.getServerGbId());
        List<DeviceChannelVo> allChannels = new ArrayList<>();

        // 回复目录
        if (catalogs.size() > 0) {
            allChannels.addAll(catalogs);
        }
        // 回复级联的通道
        if (deviceChannelVoInPlatforms.size() > 0) {
            allChannels.addAll(deviceChannelVoInPlatforms);
        }
        // 回复直播的通道
        if (gbStreams.size() > 0) {
            allChannels.addAll(gbStreams);
        }
        try {
            if (allChannels.size() > 0) {
                sipCommanderForPlatform.catalogQuery(sipServer,allChannels, parentPlatformVo, sn, fromHeader.getTag(),null,null);
            }else {
                // 回复无通道
                sipCommanderForPlatform.catalogQuery(sipServer,null, parentPlatformVo, sn, fromHeader.getTag(), 0,null,null);
            }
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 国标级联 目录查询回复: {}", e.getMessage());
        }
    }
}
