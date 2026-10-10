package cn.com.nla.common.video.core.sip.listener.event.request.impl.message.control.cmd;

import cn.com.nla.common.video.basic.bean.DragZoomRequest;
import cn.com.nla.common.video.basic.bean.HomePositionRequest;
import cn.com.nla.common.video.basic.enums.CmdType;
import cn.com.nla.common.video.basic.enums.DeviceControlType;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.basic.vo.video.ParentPlatformVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.service.video.DeviceVoService;
import cn.com.nla.common.video.core.sip.listener.event.request.SipResponseEvent;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.MessageHandler;
import cn.com.nla.common.video.core.sip.listener.event.request.impl.message.control.ControlMessageHandler;
import cn.com.nla.common.video.core.model.EventResult;
import cn.com.nla.common.video.core.utils.XmlUtils;
import cn.hutool.core.util.XmlUtil;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Element;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.SipURI;
import javax.sip.message.Response;
import java.text.ParseException;
import java.util.List;

/**
 * 设备控制
 */
@Slf4j
public class DeviceControlQueryMessageHandler extends SipResponseEvent implements MessageHandler {


    public DeviceControlQueryMessageHandler(ControlMessageHandler handler){
        handler.setMessageHandler(CmdType.DEVICE_CONTROL_NOTIFY.getValue(),this);
    }

    @Override
    public void handForDevice(RequestEvent evt, DeviceVo deviceVo, Element element) {
        //暂无
    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatformVo parentPlatformVo, Element element) {
        // 此处是上级发出的DeviceControl指令
        SIPRequest request = (SIPRequest) evt.getRequest();
        String targetGBId = ((SipURI) request.getToHeader().getAddress().getURI()).getUser();
        String channelId = XmlUtils.getText(element, "DeviceID");
        DeviceVoService deviceVoService = VideoService.getDeviceService();
        // 远程启动功能
//        if (!ObjectUtils.isEmpty(XmlUtils.getText(element, "TeleBoot")) && parentPlatform.getServerGbId().equals(targetGBId)) {
//            // 远程启动本平台：需要在重新启动程序后先对SipStack解绑
//            log.info("执行远程启动本平台命令");
//            try {
//                sipCommanderForPlatform.unregister(sipServer,parentPlatform, null, null);
//            } catch (InvalidArgumentException | ParseException | SipException e) {
//                log.error("[命令发送失败] 国标级联 注销: {}", e.getMessage());
//            }
//        }
        DeviceControlType deviceControlType = DeviceControlType.typeOf(element);
        log.info("[接受deviceControl命令] 命令: {}", deviceControlType);
        if(deviceControlType == null || parentPlatformVo.getServerGbId().equals(targetGBId)){
            try {
                responseAck(request, Response.NOT_FOUND,"接受deviceControl命令错误 未获取到当前类型");
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        //判断是否存在该通道
        DeviceVo deviceVoForPlatform = deviceVoService.findPlatformIdChannelId(parentPlatformVo.getServerGbId(), channelId);
        if (deviceVoForPlatform == null) {
            try {
                responseAck(request, Response.NOT_FOUND,String.format("未获取当前通道的设备：%s : %s", parentPlatformVo.getServerGbId(), channelId));
            } catch (SipException | InvalidArgumentException | ParseException e) {
                log.error("[命令发送失败] 错误信息: {}", e.getMessage());
            }
            return;
        }
        switch (deviceControlType){
            case PTZ:
                handlePtzCmd(deviceVoForPlatform, channelId, element, request, DeviceControlType.PTZ);
                break;
            case ALARM:
                handleAlarmCmd(deviceVoForPlatform, element, request);
                break;
            case GUARD:
                handleGuardCmd(deviceVoForPlatform, element, request, DeviceControlType.GUARD);
                break;
            case RECORD:
                handleRecordCmd(deviceVoForPlatform, channelId, element, request, DeviceControlType.RECORD);
                break;
            case I_FRAME:
                handleIFameCmd(deviceVoForPlatform, request, channelId);
                break;
            case TELE_BOOT:
                handleTeleBootCmd(deviceVoForPlatform, request);
                break;
            case DRAG_ZOOM_IN:
                handleDragZoom(deviceVoForPlatform, channelId, element, request, DeviceControlType.DRAG_ZOOM_IN);
                break;
            case DRAG_ZOOM_OUT:
                handleDragZoom(deviceVoForPlatform, channelId, element, request, DeviceControlType.DRAG_ZOOM_OUT);
                break;
            case HOME_POSITION:
                handleHomePositionCmd(deviceVoForPlatform, channelId, element, request, DeviceControlType.HOME_POSITION);
                break;
            default:
                break;
        }


    }




    /**
     * 处理云台指令
     *
     * @param deviceVo      设备
     * @param channelId   通道id
     * @param rootElement
     * @param request
     */
    private void handlePtzCmd(DeviceVo deviceVo, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        String cmdString = XmlUtils.getText(rootElement, type.getVal());
        try {
            sipCommander.fronEndCmd(sipServer, deviceVo, channelId, cmdString,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 云台/前端: {}", e.getMessage());
        }
    }

    /**
     * 处理强制关键帧
     *
     * @param deviceVo    设备
     * @param channelId 通道id
     */
    private void handleIFameCmd(DeviceVo deviceVo, SIPRequest request, String channelId) {
        try {
            sipCommander.iFrameCmd(sipServer, deviceVo, channelId,null,null);
            responseAck(request, Response.OK,null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 强制关键帧: {}", e.getMessage());
        }
    }

    /**
     * 处理重启命令
     *
     * @param deviceVo 设备信息
     */
    private void handleTeleBootCmd(DeviceVo deviceVo, SIPRequest request) {
        try {
            sipCommander.teleBootCmd(sipServer, deviceVo,null,null);
            responseAck(request, Response.OK,null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 重启: {}", e.getMessage());
        }

    }

    /**
     * 处理拉框控制***
     *
     * @param deviceVo      设备信息
     * @param channelId   通道id
     * @param rootElement 根节点
     * @param type        消息类型
     */
    private void handleDragZoom(DeviceVo deviceVo, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        try {
            DragZoomRequest dragZoomRequest = XmlUtils.loadElement(rootElement, DragZoomRequest.class);
            DragZoomRequest.DragZoom dragZoom = dragZoomRequest.getDragZoomIn();
            if (dragZoom == null) {
                dragZoom = dragZoomRequest.getDragZoomOut();
            }
            StringBuffer cmdXml = new StringBuffer(200);
            cmdXml.append("<" + type.getVal() + ">\r\n");
            cmdXml.append("<Length>" + dragZoom.getLength() + "</Length>\r\n");
            cmdXml.append("<Width>" + dragZoom.getWidth() + "</Width>\r\n");
            cmdXml.append("<MidPointX>" + dragZoom.getMidPointX() + "</MidPointX>\r\n");
            cmdXml.append("<MidPointY>" + dragZoom.getMidPointY() + "</MidPointY>\r\n");
            cmdXml.append("<LengthX>" + dragZoom.getLengthX() + "</LengthX>\r\n");
            cmdXml.append("<LengthY>" + dragZoom.getLengthY() + "</LengthY>\r\n");
            cmdXml.append("</" + type.getVal() + ">\r\n");
            sipCommander.dragZoomCmd(sipServer, deviceVo, channelId, cmdXml.toString(),null,null);
            responseAck(request, Response.OK,null);
        } catch (Exception e) {
            log.error("[命令发送失败] 拉框控制: {}", e.getMessage());
        }

    }

    /**
     * 处理看守位命令***
     *
     * @param deviceVo      设备信息
     * @param channelId   通道id
     * @param rootElement 根节点
     * @param request     请求信息
     * @param type        消息类型
     */
    private void handleHomePositionCmd(DeviceVo deviceVo, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        try {
            HomePositionRequest homePosition = XmlUtils.loadElement(rootElement, HomePositionRequest.class);
            //获取整个消息主体，我们只需要修改请求头即可
            HomePositionRequest.HomePosition info = homePosition.getHomePosition();
            sipCommander.homePositionCmd(sipServer, deviceVo, channelId, info.getEnabled(), info.getResetTime(), info.getPresetIndex(),
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (Exception e) {
            log.error("[命令发送失败] 看守位设置: {}", e.getMessage());
        }
    }

    /**
     * 处理告警消息***
     *
     * @param deviceVo      设备信息
     * @param rootElement 根节点
     * @param request     请求信息
     */
    private void handleAlarmCmd(DeviceVo deviceVo, Element rootElement, SIPRequest request) {
        //告警方法
        String alarmMethod = "";
        //告警类型
        String alarmType = "";
        Element infoModel = XmlUtil.getElement(rootElement, "Info");
        if(infoModel != null){
            List<Element> info = XmlUtil.getElements(infoModel,"Item");
            for (Element element : info) {
                alarmMethod = XmlUtils.getText(element, "AlarmMethod");
                alarmType = XmlUtils.getText(element, "AlarmType");
            }
        }
        try {
            sipCommander.alarmCmd(sipServer, deviceVo, alarmMethod, alarmType,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 告警消息: {}", e.getMessage());
        }
    }

    /**
     * 处理录像控制
     *
     * @param deviceVo      设备信息
     * @param channelId   通道id
     * @param rootElement 根节点
     * @param request     请求信息
     * @param type        消息类型
     */
    private void handleRecordCmd(DeviceVo deviceVo, String channelId, Element rootElement, SIPRequest request, DeviceControlType type) {
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = XmlUtils.getText(rootElement, type.getVal());
        try {
            sipCommander.recordCmd(sipServer, deviceVo, channelId, cmdString,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 录像控制: {}", e.getMessage());
        }
    }

    /**
     * 处理报警布防/撤防命令
     *
     * @param deviceVo      设备信息
     * @param rootElement 根节点
     * @param request     请求信息
     * @param type        消息类型
     */
    private void handleGuardCmd(DeviceVo deviceVo, Element rootElement, SIPRequest request, DeviceControlType type) {
        //获取整个消息主体，我们只需要修改请求头即可
        String cmdString = XmlUtils.getText(rootElement, type.getVal());
        try {
            sipCommander.guardCmd(sipServer, deviceVo,null, cmdString,
                    errorResult -> onError(request, errorResult),
                    okResult -> onOk(request, okResult));
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 布防/撤防命令: {}", e.getMessage());
        }
    }


    /**
     * 错误响应处理
     *
     * @param request     请求
     * @param eventResult 响应结构
     */
    private void onError(SIPRequest request, EventResult eventResult) {
        // 失败的回复
        try {
            responseAck(request, eventResult.getStatusCode(), eventResult.getMsg());
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }

    /**
     * 成功响应处理
     *
     * @param request     请求
     * @param eventResult 响应结构
     */
    private void onOk(SIPRequest request, EventResult eventResult) {
        // 成功的回复
        try {
            responseAck(request, eventResult.getStatusCode(),null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 回复: {}", e.getMessage());
        }
    }
}
