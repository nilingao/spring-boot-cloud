package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IVideoAudioService;
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
 * 语音对讲与语音广播端点。
 *
 * <p>推流地址、推流状态、关闭推流同步返回；语音广播需要设备应答，只返回受理凭据 {@code requestId}，
 * 结果经 SSE 消息通道推送。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/audio")
public class VideoAudioController extends BaseController {

    private final IVideoAudioService videoAudioService;

    /**
     * 获取语音对讲推流地址
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return WebRTC 推流地址
     */
    @SaCheckPermission("video:audio:push")
    @Log(title = "语音对讲", businessType = BusinessType.OTHER)
    @GetMapping("/push-path")
    public R<String> pushPath(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                              @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoAudioService.findPushPath(deviceId, channelId));
    }

    /**
     * 获取语音对讲推流状态
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 是否正在推流
     */
    @SaCheckPermission("video:audio:push")
    @GetMapping("/push-status")
    public R<Boolean> pushStatus(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                 @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoAudioService.isPushing(deviceId, channelId));
    }

    /**
     * 停止语音对讲推流
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 操作结果
     */
    @SaCheckPermission("video:audio:push")
    @Log(title = "语音对讲", businessType = BusinessType.OTHER)
    @GetMapping("/push-stop")
    public R<Void> pushStop(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                            @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        videoAudioService.stopPush(deviceId, channelId);
        return R.ok();
    }

    /**
     * 语音广播命令
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:audio:broadcast")
    @Log(title = "语音广播", businessType = BusinessType.OTHER)
    @GetMapping("/broadcast")
    public R<VideoCommandTicketVo> broadcast(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                             @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoAudioService.broadcast(deviceId, channelId));
    }
}
