package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoConvertVo;
import cn.com.nla.video.service.IVideoPlayService;
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
 * 实时视频点播端点。
 *
 * <p>点播与停止为异步指令，接口只返回受理凭据 {@code requestId}，收流结果经 SSE
 * 消息通道推送；前端需在 {@code /resource/message} 监听 {@code video-command} 类型。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/play")
public class VideoPlayController extends BaseController {

    private final IVideoPlayService videoPlayService;

    /**
     * 开始点播
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:play:start")
    @Log(title = "视频点播", businessType = BusinessType.OTHER)
    @GetMapping("/start")
    public R<VideoCommandTicketVo> start(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                         @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoPlayService.play(deviceId, channelId));
    }

    /**
     * 停止点播
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:play:stop")
    @Log(title = "视频点播", businessType = BusinessType.OTHER)
    @GetMapping("/stop")
    public R<VideoCommandTicketVo> stop(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                        @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoPlayService.stop(deviceId, channelId));
    }

    /**
     * 视频流转码
     *
     * @param streamId 视频流ID
     * @return 转码结果，含结束转码所需的进程句柄
     */
    @SaCheckPermission("video:play:convert")
    @GetMapping("/convert")
    public R<VideoConvertVo> convert(@NotBlank(message = "视频流ID不能为空") @RequestParam String streamId) {
        return R.ok(videoPlayService.convert(streamId));
    }

    /**
     * 结束视频流转码
     *
     * @param key           转码进程句柄
     * @param mediaServerId 流媒体服务ID
     * @return 操作结果
     */
    @SaCheckPermission("video:play:convert")
    @Log(title = "视频转码", businessType = BusinessType.OTHER)
    @GetMapping("/convert-stop")
    public R<Void> convertStop(@NotBlank(message = "转码进程句柄不能为空") @RequestParam String key,
                               @NotBlank(message = "流媒体服务ID不能为空") @RequestParam String mediaServerId) {
        videoPlayService.convertStop(key, mediaServerId);
        return R.ok();
    }
}
