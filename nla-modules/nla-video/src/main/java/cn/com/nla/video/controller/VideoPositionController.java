package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;
import cn.com.nla.video.domain.vo.VideoCommandTicketVo;
import cn.com.nla.video.service.IVideoPositionService;
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

import java.util.List;

/**
 * 设备移动位置端点。
 *
 * <p>历史轨迹与最新位置直接查库；实时位置需要设备应答，只返回受理凭据 {@code requestId}，
 * 结果经 SSE 消息通道推送；订阅与坐标换算同步返回。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/position")
public class VideoPositionController extends BaseController {

    private final IVideoPositionService videoPositionService;

    /**
     * 查询设备历史轨迹
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @param start     起始时间
     * @param end       结束时间
     * @return 轨迹列表
     */
    @SaCheckPermission("video:position:query")
    @GetMapping("/history")
    public R<List<DeviceMobilePositionVo>> history(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                                   @RequestParam(required = false) String channelId,
                                                   @RequestParam(required = false) String start,
                                                   @RequestParam(required = false) String end) {
        return R.ok(videoPositionService.queryHistory(deviceId, channelId, start, end));
    }

    /**
     * 查询设备最新位置
     *
     * @param deviceId 设备国标编号
     * @return 最新位置
     */
    @SaCheckPermission("video:position:query")
    @GetMapping("/latest")
    public R<DeviceMobilePositionVo> latest(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        return R.ok(videoPositionService.queryLatest(deviceId));
    }

    /**
     * 主动查询设备实时位置
     *
     * @param deviceId  设备国标编号
     * @param channelId 通道国标编号
     * @return 指令受理凭据
     */
    @SaCheckPermission("video:position:realtime")
    @Log(title = "设备位置", businessType = BusinessType.OTHER)
    @GetMapping("/realtime")
    public R<VideoCommandTicketVo> realtime(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                                            @RequestParam(required = false) String channelId) {
        return R.ok(videoPositionService.queryRealtime(deviceId, channelId));
    }

    /**
     * 订阅设备通知
     *
     * @param deviceId 设备国标编号
     * @param type     订阅类型：1 目录，2 移动位置，3 报警
     * @param expires  订阅周期，单位秒，0 表示取消订阅
     * @return 操作结果
     */
    @SaCheckPermission("video:position:subscribe")
    @Log(title = "设备位置", businessType = BusinessType.OTHER)
    @GetMapping("/subscribe")
    public R<Void> subscribe(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId,
                             @NotNull(message = "订阅类型不能为空") @RequestParam Integer type,
                             @NotNull(message = "订阅周期不能为空") @RequestParam Integer expires) {
        videoPositionService.subscribe(deviceId, type, expires);
        return R.ok();
    }

    /**
     * 通道坐标格式换算
     *
     * @param deviceId 设备国标编号
     * @return 操作结果
     */
    @SaCheckPermission("video:position:transform")
    @Log(title = "设备位置", businessType = BusinessType.UPDATE)
    @GetMapping("/transform")
    public R<Void> transform(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        videoPositionService.transform(deviceId);
        return R.ok();
    }
}
