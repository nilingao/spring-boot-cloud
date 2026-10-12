package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.enums.ConstEnum;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.redis.RedisService;
import cn.com.nla.common.video.core.redis.subscribe.sip.message.SipSubscribeEvent;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoSubscribeInfoVo;
import cn.com.nla.video.service.IVideoDeviceControlService;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 设备配置与控制服务实现。
 *
 * <p>指令下发统一在受理阶段查设备，设备缺失或发送异常立即以失败回执释放，不再让前端等待超时；
 * 拉框指令串按旧实现逐字构造，由协议层包裹进 {@code <Control>} 报文。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoDeviceControlServiceImpl implements IVideoDeviceControlService {

    /**
     * 设备未回指令结果时的超时描述，沿用旧文案
     */
    private static final String UNSUPPORTED_MESSAGE = "设备不支持";

    /**
     * 录像指令：开始
     */
    private static final String RECORD_START = "Record";

    /**
     * 录像指令：停止
     */
    private static final String RECORD_STOP = "StopRecord";

    /**
     * 布防指令
     */
    private static final String GUARD_SET = "SetGuard";

    /**
     * 撤防指令
     */
    private static final String GUARD_RESET = "ResetGuard";

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public VideoCommandTicketVo queryStatus(String deviceId) {
        return dispatch("device-status", VideoCommandKeys.deviceStatus(deviceId), UNSUPPORTED_MESSAGE, deviceId,
            (deviceVo, errorEvent) -> sipCommander.deviceStatusQuery(sipServer, deviceVo, null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo queryAlarm(String deviceId, String startPriority, String endPriority,
                                          String alarmMethod, String alarmType, String startTime, String endTime) {
        return dispatch("device-alarm", VideoCommandKeys.alarm(deviceId), "设备报警查询超时", deviceId,
            (deviceVo, errorEvent) -> sipCommander.alarmInfoQuery(sipServer, deviceVo, startPriority, endPriority,
                alarmMethod, alarmType, startTime, endTime, null, errorEvent));
    }

    @Override
    public void startControl(String deviceId) {
        DeviceVo deviceVo = requireDevice(deviceId);
        try {
            sipCommander.teleBootCmd(sipServer, deviceVo, null, null);
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 远程启动：", e);
            throw new ServiceException("命令发送失败");
        }
    }

    @Override
    public VideoSubscribeInfoVo subscribeInfo(String deviceId) {
        requireDevice(deviceId);
        VideoSubscribeInfoVo vo = new VideoSubscribeInfoVo();
        vo.setAlarm(RedisService.getDeviceNotifySubscribeManager().getAlarmSubscribe(deviceId));
        vo.setCatalog(RedisService.getDeviceNotifySubscribeManager().getCatalogSubscribe(deviceId));
        vo.setMobilePosition(RedisService.getDeviceNotifySubscribeManager().getMobilePositionSubscribe(deviceId));
        return vo;
    }

    @Override
    public VideoCommandTicketVo recordControl(String deviceId, String channelId, Integer status) {
        String recordCmdStr = isYes(status) ? RECORD_START : RECORD_STOP;
        return dispatch("record-control", VideoCommandKeys.deviceControl(deviceId, channelId), UNSUPPORTED_MESSAGE,
            deviceId, (deviceVo, errorEvent) -> sipCommander.recordCmd(sipServer, deviceVo, channelId, recordCmdStr,
                null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo guardControl(String deviceId, String channelId, Integer status) {
        String guardCmdStr = isYes(status) ? GUARD_SET : GUARD_RESET;
        return dispatch("guard-control", VideoCommandKeys.deviceControl(deviceId, channelId), UNSUPPORTED_MESSAGE,
            deviceId, (deviceVo, errorEvent) -> sipCommander.guardCmd(sipServer, deviceVo, channelId, guardCmdStr,
                null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo resetAlarm(String deviceId, String channelId, String alarmMethod, String alarmType) {
        return dispatch("reset-alarm", VideoCommandKeys.deviceControl(deviceId, channelId), UNSUPPORTED_MESSAGE,
            deviceId, (deviceVo, errorEvent) -> sipCommander.alarmCmd(sipServer, deviceVo, alarmMethod, alarmType,
                null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo iFrame(String deviceId, String channelId) {
        return dispatch("i-frame", VideoCommandKeys.deviceControl(deviceId, channelId), UNSUPPORTED_MESSAGE, deviceId,
            (deviceVo, errorEvent) -> sipCommander.iFrameCmd(sipServer, deviceVo, channelId, null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo homePosition(String deviceId, String channelId, Integer enabled, Integer resetTime,
                                            Integer presetIndex) {
        return dispatch("home-position", VideoCommandKeys.deviceControl(deviceId, channelId), UNSUPPORTED_MESSAGE,
            deviceId, (deviceVo, errorEvent) -> sipCommander.homePositionCmd(sipServer, deviceVo, channelId,
                toStr(enabled), toStr(resetTime), toStr(presetIndex), null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo zoomIn(String deviceId, String channelId, Integer length, Integer width,
                                      Integer midpointx, Integer midpointy, Integer lengthx, Integer lengthy) {
        return dispatchZoom(deviceId, channelId, "DragZoomIn", "drag-zoom-in", length, width, midpointx, midpointy,
            lengthx, lengthy);
    }

    @Override
    public VideoCommandTicketVo zoomOut(String deviceId, String channelId, Integer length, Integer width,
                                       Integer midpointx, Integer midpointy, Integer lengthx, Integer lengthy) {
        return dispatchZoom(deviceId, channelId, "DragZoomOut", "drag-zoom-out", length, width, midpointx, midpointy,
            lengthx, lengthy);
    }

    @Override
    public VideoCommandTicketVo basicParam(String deviceId, String channelId, String name, String expiration,
                                          String heartBeatInterval, String heartBeatCount) {
        return dispatch("basic-param", VideoCommandKeys.deviceConfig(deviceId, channelId), UNSUPPORTED_MESSAGE,
            deviceId, (deviceVo, errorEvent) -> sipCommander.deviceBasicConfigCmd(sipServer, deviceVo, channelId,
                name, expiration, heartBeatInterval, heartBeatCount, null, errorEvent));
    }

    @Override
    public VideoCommandTicketVo queryParam(String deviceId, String channelId, String configType) {
        return dispatch("query-param", VideoCommandKeys.configDownload(deviceId, channelId), UNSUPPORTED_MESSAGE,
            deviceId, (deviceVo, errorEvent) -> sipCommander.deviceConfigQuery(sipServer, deviceVo, channelId,
                configType, null, errorEvent));
    }

    /**
     * 受理拉框缩放指令。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param tag       拉框报文标签
     * @param action    业务动作标识
     * @return 受理凭据
     */
    private VideoCommandTicketVo dispatchZoom(String deviceId, String channelId, String tag, String action,
                                              Integer length, Integer width, Integer midpointx, Integer midpointy,
                                              Integer lengthx, Integer lengthy) {
        String cmdXml = dragZoomXml(tag, length, width, midpointx, midpointy, lengthx, lengthy);
        return dispatch(action, VideoCommandKeys.deviceControl(deviceId, channelId), UNSUPPORTED_MESSAGE, deviceId,
            (deviceVo, errorEvent) -> sipCommander.dragZoomCmd(sipServer, deviceVo, channelId, cmdXml, null,
                errorEvent));
    }

    /**
     * 受理设备指令：查设备、下发、失败即释放回执。
     *
     * @param action         业务动作标识
     * @param key            回执路由键
     * @param timeoutMessage 超时描述
     * @param deviceId       设备国标编号
     * @param sender         指令下发逻辑
     * @return 受理凭据
     */
    private VideoCommandTicketVo dispatch(String action, String key, String timeoutMessage, String deviceId,
                                          CommandSender sender) {
        return videoCommandDispatcher.dispatch(action, key, VideoCommandDispatcher.DEFAULT_TIMEOUT_MILLIS,
            timeoutMessage, handle -> {
                DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
                if (deviceVo == null) {
                    handle.fail("未获取设备信息");
                    return;
                }
                try {
                    sender.send(deviceVo, error -> handle.fail(error.getStatusCode(), error.getMsg()));
                } catch (InvalidArgumentException | SipException | ParseException e) {
                    log.error("[命令发送失败] {}：", action, e);
                    handle.fail("命令发送失败");
                }
            });
    }

    /**
     * 构造拉框缩放报文片段。
     *
     * @param tag       报文标签：DragZoomIn / DragZoomOut
     * @param length    播放窗口长度像素值
     * @param width     播放窗口宽度像素值
     * @param midpointx 拉框中心横轴坐标像素值
     * @param midpointy 拉框中心纵轴坐标像素值
     * @param lengthx   拉框长度像素值
     * @param lengthy   拉框宽度像素值
     * @return 报文片段
     */
    private String dragZoomXml(String tag, Integer length, Integer width, Integer midpointx, Integer midpointy,
                               Integer lengthx, Integer lengthy) {
        StringBuilder xml = new StringBuilder(200);
        xml.append("<").append(tag).append(">\r\n");
        xml.append("<Length>").append(nz(length)).append("</Length>\r\n");
        xml.append("<Width>").append(nz(width)).append("</Width>\r\n");
        xml.append("<MidPointX>").append(nz(midpointx)).append("</MidPointX>\r\n");
        xml.append("<MidPointY>").append(nz(midpointy)).append("</MidPointY>\r\n");
        xml.append("<LengthX>").append(nz(lengthx)).append("</LengthX>\r\n");
        xml.append("<LengthY>").append(nz(lengthy)).append("</LengthY>\r\n");
        xml.append("</").append(tag).append(">\r\n");
        return xml.toString();
    }

    /**
     * 查询设备，缺失时抛出业务异常。
     *
     * @param deviceId 设备国标编号
     * @return 设备信息
     */
    private DeviceVo requireDevice(String deviceId) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
        if (deviceVo == null) {
            throw new ServiceException("未获取设备信息");
        }
        return deviceVo;
    }

    /**
     * 是否启用状态。
     *
     * @param status 状态值
     * @return true = 启用
     */
    private boolean isYes(Integer status) {
        return ObjectUtil.equal(status, ConstEnum.Flag.YES.getValue());
    }

    /**
     * 数值转协议字符串，空值保持为空由协议层按缺省处理。
     *
     * @param value 数值
     * @return 字符串，入参为空时返回 {@code null}
     */
    private String toStr(Integer value) {
        return value == null ? null : value.toString();
    }

    /**
     * 空值取零。
     *
     * @param value 数值
     * @return 数值，入参为空时返回 0
     */
    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 设备指令下发逻辑，允许抛出 SIP 受检异常，由受理阶段统一转成失败回执。
     */
    @FunctionalInterface
    private interface CommandSender {

        /**
         * 下发指令。
         *
         * @param deviceVo   设备信息
         * @param errorEvent 设备错误回执
         * @throws InvalidArgumentException SIP 参数非法
         * @throws SipException             SIP 发送异常
         * @throws ParseException           报文解析异常
         */
        void send(DeviceVo deviceVo, SipSubscribeEvent errorEvent)
            throws InvalidArgumentException, SipException, ParseException;
    }
}
