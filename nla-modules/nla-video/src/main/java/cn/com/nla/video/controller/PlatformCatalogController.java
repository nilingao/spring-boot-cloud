package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.redis.annotation.RepeatSubmit;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.bo.PlatformCatalogBo;
import cn.com.nla.video.domain.vo.PlatformCatalogVo;
import cn.com.nla.video.service.IPlatformCatalogService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台目录节点维护端点，目录由上级平台订阅同步写入。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/platformCatalog")
public class PlatformCatalogController extends BaseController {

    private final IPlatformCatalogService platformCatalogService;

    /**
     * 查询平台目录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 目录分页列表
     */
    @SaCheckPermission("video:platformCatalog:list")
    @GetMapping("/list")
    public R<PageResult<PlatformCatalogVo>> list(PlatformCatalogBo bo, PageQuery pageQuery) {
        return R.ok(platformCatalogService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取平台目录详细信息
     *
     * @param id 目录编号
     * @return 目录详情
     */
    @SaCheckPermission("video:platformCatalog:query")
    @GetMapping("/{id}")
    public R<PlatformCatalogVo> getInfo(@NotNull(message = "目录编号不能为空") @PathVariable String id) {
        return R.ok(platformCatalogService.queryById(id));
    }

    /**
     * 新增平台目录
     *
     * @param bo 目录信息
     * @return 操作结果
     */
    @SaCheckPermission("video:platformCatalog:add")
    @Log(title = "平台目录", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PlatformCatalogBo bo) {
        return toAjax(platformCatalogService.insertByBo(bo));
    }

    /**
     * 修改平台目录
     *
     * @param bo 目录信息
     * @return 操作结果
     */
    @SaCheckPermission("video:platformCatalog:edit")
    @Log(title = "平台目录", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PlatformCatalogBo bo) {
        return toAjax(platformCatalogService.updateByBo(bo));
    }

    /**
     * 删除平台目录，存在挂载通道时拒绝删除
     *
     * @param ids 目录编号串
     * @return 操作结果
     */
    @SaCheckPermission("video:platformCatalog:remove")
    @Log(title = "平台目录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "目录编号不能为空") @PathVariable String[] ids) {
        return toAjax(platformCatalogService.deleteWithValidByIds(List.of(ids), true));
    }
}
