package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.video.basic.vo.media.OnStreamChangedResult;
import cn.com.nla.common.video.core.demo.StreamInfo;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.service.IVideoStreamService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流媒体运行态端点：在线流地址查询、流信息查询、FFmpeg 模板与拉流代理启停。
 *
 * <p>这些接口读取的是流媒体节点的实时状态，与流任务配置表（拉流代理、推流）的增删改查互不影响。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/stream")
public class VideoStreamController extends BaseController {

    private final IVideoStreamService videoStreamService;

    /**
     * 获取设备通道当前点播的播放地址
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 各协议播放地址
     */
    @SaCheckPermission("video:stream:query")
    @GetMapping("/play-url")
    public R<StreamInfo> playUrl(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                 @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId) {
        return R.ok(videoStreamService.queryInviteStreamUrl(deviceId, channelId));
    }

    /**
     * 获取流信息
     *
     * @param app           流应用名
     * @param stream        流编号
     * @param mediaServerId 流媒体服务编号
     * @return 流媒体信息
     */
    @SaCheckPermission("video:stream:query")
    @GetMapping("/media-info")
    public R<OnStreamChangedResult> mediaInfo(@NotBlank(message = "流应用名不能为空") @RequestParam String app,
                                              @NotBlank(message = "流编号不能为空") @RequestParam String stream,
                                              @NotBlank(message = "流媒体服务编号不能为空") @RequestParam String mediaServerId) {
        return R.ok(videoStreamService.queryMediaInfo(app, stream, mediaServerId));
    }

    /**
     * 获取流媒体节点的 FFmpeg 拉流命令模板
     *
     * @param mediaServerId 流媒体服务编号
     * @return FFmpeg 命令模板
     */
    @SaCheckPermission("video:stream:query")
    @GetMapping("/ffmpeg-cmd")
    public R<String> ffmpegCmd(@NotBlank(message = "流媒体服务编号不能为空") @RequestParam String mediaServerId) {
        return R.ok(videoStreamService.queryFfmpegCmd(mediaServerId));
    }

    /**
     * 启用拉流代理
     *
     * @param id 拉流代理主键
     * @return 操作结果
     */
    @SaCheckPermission("video:stream:start")
    @Log(title = "拉流代理", businessType = BusinessType.UPDATE)
    @GetMapping("/proxy/start")
    public R<Void> startProxy(@NotNull(message = "拉流代理主键不能为空") @RequestParam Long id) {
        videoStreamService.startProxy(id);
        return R.ok("启用成功");
    }

    /**
     * 停用拉流代理
     *
     * @param id 拉流代理主键
     * @return 操作结果
     */
    @SaCheckPermission("video:stream:stop")
    @Log(title = "拉流代理", businessType = BusinessType.UPDATE)
    @GetMapping("/proxy/stop")
    public R<Void> stopProxy(@NotNull(message = "拉流代理主键不能为空") @RequestParam Long id) {
        videoStreamService.stopProxy(id);
        return R.ok("停用成功");
    }

    /**
     * 获取拉流代理播放地址
     *
     * @param id 拉流代理主键
     * @return 各协议播放地址
     */
    @SaCheckPermission("video:stream:query")
    @GetMapping("/proxy/play-url")
    public R<StreamInfo> proxyPlayUrl(@NotNull(message = "拉流代理主键不能为空") @RequestParam Long id) {
        return R.ok(videoStreamService.queryProxyStreamUrl(id));
    }

    /**
     * 获取推流播放地址
     *
     * @param id 推流主键
     * @return 各协议播放地址
     */
    @SaCheckPermission("video:stream:query")
    @GetMapping("/push/play-url")
    public R<StreamInfo> pushPlayUrl(@NotNull(message = "推流主键不能为空") @RequestParam Long id) {
        return R.ok(videoStreamService.queryPushStreamUrl(id));
    }
}
