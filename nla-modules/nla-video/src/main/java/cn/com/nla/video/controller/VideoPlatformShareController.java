package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.redis.annotation.RepeatSubmit;
import cn.com.nla.common.video.basic.vo.sip.Address;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.bo.VideoPlatformShareBo;
import cn.com.nla.video.domain.vo.GbStreamVo;
import cn.com.nla.video.domain.vo.VideoBindKeyVo;
import cn.com.nla.video.service.IVideoPlatformShareService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 上级平台级联共享端点：目录树、可共享通道/直播流与目录绑定关系维护。
 *
 * <p>这里维护的是共享关系，上级平台与目录节点本身的增删改查见对应配置表控制器。</p>
 *
 * <p>仅在 {@code video.enabled=true} 时注册，协议未开启时不暴露指令入口。</p>
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "video", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/video/platformShare")
public class VideoPlatformShareController extends BaseController {

    private final IVideoPlatformShareService videoPlatformShareService;

    /**
     * 查询注册到本级 SIP 服务的节点列表
     *
     * @return SIP 注册地址列表
     */
    @SaCheckPermission("video:platformShare:query")
    @GetMapping("/sip-list")
    public R<List<Address>> sipList() {
        return R.ok(videoPlatformShareService.querySipList());
    }

    /**
     * 查询可共享的设备通道树
     *
     * @return 设备通道树形目录
     */
    @SaCheckPermission("video:platformShare:query")
    @GetMapping("/channel-tree")
    public R<List<Tree<String>>> channelTree() {
        return R.ok(videoPlatformShareService.queryChannelTree());
    }

    /**
     * 查询上级平台目录树
     *
     * @param platformId 上级平台国标编号
     * @return 平台目录树形结构
     */
    @SaCheckPermission("video:platformShare:query")
    @GetMapping("/catalog-tree")
    public R<List<Tree<String>>> catalogTree(@NotBlank(message = "上级平台国标编号不能为空") @RequestParam String platformId) {
        return R.ok(videoPlatformShareService.queryCatalogTree(platformId));
    }

    /**
     * 查询可共享的国标直播流列表
     *
     * @return 国标直播流列表
     */
    @SaCheckPermission("video:platformShare:query")
    @GetMapping("/stream-list")
    public R<List<GbStreamVo>> streamList() {
        return R.ok(videoPlatformShareService.queryStreamList());
    }

    /**
     * 查询当前目录下已绑定的通道编号
     *
     * @param bo 平台、目录与范围条件
     * @return 已绑定编号集合
     */
    @SaCheckPermission("video:platformShare:query")
    @PostMapping("/channel-bind-key")
    public R<VideoBindKeyVo> channelBindKey(@Validated @RequestBody VideoPlatformShareBo bo) {
        return R.ok(videoPlatformShareService.queryChannelBindKey(bo));
    }

    /**
     * 查询当前目录下已绑定的直播流编号
     *
     * @param bo 平台、目录与范围条件
     * @return 已绑定编号集合
     */
    @SaCheckPermission("video:platformShare:query")
    @PostMapping("/stream-bind-key")
    public R<VideoBindKeyVo> streamBindKey(@Validated @RequestBody VideoPlatformShareBo bo) {
        return R.ok(videoPlatformShareService.queryStreamBindKey(bo));
    }

    /**
     * 将设备通道共享到上级平台目录
     *
     * @param bo 平台、目录与通道范围条件
     * @return 操作结果
     */
    @SaCheckPermission("video:platformShare:bind")
    @RepeatSubmit()
    @Log(title = "级联通道共享", businessType = BusinessType.INSERT)
    @PostMapping("/channel-bind")
    public R<Void> bindChannel(@Validated @RequestBody VideoPlatformShareBo bo) {
        videoPlatformShareService.bindChannel(bo);
        return R.ok("共享成功");
    }

    /**
     * 从上级平台目录移除设备通道共享
     *
     * @param bo 平台、目录与通道范围条件
     * @return 操作结果
     */
    @SaCheckPermission("video:platformShare:unbind")
    @Log(title = "级联通道共享", businessType = BusinessType.DELETE)
    @PostMapping("/channel-unbind")
    public R<Void> unbindChannel(@Validated @RequestBody VideoPlatformShareBo bo) {
        videoPlatformShareService.unbindChannel(bo);
        return R.ok("解除共享成功");
    }

    /**
     * 将国标直播流共享到上级平台目录
     *
     * @param bo 平台、目录与直播流范围条件
     * @return 操作结果
     */
    @SaCheckPermission("video:platformShare:bind")
    @RepeatSubmit()
    @Log(title = "级联流共享", businessType = BusinessType.INSERT)
    @PostMapping("/stream-bind")
    public R<Void> bindStream(@Validated @RequestBody VideoPlatformShareBo bo) {
        videoPlatformShareService.bindStream(bo);
        return R.ok("共享成功");
    }

    /**
     * 从上级平台目录移除国标直播流共享
     *
     * @param bo 平台、目录与直播流范围条件
     * @return 操作结果
     */
    @SaCheckPermission("video:platformShare:unbind")
    @Log(title = "级联流共享", businessType = BusinessType.DELETE)
    @PostMapping("/stream-unbind")
    public R<Void> unbindStream(@Validated @RequestBody VideoPlatformShareBo bo) {
        videoPlatformShareService.unbindStream(bo);
        return R.ok("解除共享成功");
    }

    /**
     * 解除目录下已共享的通道或直播流
     *
     * @param catalogId 目录国标编号，等于上级平台国标编号时为平台根目录
     * @param type      解除类型：0 全部，1 通道，2 直播流，为空按全部处理
     * @return 操作结果
     */
    @SaCheckPermission("video:platformShare:unbind")
    @Log(title = "级联目录共享", businessType = BusinessType.DELETE)
    @DeleteMapping("/catalog-relation")
    public R<Void> deleteCatalogRelation(@NotBlank(message = "目录国标编号不能为空") @RequestParam String catalogId,
                                         @RequestParam(required = false) Integer type) {
        videoPlatformShareService.deleteCatalogRelation(catalogId, type);
        return R.ok("解除共享成功");
    }
}
