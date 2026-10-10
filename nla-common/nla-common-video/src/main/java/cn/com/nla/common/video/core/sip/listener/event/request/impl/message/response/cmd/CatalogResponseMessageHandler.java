package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.cmd;

import cn.com.nla.common.video.basic.enums.CharsetType;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.vo.video.DeviceChannelVo;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.impl.CatalogDataManager;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.response.ResponseMessageHandler;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 目录查询的回复
 */
@Slf4j
public class CatalogResponseMessageHandler extends SipResponseEvent implements MessageHandler {

    public CatalogResponseMessageHandler(ResponseMessageHandler handler){
        handler.setMessageHandler(CmdType.CATALOG_RESPONSE.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        try {
            CatalogDataManager catalogDataManager = RedisService.getCatalogDataManager();
            try {
                // 回复200 OK
                responseAck((SIPRequest) evt.getRequest(), Response.OK,null);
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 目录查询 查询: {}", e.getMessage());
            }
            Element rootElement = getRootElement(evt, CharsetType.getName(deviceVo.getCharset()));
            if (rootElement == null) {
                log.warn("[ 收到通道 ] content cannot be null, {}", evt.getRequest());
                return;
            }
            int sn = Integer.parseInt(XmlUtils.getText(rootElement,"SN"));
            int sumNum = Integer.parseInt(XmlUtils.getText(rootElement,"SumNum"));
            if (sumNum == 0) {
                log.info("[收到通道]设备:{}的: 0个", deviceVo.getDeviceId());
                // 数据已经完整接收
                VideoService.getDeviceChannelService().delAll(deviceVo.getDeviceId());
                RedisService.getCatalogDataManager().put(deviceVo.getDeviceId(), sn, sumNum, deviceVo, new ArrayList<>());
            } else {
                Element deviceModel = XmlUtil.getElement(rootElement, "DeviceList");
                if(deviceModel == null){
                    log.warn("[ 收到通道 ] 未获取通道信息 DeviceList, {}", evt.getRequest());
                    return;
                }
                List<Element> deviceList = XmlUtil.getElements(deviceModel, "Item");
                if (! deviceList.isEmpty()) {
                    List<DeviceChannelVo> channelList = new ArrayList<>();
                    // 遍历DeviceList
                    for (Element deviceElement : deviceList) {
                        String deviceID = XmlUtils.getText(deviceElement, "DeviceID");
                        if (StringUtils.isEmpty(deviceID)) {
                            continue;
                        }
                        DeviceChannelVo deviceChannelVo = XmlUtils.channelContentHandler(deviceElement, deviceVo, null);
                        if (deviceChannelVo != null) {
                            deviceChannelVo.initGps(deviceVo.getGeoCoordSys());
                            deviceChannelVo.setDeviceId(deviceVo.getDeviceId());
                            channelList.add(deviceChannelVo);
                        }
                    }
                    if(!channelList.isEmpty()){
                        synchronized (this){
                            List<DeviceChannelVo> deviceChannelVos = catalogDataManager.get(deviceVo.getDeviceId());
                            deviceChannelVos.addAll(channelList);
                            catalogDataManager.put(deviceVo.getDeviceId(), sn, sumNum, deviceVo, deviceChannelVos);
                            List<DeviceChannelVo> deviceChannelVoList = RedisService.getCatalogDataManager().get(deviceVo.getDeviceId());
                            log.info("[收到通道]设备: {} -> {}个，{}/{}", deviceVo.getDeviceId(), channelList.size(), deviceChannelVoList == null ? 0 : deviceChannelVoList.size(), sumNum);
                            if (deviceChannelVos.size() >= sumNum) {
                                // 数据已经完整接收， 此时可能存在某个设备离线变上线的情况，但是考虑到性能，此处不做处理，
                                // 目前支持设备通道上线通知时和设备上线时向上级通知
                                boolean resetChannelsResult = VideoService.getDeviceChannelService().resetChannels(deviceVo.getDeviceId(), deviceChannelVos);
                                if (!resetChannelsResult) {
                                    String errorMsg = "接收成功，写入失败，共" + sumNum + "条，已接收" + RedisService.getCatalogDataManager().get(deviceVo.getDeviceId()).size() + "条";
                                    RedisService.getCatalogDataManager().setChannelSyncEnd(deviceVo.getDeviceId(), errorMsg);
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e) {
            log.warn("[收到通道] 发现未处理的异常, \r\n{}", evt.getRequest());
            log.error("[收到通道] 异常内容： ", e);
        }
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        //没有上级回复
    }
}
