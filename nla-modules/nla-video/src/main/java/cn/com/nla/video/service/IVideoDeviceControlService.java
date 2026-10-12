package cn.com.nla.video.service;

import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoSubscribeInfoVo;

/**
 * 设备配置与控制服务：查询与控制类指令需设备回执，统一走 SSE 异步回执；
 * 远程启动与订阅状态为同步操作。
 *
 * @author TZY
 */
public interface IVideoDeviceControlService {

    /**
     * 设备状态查询，结果经 SSE 回执推送。
     *
     * @param deviceId 设备国标编号
     * @return 受理凭据
     */
    VideoCommandTicketVo queryStatus(String deviceId);

    /**
     * 设备报警查询，结果经 SSE 回执推送。
     *
     * @param deviceId      设备国标编号
     * @param startPriority 报警起始级别（可选）
     * @param endPriority   报警终止级别（可选）
     * @param alarmMethod   报警方式条件（可选）
     * @param alarmType     报警类型（可选）
     * @param startTime     报警发生起始时间（可选）
     * @param endTime       报警发生终止时间（可选）
     * @return 受理凭据
     */
    VideoCommandTicketVo queryAlarm(String deviceId, String startPriority, String endPriority, String alarmMethod,
                                    String alarmType, String startTime, String endTime);

    /**
     * 远程启动控制。
     *
     * @param deviceId 设备国标编号
     */
    void startControl(String deviceId);

    /**
     * 查询设备订阅状态。
     *
     * @param deviceId 设备国标编号
     * @return 订阅状态
     */
    VideoSubscribeInfoVo subscribeInfo(String deviceId);

    /**
     * 录像控制，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param status    操作状态：0 停止，1 启用
     * @return 受理凭据
     */
    VideoCommandTicketVo recordControl(String deviceId, String channelId, Integer status);

    /**
     * 布防/撤防控制，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param status    操作状态：0 撤防，1 布防
     * @return 受理凭据
     */
    VideoCommandTicketVo guardControl(String deviceId, String channelId, Integer status);

    /**
     * 报警复位，结果经 SSE 回执推送。
     *
     * @param deviceId    设备国标编号
     * @param channelId   通道国标编号，仅用于回执路由
     * @param alarmMethod 报警方式
     * @param alarmType   报警类型
     * @return 受理凭据
     */
    VideoCommandTicketVo resetAlarm(String deviceId, String channelId, String alarmMethod, String alarmType);

    /**
     * 强制关键帧，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 受理凭据
     */
    VideoCommandTicketVo iFrame(String deviceId, String channelId);

    /**
     * 看守位控制，结果经 SSE 回执推送。
     *
     * @param deviceId    设备国标编号
     * @param channelId   通道国标编号
     * @param enabled     看守位使能：1 开启，0 关闭
     * @param resetTime   自动归位时间间隔，单位秒
     * @param presetIndex 调用预置位编号，取值 0-255
     * @return 受理凭据
     */
    VideoCommandTicketVo homePosition(String deviceId, String channelId, Integer enabled, Integer resetTime,
                                      Integer presetIndex);

    /**
     * 拉框放大，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param length    播放窗口长度像素值
     * @param width     播放窗口宽度像素值
     * @param midpointx 拉框中心横轴坐标像素值
     * @param midpointy 拉框中心纵轴坐标像素值
     * @param lengthx   拉框长度像素值
     * @param lengthy   拉框宽度像素值
     * @return 受理凭据
     */
    VideoCommandTicketVo zoomIn(String deviceId, String channelId, Integer length, Integer width, Integer midpointx,
                                Integer midpointy, Integer lengthx, Integer lengthy);

    /**
     * 拉框缩小，结果经 SSE 回执推送。
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param length    播放窗口长度像素值
     * @param width     播放窗口宽度像素值
     * @param midpointx 拉框中心横轴坐标像素值
     * @param midpointy 拉框中心纵轴坐标像素值
     * @param lengthx   拉框长度像素值
     * @param lengthy   拉框宽度像素值
     * @return 受理凭据
     */
    VideoCommandTicketVo zoomOut(String deviceId, String channelId, Integer length, Integer width, Integer midpointx,
                                 Integer midpointy, Integer lengthx, Integer lengthy);

    /**
     * 基本配置设置，结果经 SSE 回执推送。
     *
     * @param deviceId          设备国标编号
     * @param channelId         通道国标编号
     * @param name              设备或通道名称（可选）
     * @param expiration        注册过期时间，单位秒（可选）
     * @param heartBeatInterval 心跳间隔时间，单位秒（可选）
     * @param heartBeatCount    心跳超时次数（可选）
     * @return 受理凭据
     */
    VideoCommandTicketVo basicParam(String deviceId, String channelId, String name, String expiration,
                                    String heartBeatInterval, String heartBeatCount);

    /**
     * 设备配置查询，结果经 SSE 回执推送。
     *
     * @param deviceId   设备国标编号
     * @param channelId  通道国标编号（可选）
     * @param configType 配置类型：BasicParam/VideoParamOpt/SVACEncodeConfig/SVACDecodeConfig
     * @return 受理凭据
     */
    VideoCommandTicketVo queryParam(String deviceId, String channelId, String configType);
}
