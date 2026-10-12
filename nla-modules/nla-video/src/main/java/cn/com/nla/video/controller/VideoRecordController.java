package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.domain.vo.VideoDownloadTaskVo;
import cn.com.nla.video.service.IVideoRecordService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 国标录像端点。
 *
 * <p>录像目录查询与录像下载为异步指令，接口只返回受理凭据 {@code requestId}，
 * 结果经 SSE 消息通道推送；下载任务列表与清除只操作流媒体收流缓存，同步返回。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/record")
public class VideoRecordController extends BaseController {

    private final IVideoRecordService videoRecordService;

    /**
     * 查询设备录像目录
     *
     * @param deviceGbId 设备国标编号
     * @param channelId  通道国标编号
     * @param startTime  开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime    结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:record:list")
    @Log(title = "国标录像", businessType = BusinessType.OTHER)
    @GetMapping("/list")
    public R<VideoCommandTicketVo> list(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceGbId,
                                        @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                        @NotBlank(message = "开始时间不能为空") @RequestParam String startTime,
                                        @NotBlank(message = "结束时间不能为空") @RequestParam String endTime) {
        return R.ok(videoRecordService.query(deviceGbId, channelId, startTime, endTime));
    }

    /**
     * 开始下载录像
     *
     * @param deviceGbId    设备国标编号
     * @param channelId     通道国标编号
     * @param startTime     开始时间，格式 yyyy-MM-dd HH:mm:ss
     * @param endTime       结束时间，格式 yyyy-MM-dd HH:mm:ss
     * @param downloadSpeed 下载倍速
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:record:download")
    @Log(title = "国标录像", businessType = BusinessType.OTHER)
    @GetMapping("/download")
    public R<VideoCommandTicketVo> download(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceGbId,
                                            @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                            @NotBlank(message = "开始时间不能为空") @RequestParam String startTime,
                                            @NotBlank(message = "结束时间不能为空") @RequestParam String endTime,
                                            @RequestParam Integer downloadSpeed) {
        return R.ok(videoRecordService.download(deviceGbId, channelId, startTime, endTime, downloadSpeed));
    }

    /**
     * 停止下载录像
     *
     * @param deviceGbId 设备国标编号
     * @param channelId  通道国标编号
     * @param stream     流ID
     * @return 操作结果
     */
    @SaCheckPermission("video:record:download")
    @Log(title = "国标录像", businessType = BusinessType.OTHER)
    @GetMapping("/download/stop")
    public R<Void> stopDownload(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceGbId,
                                @NotBlank(message = "通道国标编号不能为空") @RequestParam String channelId,
                                @NotBlank(message = "流ID不能为空") @RequestParam String stream) {
        videoRecordService.stopDownload(deviceGbId, channelId, stream);
        return R.ok();
    }

    /**
     * 查询当前用户的录像下载任务
     *
     * @return 下载任务列表
     */
    @SaCheckPermission("video:record:download")
    @GetMapping("/download/task")
    public R<List<VideoDownloadTaskVo>> downloadTaskList() {
        return R.ok(videoRecordService.queryDownloadList());
    }

    /**
     * 清除当前用户的录像下载任务记录
     *
     * @param key 下载任务缓存键
     * @return 操作结果
     */
    @SaCheckPermission("video:record:download")
    @Log(title = "国标录像", businessType = BusinessType.DELETE)
    @DeleteMapping("/download/task")
    public R<Void> deleteDownload(@NotBlank(message = "下载任务缓存键不能为空") @RequestParam String key) {
        videoRecordService.deleteDownload(key);
        return R.ok();
    }
}
