package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IVideoPtzService;
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
 * 云台控制端点。
 *
 * <p>前端控制指令国标只回 ACK，接口即发即返；预置位查询返回受理凭据 {@code requestId}，
 * 结果经 SSE 消息通道推送。点动指令高频触发，故不写操作日志，避免审计日志刷屏。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/ptz")
public class VideoPtzController extends BaseController {

    private final IVideoPtzService videoPtzService;

    /**
     * 前端控制（国标指令码直传）
     *
     * @param deviceId     设备国标编号
     * @param channelId    通道国标编号
     * @param cmdCode      指令码
     * @param parameter1   数据一
     * @param parameter2   数据二
     * @param combindCode2 组合码二
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:control")
    @GetMapping("/front-end-command")
    public R<Void> frontEndCommand(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                   @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                   @RequestParam(defaultValue = "0") Integer cmdCode,
                                   @RequestParam(defaultValue = "0") Integer parameter1,
                                   @RequestParam(defaultValue = "0") Integer parameter2,
                                   @RequestParam(defaultValue = "0") Integer combindCode2) {
        videoPtzService.frontEndCommand(deviceId, channelId, cmdCode, parameter1, parameter2, combindCode2);
        return R.ok();
    }

    /**
     * 云台方向与缩放控制
     *
     * @param deviceId      设备国标编号
     * @param channelId     通道国标编号
     * @param command       控制指令
     * @param horizonSpeed  水平移动速度
     * @param verticalSpeed 垂直移动速度
     * @param zoomSpeed     缩放速度
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:control")
    @GetMapping("/ptz")
    public R<Void> ptz(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                       @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                       @NotBlank(message = "控制指令不能为空") @RequestParam String command,
                       @RequestParam(defaultValue = "0") Integer horizonSpeed,
                       @RequestParam(defaultValue = "0") Integer verticalSpeed,
                       @RequestParam(defaultValue = "0") Integer zoomSpeed) {
        videoPtzService.ptz(deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed);
        return R.ok();
    }

    /**
     * 光圈控制
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：in/out/stop
     * @param speed     速度
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:control")
    @GetMapping("/iris")
    public R<Void> iris(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                        @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                        @NotBlank(message = "控制指令不能为空") @RequestParam String command,
                        @RequestParam Integer speed) {
        videoPtzService.iris(deviceId, channelId, command, speed);
        return R.ok();
    }

    /**
     * 聚焦控制
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：near/far/stop
     * @param speed     速度
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:control")
    @GetMapping("/focus")
    public R<Void> focus(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                         @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                         @NotBlank(message = "控制指令不能为空") @RequestParam String command,
                         @RequestParam Integer speed) {
        videoPtzService.focus(deviceId, channelId, command, speed);
        return R.ok();
    }

    /**
     * 查询预置位
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号，为空时按设备查询
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:ptz:preset")
    @GetMapping("/preset-query")
    public R<VideoCommandTicketVo> presetQuery(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                               @RequestParam(required = false) String channelId) {
        return R.ok(videoPtzService.presetQuery(deviceId, channelId));
    }

    /**
     * 设置预置位
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:preset")
    @GetMapping("/add-preset")
    public R<Void> addPreset(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                             @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                             @RequestParam Integer presetId) {
        videoPtzService.addPreset(deviceId, channelId, presetId);
        return R.ok();
    }

    /**
     * 调用预置位
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:preset")
    @GetMapping("/call-preset")
    public R<Void> callPreset(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                              @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                              @RequestParam Integer presetId) {
        videoPtzService.callPreset(deviceId, channelId, presetId);
        return R.ok();
    }

    /**
     * 删除预置位
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:preset")
    @GetMapping("/del-preset")
    public R<Void> delPreset(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                             @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                             @RequestParam Integer presetId) {
        videoPtzService.delPreset(deviceId, channelId, presetId);
        return R.ok();
    }

    /**
     * 巡航组添加巡航点
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号
     * @param cruiseId  巡航组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:cruise")
    @GetMapping("/add-cruise-point")
    public R<Void> addCruisePoint(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                  @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                  @RequestParam Integer presetId,
                                  @RequestParam Integer cruiseId) {
        videoPtzService.addCruisePoint(deviceId, channelId, presetId, cruiseId);
        return R.ok();
    }

    /**
     * 巡航组删除巡航点
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param presetId  预置位编号
     * @param cruiseId  巡航组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:cruise")
    @GetMapping("/del-cruise-point")
    public R<Void> delCruisePoint(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                  @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                  @RequestParam Integer presetId,
                                  @RequestParam Integer cruiseId) {
        videoPtzService.delCruisePoint(deviceId, channelId, presetId, cruiseId);
        return R.ok();
    }

    /**
     * 设置巡航速度
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号
     * @param speed     巡航速度
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:cruise")
    @GetMapping("/speed-cruise-point")
    public R<Void> speedCruisePoint(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                    @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                    @RequestParam Integer cruiseId,
                                    @RequestParam Integer speed) {
        videoPtzService.speedCruisePoint(deviceId, channelId, cruiseId, speed);
        return R.ok();
    }

    /**
     * 设置巡航停留时间
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号
     * @param time      停留时间，单位秒
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:cruise")
    @GetMapping("/time-cruise-point")
    public R<Void> timeCruisePoint(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                   @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                   @RequestParam Integer cruiseId,
                                   @RequestParam Integer time) {
        videoPtzService.timeCruisePoint(deviceId, channelId, cruiseId, time);
        return R.ok();
    }

    /**
     * 开始巡航
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:cruise")
    @GetMapping("/start-cruise-point")
    public R<Void> startCruisePoint(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                    @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                    @RequestParam Integer cruiseId) {
        videoPtzService.startCruisePoint(deviceId, channelId, cruiseId);
        return R.ok();
    }

    /**
     * 停止巡航
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param cruiseId  巡航组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:cruise")
    @GetMapping("/stop-cruise-point")
    public R<Void> stopCruisePoint(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                   @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                   @RequestParam Integer cruiseId) {
        videoPtzService.stopCruisePoint(deviceId, channelId, cruiseId);
        return R.ok();
    }

    /**
     * 开始自动扫描
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:scan")
    @GetMapping("/start-scan")
    public R<Void> startScan(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                             @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                             @RequestParam Integer scanId) {
        videoPtzService.startScan(deviceId, channelId, scanId);
        return R.ok();
    }

    /**
     * 停止自动扫描
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:scan")
    @GetMapping("/stop-scan")
    public R<Void> stopScan(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                            @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                            @RequestParam Integer scanId) {
        videoPtzService.stopScan(deviceId, channelId, scanId);
        return R.ok();
    }

    /**
     * 设置自动扫描左边界
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:scan")
    @GetMapping("/set-left-scan")
    public R<Void> setLeftScan(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                               @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                               @RequestParam Integer scanId) {
        videoPtzService.setLeftScan(deviceId, channelId, scanId);
        return R.ok();
    }

    /**
     * 设置自动扫描右边界
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:scan")
    @GetMapping("/set-right-scan")
    public R<Void> setRightScan(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                @RequestParam Integer scanId) {
        videoPtzService.setRightScan(deviceId, channelId, scanId);
        return R.ok();
    }

    /**
     * 设置自动扫描速度
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param scanId    扫描组号
     * @param speed     扫描速度
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:scan")
    @GetMapping("/set-speed-scan")
    public R<Void> setSpeedScan(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                @RequestParam Integer scanId,
                                @RequestParam Integer speed) {
        videoPtzService.setSpeedScan(deviceId, channelId, scanId, speed);
        return R.ok();
    }

    /**
     * 辅助开关控制-雨刷
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：on/off
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:control")
    @GetMapping("/wiper")
    public R<Void> wiper(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                         @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                         @NotBlank(message = "控制指令不能为空") @RequestParam String command) {
        videoPtzService.wiper(deviceId, channelId, command);
        return R.ok();
    }

    /**
     * 辅助开关控制
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param command   控制指令：on/off
     * @param switchId  开关编号
     * @return 操作结果
     */
    @SaCheckPermission("video:ptz:control")
    @GetMapping("/auxiliary")
    public R<Void> auxiliary(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                             @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                             @NotBlank(message = "控制指令不能为空") @RequestParam String command,
                             @RequestParam Integer switchId) {
        videoPtzService.auxiliary(deviceId, channelId, command, switchId);
        return R.ok();
    }
}
