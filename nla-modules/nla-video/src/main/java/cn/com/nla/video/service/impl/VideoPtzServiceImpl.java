package cn.com.nla.video.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.video.basic.vo.video.DeviceVo;
import cn.com.nla.common.video.core.service.VideoService;
import cn.com.nla.common.video.core.sip.SipServer;
import cn.com.nla.common.video.core.sip.cmd.SIPCommander;
import cn.com.nla.video.command.VideoCommandDispatcher;
import cn.com.nla.video.command.VideoCommandKeys;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IVideoPtzService;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 云台控制服务实现。
 *
 * <p>指令码映射与取值范围校验完全对齐旧实现，保证前端与设备侧行为不变；
 * 预置位查询按 {@code 通道优先、缺省回落设备} 的键路由回执。</p>
 *
 * @author TZY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
public class VideoPtzServiceImpl implements IVideoPtzService {

    /**
     * 水平/垂直速度缺省值
     */
    private static final int DEFAULT_MOVE_SPEED = 100;

    /**
     * 缩放速度缺省值
     */
    private static final int DEFAULT_ZOOM_SPEED = 16;

    private final SipServer sipServer;
    private final SIPCommander sipCommander;
    private final VideoCommandDispatcher videoCommandDispatcher;

    @Override
    public void frontEndCommand(String deviceId, String channelId, Integer cmdCode, Integer parameter1,
                                Integer parameter2, Integer combindCode2) {
        checkRange(parameter1, 0, 255, "parameter1 为 0-255的数字");
        checkRange(parameter2, 0, 255, "parameter2 为 0-255的数字");
        checkRange(combindCode2, 0, 15, "combindCode2 为 0-15的数字");
        send(deviceId, channelId, cmdCode, parameter1, parameter2, combindCode2);
    }

    @Override
    public void ptz(String deviceId, String channelId, String command, Integer horizonSpeed, Integer verticalSpeed,
                    Integer zoomSpeed) {
        int horizon = checkSpeed(horizonSpeed, 0, 255, DEFAULT_MOVE_SPEED, "horizonSpeed 为 0-255的数字");
        int vertical = checkSpeed(verticalSpeed, 0, 255, DEFAULT_MOVE_SPEED, "verticalSpeed 为 0-255的数字");
        int zoom = checkSpeed(zoomSpeed, 0, 15, DEFAULT_ZOOM_SPEED, "zoomSpeed 为 0-15的数字");
        int cmdCode = switch (command) {
            case "left" -> 2;
            case "right" -> 1;
            case "up" -> 8;
            case "down" -> 4;
            case "upleft" -> 10;
            case "upright" -> 9;
            case "downleft" -> 6;
            case "downright" -> 5;
            case "zoomin" -> 16;
            case "zoomout" -> 32;
            default -> 0;
        };
        if ("stop".equals(command)) {
            horizon = 0;
            vertical = 0;
            zoom = 0;
        }
        send(deviceId, channelId, cmdCode, horizon, vertical, zoom);
    }

    @Override
    public void iris(String deviceId, String channelId, String command, Integer speed) {
        int value = checkSpeed(speed, 0, 255, DEFAULT_MOVE_SPEED, "speed 为 0-255的数字");
        int cmdCode = switch (command) {
            case "in" -> 0x44;
            case "out" -> 0x48;
            default -> 0x40;
        };
        if ("stop".equals(command)) {
            value = 0;
        }
        send(deviceId, channelId, cmdCode, 0, value, 0);
    }

    @Override
    public void focus(String deviceId, String channelId, String command, Integer speed) {
        int value = checkSpeed(speed, 0, 255, DEFAULT_MOVE_SPEED, "speed 为 0-255的数字");
        int cmdCode = switch (command) {
            case "near" -> 0x42;
            case "far" -> 0x41;
            default -> 0x40;
        };
        if ("stop".equals(command)) {
            value = 0;
        }
        send(deviceId, channelId, cmdCode, value, 0, 0);
    }

    @Override
    public VideoCommandTicketVo presetQuery(String deviceId, String channelId) {
        String key = VideoCommandKeys.presetQuery(StrUtil.isBlank(channelId) ? deviceId : channelId);
        return videoCommandDispatcher.dispatch("preset-query", key, handle -> {
            DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
            if (deviceVo == null) {
                handle.fail(String.format("设备：%s 未找到", deviceId));
                return;
            }
            try {
                sipCommander.presetQuery(sipServer, deviceVo, channelId, null,
                    error -> handle.fail(error.getStatusCode(), error.getMsg()));
            } catch (InvalidArgumentException | SipException | ParseException e) {
                log.error("[命令发送失败] 查询预置位：", e);
                handle.fail("命令发送失败");
            }
        });
    }

    @Override
    public void addPreset(String deviceId, String channelId, Integer presetId) {
        checkPresetId(presetId, "预置位编号必须为1-255之间的数字");
        send(deviceId, channelId, 0x81, 1, presetId, 0);
    }

    @Override
    public void callPreset(String deviceId, String channelId, Integer presetId) {
        checkPresetId(presetId, "预置位编号必须为1-255之间的数字");
        send(deviceId, channelId, 0x82, 1, presetId, 0);
    }

    @Override
    public void delPreset(String deviceId, String channelId, Integer presetId) {
        checkPresetId(presetId, "预置位编号必须为1-255之间的数字");
        send(deviceId, channelId, 0x83, 1, presetId, 0);
    }

    @Override
    public void addCruisePoint(String deviceId, String channelId, Integer presetId, Integer cruiseId) {
        checkPresetId(presetId, "预置位编号必须为1-255之间的数字, 为0时删除整个巡航");
        checkCruiseId(cruiseId);
        send(deviceId, channelId, 0x84, cruiseId, presetId, 0);
    }

    @Override
    public void delCruisePoint(String deviceId, String channelId, Integer presetId, Integer cruiseId) {
        checkPresetId(presetId, "预置位编号必须为1-255之间的数字, 为0时删除整个巡航");
        checkCruiseId(cruiseId);
        send(deviceId, channelId, 0x85, cruiseId, presetId, 0);
    }

    @Override
    public void speedCruisePoint(String deviceId, String channelId, Integer cruiseId, Integer speed) {
        checkCruiseId(cruiseId);
        checkRange(speed, 1, 4095, "巡航速度必须为1-4095之间的数字");
        send(deviceId, channelId, 0x86, cruiseId, speed & 0xFF, speed >> 8);
    }

    @Override
    public void timeCruisePoint(String deviceId, String channelId, Integer cruiseId, Integer time) {
        checkCruiseId(cruiseId);
        checkRange(time, 1, 4095, "巡航停留时间必须为1-4095之间的数字");
        send(deviceId, channelId, 0x87, cruiseId, time & 0xFF, time >> 8);
    }

    @Override
    public void startCruisePoint(String deviceId, String channelId, Integer cruiseId) {
        checkCruiseId(cruiseId);
        send(deviceId, channelId, 0x88, cruiseId, 0, 0);
    }

    @Override
    public void stopCruisePoint(String deviceId, String channelId, Integer cruiseId) {
        checkCruiseId(cruiseId);
        // 停止巡航按国标发送全零指令，巡航组号仅用于参数校验
        send(deviceId, channelId, 0, 0, 0, 0);
    }

    @Override
    public void startScan(String deviceId, String channelId, Integer scanId) {
        checkScanId(scanId);
        send(deviceId, channelId, 0x89, scanId, 0, 0);
    }

    @Override
    public void stopScan(String deviceId, String channelId, Integer scanId) {
        checkScanId(scanId);
        // 停止扫描按国标发送全零指令，扫描组号仅用于参数校验
        send(deviceId, channelId, 0, 0, 0, 0);
    }

    @Override
    public void setLeftScan(String deviceId, String channelId, Integer scanId) {
        checkScanId(scanId);
        send(deviceId, channelId, 0x89, scanId, 1, 0);
    }

    @Override
    public void setRightScan(String deviceId, String channelId, Integer scanId) {
        checkScanId(scanId);
        send(deviceId, channelId, 0x89, scanId, 2, 0);
    }

    @Override
    public void setSpeedScan(String deviceId, String channelId, Integer scanId, Integer speed) {
        checkScanId(scanId);
        checkRange(speed, 1, 4095, "自动扫描速度必须为1-4095之间的数字");
        send(deviceId, channelId, 0x8A, scanId, speed & 0xFF, speed >> 8);
    }

    @Override
    public void wiper(String deviceId, String channelId, String command) {
        send(deviceId, channelId, auxiliaryCmdCode(command), 1, 0, 0);
    }

    @Override
    public void auxiliary(String deviceId, String channelId, String command, Integer switchId) {
        send(deviceId, channelId, auxiliaryCmdCode(command), switchId, 0, 0);
    }

    /**
     * 辅助开关指令码映射。
     *
     * @param command 控制指令：on/off
     * @return 国标指令码，未识别时返回停止指令 0
     */
    private int auxiliaryCmdCode(String command) {
        return switch (command) {
            case "on" -> 0x8c;
            case "off" -> 0x8d;
            default -> 0;
        };
    }

    /**
     * 发送前端控制指令。
     *
     * @param deviceId      设备国标编号
     * @param channelId     通道国标编号
     * @param cmdCode       指令码
     * @param parameter1    数据一
     * @param parameter2    数据二
     * @param combindCode2  组合码二
     */
    private void send(String deviceId, String channelId, int cmdCode, int parameter1, int parameter2,
                      int combindCode2) {
        DeviceVo deviceVo = VideoService.getDeviceService().findDeviceGbId(deviceId);
        if (deviceVo == null) {
            throw new ServiceException(String.format("设备：%s 未找到", deviceId));
        }
        try {
            sipCommander.frontEndCmd(sipServer, deviceVo, channelId, cmdCode, parameter1, parameter2, combindCode2,
                null, null);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            log.error("[命令发送失败] 前端控制：", e);
            throw new ServiceException("命令发送失败");
        }
    }

    /**
     * 校验速度类参数，为空时回退缺省值。
     *
     * @param value        参数值
     * @param min          最小值
     * @param max          最大值
     * @param defaultValue 缺省值
     * @param message      错误提示
     * @return 生效参数值
     */
    private int checkSpeed(Integer value, int min, int max, int defaultValue, String message) {
        if (value == null) {
            return defaultValue;
        }
        if (value < min || value > max) {
            throw new ServiceException(message);
        }
        return value;
    }

    /**
     * 校验闭区间参数。
     *
     * @param value   参数值
     * @param min     最小值
     * @param max     最大值
     * @param message 错误提示
     */
    private void checkRange(Integer value, int min, int max, String message) {
        if (value == null || value < min || value > max) {
            throw new ServiceException(message);
        }
    }

    /**
     * 校验预置位编号。
     *
     * @param presetId 预置位编号
     * @param message  错误提示
     */
    private void checkPresetId(Integer presetId, String message) {
        checkRange(presetId, 1, 255, message);
    }

    /**
     * 校验巡航组号。
     *
     * @param cruiseId 巡航组号
     */
    private void checkCruiseId(Integer cruiseId) {
        checkRange(cruiseId, 1, 255, "巡航组号必须为1-255之间的数字");
    }

    /**
     * 校验扫描组号。
     *
     * @param scanId 扫描组号
     */
    private void checkScanId(Integer scanId) {
        checkRange(scanId, 1, 255, "扫描组号必须为1-255之间的数字");
    }
}
