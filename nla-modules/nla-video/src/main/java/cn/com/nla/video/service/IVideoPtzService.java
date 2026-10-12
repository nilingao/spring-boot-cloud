package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.VideoCommandTicketVo;

/**
 * 云台控制服务。
 *
 * <p>前端控制类指令（方向/光圈/聚焦/预置位/巡航/扫描/辅助开关）国标只回 ACK 不回执行结果，
 * 按旧实现保持即发即返，避免连续点动时被在途指令去重拦截；只有预置位查询走 SSE 异步回执。</p>
 *
 * @author TZY
 */
public interface IVideoPtzService {

    /**
     * 前端控制（国标 PTZ 指令码直传）。
     *
     * @param deviceId     设备国标编号
     * @param channelId    通道国标编号
     * @param cmdCode      指令码，国标指令格式字节4
     * @param parameter1   数据一，取值 0-255
     * @param parameter2   数据二，取值 0-255
     * @param combindCode2 组合码二，取值 0-15
     */
    void frontEndCommand(String deviceId, String channelId, Integer cmdCode, Integer parameter1, Integer parameter2,
                         Integer combindCode2);

    /**
     * 云台方向与缩放控制。
     *
     * @param deviceId      设备国标编号
     * @param channelId     通道国标编号
     * @param command       控制指令：left/right/up/down/upleft/upright/downleft/downright/zoomin/zoomout/stop
     * @param horizonSpeed  水平移动速度，取值 0-255
     * @param verticalSpeed 垂直移动速度，取值 0-255
     * @param zoomSpeed     缩放速度，取值 0-15
     */
    void ptz(String deviceId, String channelId, String command, Integer horizonSpeed, Integer verticalSpeed,
             Integer zoomSpeed);

    /**
     * 光圈控制。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：in/out/stop
     * @param speed     速度，取值 0-255
     */
    void iris(String deviceId, String channelId, String command, Integer speed);

    /**
     * 聚焦控制。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：near/far/stop
     * @param speed     速度，取值 0-255
     */
    void focus(String deviceId, String channelId, String command, Integer speed);

    /**
     * 预置位查询，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号，为空时按设备查询
     * @return 指令受理凭据
     */
    VideoCommandTicketVo presetQuery(String deviceId, String channelId);

    /**
     * 设置预置位。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号，取值 1-255
     */
    void addPreset(String deviceId, String channelId, Integer presetId);

    /**
     * 调用预置位。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号，取值 1-255
     */
    void callPreset(String deviceId, String channelId, Integer presetId);

    /**
     * 删除预置位。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号，取值 1-255
     */
    void delPreset(String deviceId, String channelId, Integer presetId);

    /**
     * 巡航组添加巡航点。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号，取值 1-255
     * @param cruiseId  巡航组号，取值 1-255
     */
    void addCruisePoint(String deviceId, String channelId, Integer presetId, Integer cruiseId);

    /**
     * 巡航组删除巡航点。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号，取值 1-255
     * @param cruiseId  巡航组号，取值 1-255
     */
    void delCruisePoint(String deviceId, String channelId, Integer presetId, Integer cruiseId);

    /**
     * 设置巡航速度。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号，取值 1-255
     * @param speed     巡航速度，取值 1-4095
     */
    void speedCruisePoint(String deviceId, String channelId, Integer cruiseId, Integer speed);

    /**
     * 设置巡航停留时间。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号，取值 1-255
     * @param time      停留时间，单位秒，取值 1-4095
     */
    void timeCruisePoint(String deviceId, String channelId, Integer cruiseId, Integer time);

    /**
     * 开始巡航。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号，取值 1-255
     */
    void startCruisePoint(String deviceId, String channelId, Integer cruiseId);

    /**
     * 停止巡航。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号，取值 1-255
     */
    void stopCruisePoint(String deviceId, String channelId, Integer cruiseId);

    /**
     * 开始自动扫描。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号，取值 1-255
     */
    void startScan(String deviceId, String channelId, Integer scanId);

    /**
     * 停止自动扫描。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号，取值 1-255
     */
    void stopScan(String deviceId, String channelId, Integer scanId);

    /**
     * 设置自动扫描左边界。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号，取值 1-255
     */
    void setLeftScan(String deviceId, String channelId, Integer scanId);

    /**
     * 设置自动扫描右边界。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号，取值 1-255
     */
    void setRightScan(String deviceId, String channelId, Integer scanId);

    /**
     * 设置自动扫描速度。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号，取值 1-255
     * @param speed     扫描速度，取值 1-4095
     */
    void setSpeedScan(String deviceId, String channelId, Integer scanId, Integer speed);

    /**
     * 辅助开关控制-雨刷。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：on/off
     */
    void wiper(String deviceId, String channelId, String command);

    /**
     * 辅助开关控制。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：on/off
     * @param switchId  开关编号，数据一
     */
    void auxiliary(String deviceId, String channelId, String command, Integer switchId);
}
