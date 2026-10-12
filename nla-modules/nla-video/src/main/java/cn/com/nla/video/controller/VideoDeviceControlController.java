package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoSubscribeInfoVo;
import cn.com.nla.video.service.IVideoDeviceControlService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备配置与控制端点。
 *
 * <p>需要设备应答的查询与控制指令只返回受理凭据 {@code requestId}，结果经 SSE 消息通道推送；
 * 远程启动与订阅状态同步返回。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/deviceControl")
public class VideoDeviceControlController extends BaseController {

    private final IVideoDeviceControlService videoDeviceControlService;

    /**
     * 设备状态查询
     *
     * @param deviceId 设备国标编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:query")
    @GetMapping("/status")
    public R<VideoCommandTicketVo> status(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        return R.ok(videoDeviceControlService.queryStatus(deviceId));
    }

    /**
     * 设备报警查询
     *
     * @param deviceId      设备国标编号
     * @param startPriority 报警起始级别
     * @param endPriority   报警终止级别
     * @param alarmMethod   报警方式条件
     * @param alarmType     报警类型
     * @param startTime     报警发生起始时间
     * @param endTime       报警发生终止时间
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:query")
    @GetMapping("/alarm")
    public R<VideoCommandTicketVo> alarm(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                         @RequestParam(required = false) String startPriority,
                                         @RequestParam(required = false) String endPriority,
                                         @RequestParam(required = false) String alarmMethod,
                                         @RequestParam(required = false) String alarmType,
                                         @RequestParam(required = false) String startTime,
                                         @RequestParam(required = false) String endTime) {
        return R.ok(videoDeviceControlService.queryAlarm(deviceId, startPriority, endPriority, alarmMethod, alarmType,
            startTime, endTime));
    }

    /**
     * 远程启动控制
     *
     * @param deviceId 设备国标编号
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/start-control")
    public R<Void> startControl(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        videoDeviceControlService.startControl(deviceId);
        return R.ok();
    }

    /**
     * 获取设备订阅状态
     *
     * @param deviceId 设备国标编号
     * @return 订阅状态
     */
    @SaCheckPermission("video:deviceControl:query")
    @GetMapping("/subscribe-info")
    public R<VideoSubscribeInfoVo> subscribeInfo(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        return R.ok(videoDeviceControlService.subscribeInfo(deviceId));
    }

    /**
     * 录像控制
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param status    操作状态：0 停止，1 启用
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/record-control")
    public R<VideoCommandTicketVo> recordControl(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                                 @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                                 @RequestParam Integer status) {
        return R.ok(videoDeviceControlService.recordControl(deviceId, channelId, status));
    }

    /**
     * 布防/撤防控制
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param status    操作状态：0 撤防，1 布防
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/guard-control")
    public R<VideoCommandTicketVo> guardControl(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                                @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                                @RequestParam Integer status) {
        return R.ok(videoDeviceControlService.guardControl(deviceId, channelId, status));
    }

    /**
     * 报警复位
     *
     * @param deviceId    设备国标编号
     * @param channelId   通道国标编号
     * @param alarmMethod 报警方式
     * @param alarmType   报警类型
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/reset-alarm")
    public R<VideoCommandTicketVo> resetAlarm(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                              @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                              @RequestParam(required = false) String alarmMethod,
                                              @RequestParam(required = false) String alarmType) {
        return R.ok(videoDeviceControlService.resetAlarm(deviceId, channelId, alarmMethod, alarmType));
    }

    /**
     * 强制关键帧
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/i-frame")
    public R<VideoCommandTicketVo> iFrame(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                          @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoDeviceControlService.iFrame(deviceId, channelId));
    }

    /**
     * 看守位控制
     *
     * @param deviceId    设备国标编号
     * @param channelId   通道国标编号
     * @param enabled     看守位使能：1 开启，0 关闭
     * @param resetTime   自动归位时间间隔，单位秒
     * @param presetIndex 调用预置位编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/home-position")
    public R<VideoCommandTicketVo> homePosition(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                                @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                                @RequestParam(required = false) Integer enabled,
                                                @RequestParam(required = false) Integer resetTime,
                                                @RequestParam(required = false) Integer presetIndex) {
        return R.ok(videoDeviceControlService.homePosition(deviceId, channelId, enabled, resetTime, presetIndex));
    }

    /**
     * 拉框放大
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param length    播放窗口长度像素值
     * @param width     播放窗口宽度像素值
     * @param midpointx 拉框中心横轴坐标像素值
     * @param midpointy 拉框中心纵轴坐标像素值
     * @param lengthx   拉框长度像素值
     * @param lengthy   拉框宽度像素值
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/zoom-in")
    public R<VideoCommandTicketVo> zoomIn(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                          @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                          @RequestParam(defaultValue = "0") Integer length,
                                          @RequestParam(defaultValue = "0") Integer width,
                                          @RequestParam(defaultValue = "0") Integer midpointx,
                                          @RequestParam(defaultValue = "0") Integer midpointy,
                                          @RequestParam(defaultValue = "0") Integer lengthx,
                                          @RequestParam(defaultValue = "0") Integer lengthy) {
        return R.ok(videoDeviceControlService.zoomIn(deviceId, channelId, length, width, midpointx, midpointy, lengthx,
            lengthy));
    }

    /**
     * 拉框缩小
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param length    播放窗口长度像素值
     * @param width     播放窗口宽度像素值
     * @param midpointx 拉框中心横轴坐标像素值
     * @param midpointy 拉框中心纵轴坐标像素值
     * @param lengthx   拉框长度像素值
     * @param lengthy   拉框宽度像素值
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:control")
    @Log(title = "设备控制", businessType = BusinessType.OTHER)
    @GetMapping("/zoom-out")
    public R<VideoCommandTicketVo> zoomOut(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                           @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                           @RequestParam(defaultValue = "0") Integer length,
                                           @RequestParam(defaultValue = "0") Integer width,
                                           @RequestParam(defaultValue = "0") Integer midpointx,
                                           @RequestParam(defaultValue = "0") Integer midpointy,
                                           @RequestParam(defaultValue = "0") Integer lengthx,
                                           @RequestParam(defaultValue = "0") Integer lengthy) {
        return R.ok(videoDeviceControlService.zoomOut(deviceId, channelId, length, width, midpointx, midpointy,
            lengthx, lengthy));
    }

    /**
     * 基本配置设置
     *
     * @param deviceId          设备国标编号
     * @param channelId         通道国标编号
     * @param name              设备或通道名称
     * @param expiration        注册过期时间，单位秒
     * @param heartBeatInterval 心跳间隔时间，单位秒
     * @param heartBeatCount    心跳超时次数
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:edit")
    @Log(title = "设备配置", businessType = BusinessType.UPDATE)
    @GetMapping("/basic-param")
    public R<VideoCommandTicketVo> basicParam(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                              @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) String expiration,
                                              @RequestParam(required = false) String heartBeatInterval,
                                              @RequestParam(required = false) String heartBeatCount) {
        return R.ok(videoDeviceControlService.basicParam(deviceId, channelId, name, expiration, heartBeatInterval,
            heartBeatCount));
    }

    /**
     * 设备配置查询
     *
     * @param deviceId   设备国标编号
     * @param channelId  通道国标编号
     * @param configType 配置类型
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:deviceControl:query")
    @GetMapping("/query-param")
    public R<VideoCommandTicketVo> queryParam(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                              @RequestParam(required = false) String channelId,
                                              @RequestParam(required = false) String configType) {
        return R.ok(videoDeviceControlService.queryParam(deviceId, channelId, configType));
    }
}
