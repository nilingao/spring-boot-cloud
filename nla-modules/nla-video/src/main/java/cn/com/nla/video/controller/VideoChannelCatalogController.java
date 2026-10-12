package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.video.basic.vo.sip.SyncStatus;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.service.IVideoChannelCatalogService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备通道目录端点：设备/通道树形目录与国标目录同步。
 *
 * <p>目录同步是异步信令过程，接口只负责发起，前端轮询同步进度直至完成或失败。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/channelCatalog")
public class VideoChannelCatalogController extends BaseController {

    private final IVideoChannelCatalogService videoChannelCatalogService;

    /**
     * 获取设备通道树形目录
     *
     * @return 树形目录
     */
    @SaCheckPermission("video:channelCatalog:query")
    @GetMapping("/tree")
    public R<List<Tree<String>>> tree() {
        return R.ok(videoChannelCatalogService.queryTree());
    }

    /**
     * 同步设备通道目录
     *
     * @param deviceId 设备国标编号
     * @return 发起结果
     */
    @SaCheckPermission("video:channelCatalog:sync")
    @Log(title = "通道目录同步", businessType = BusinessType.OTHER)
    @GetMapping("/sync")
    public R<Void> sync(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        videoChannelCatalogService.sync(deviceId);
        return R.ok("设备同步中", null);
    }

    /**
     * 获取设备通道目录同步进度
     *
     * @param deviceId 设备国标编号
     * @return 同步进度
     */
    @SaCheckPermission("video:channelCatalog:query")
    @GetMapping("/sync-status")
    public R<SyncStatus> syncStatus(@NotBlank(message = "设备国标编号不能为空") @RequestParam String deviceId) {
        return R.ok(videoChannelCatalogService.querySyncStatus(deviceId));
    }
}
