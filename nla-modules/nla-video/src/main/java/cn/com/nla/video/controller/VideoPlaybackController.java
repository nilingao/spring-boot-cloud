package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IVideoPlaybackService;
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
 * 历史视频回放端点。
 *
 * <p>起播为异步指令，接口只返回受理凭据 {@code requestId}，收流结果经 SSE 消息通道推送；
 * 暂停/恢复/拖动/倍速为同步控制指令，直接返回执行结果。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/playback")
public class VideoPlaybackController extends BaseController {

    private final IVideoPlaybackService videoPlaybackService;

    /**
     * 开始回放
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param startTime 开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime   结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:playback:start")
    @Log(title = "视频回放", businessType = BusinessType.OTHER)
    @GetMapping("/start")
    public R<VideoCommandTicketVo> start(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                         @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                         @NotBlank(message = "开始时间不能为空") @RequestParam String startTime,
                                         @NotBlank(message = "结束时间不能为空") @RequestParam String endTime) {
        return R.ok(videoPlaybackService.start(deviceId, channelId, startTime, endTime));
    }

    /**
     * 停止回放
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param streamId  流ID
     * @return 操作结果
     */
    @SaCheckPermission("video:playback:stop")
    @Log(title = "视频回放", businessType = BusinessType.OTHER)
    @GetMapping("/stop")
    public R<Void> stop(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                        @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                        @NotBlank(message = "流ID不能为空") @RequestParam String streamId) {
        videoPlaybackService.stop(deviceId, channelId, streamId);
        return R.ok();
    }

    /**
     * 暂停回放
     *
     * @param streamId 流ID
     * @return 操作结果
     */
    @SaCheckPermission("video:playback:suspend")
    @Log(title = "视频回放", businessType = BusinessType.OTHER)
    @GetMapping("/suspend")
    public R<Void> suspend(@NotBlank(message = "流ID不能为空") @RequestParam String streamId) {
        videoPlaybackService.suspend(streamId);
        return R.ok();
    }

    /**
     * 恢复回放
     *
     * @param streamId 流ID
     * @return 操作结果
     */
    @SaCheckPermission("video:playback:restore")
    @Log(title = "视频回放", businessType = BusinessType.OTHER)
    @GetMapping("/restore")
    public R<Void> restore(@NotBlank(message = "流ID不能为空") @RequestParam String streamId) {
        videoPlaybackService.restore(streamId);
        return R.ok();
    }

    /**
     * 回放拖动播放
     *
     * @param streamId 流ID
     * @param seekTime 拖动偏移量，单位秒
     * @return 操作结果
     */
    @SaCheckPermission("video:playback:seek")
    @Log(title = "视频回放", businessType = BusinessType.OTHER)
    @GetMapping("/seek")
    public R<Void> seek(@NotBlank(message = "流ID不能为空") @RequestParam String streamId,
                        @RequestParam(defaultValue = "0") Long seekTime) {
        videoPlaybackService.seek(streamId, seekTime);
        return R.ok();
    }

    /**
     * 回放倍速播放
     *
     * @param streamId 流ID
     * @param speed    倍速，仅支持 0.25、0.5、1、2、4
     * @return 操作结果
     */
    @SaCheckPermission("video:playback:speed")
    @Log(title = "视频回放", businessType = BusinessType.OTHER)
    @GetMapping("/speed")
    public R<Void> speed(@NotBlank(message = "流ID不能为空") @RequestParam String streamId,
                         @RequestParam(defaultValue = "0") Double speed) {
        videoPlaybackService.speed(streamId, speed);
        return R.ok();
    }
}
