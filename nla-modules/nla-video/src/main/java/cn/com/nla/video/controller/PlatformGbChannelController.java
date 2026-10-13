package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.excel.utils.ExcelBuilder;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.redis.annotation.RepeatSubmit;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.bo.PlatformGbChannelBo;
import cn.com.nla.video.domain.vo.PlatformGbChannelVo;
import cn.com.nla.video.service.IPlatformGbChannelService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.servlet.http.HttpServletResponse;
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
 * 上级平台共享通道关系维护端点。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/platformGbChannel")
public class PlatformGbChannelController extends BaseController {

    private final IPlatformGbChannelService platformGbChannelService;

    /**
     * 查询共享通道关系列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 共享关系分页列表
     */
    @SaCheckPermission("video:platformGbChannel:list")
    @GetMapping("/list")
    public R<PageResult<PlatformGbChannelVo>> list(PlatformGbChannelBo bo, PageQuery pageQuery) {
        return R.ok(platformGbChannelService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取共享通道关系详细信息
     *
     * @param id 主键
     * @return 共享关系详情
     */
    @SaCheckPermission("video:platformGbChannel:query")
    @GetMapping("/{id}")
    public R<PlatformGbChannelVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(platformGbChannelService.queryById(id));
    }

    /**
     * 新增共享通道关系
     *
     * @param bo 共享关系信息
     * @return 操作结果
     */
    @SaCheckPermission("video:platformGbChannel:add")
    @Log(title = "平台共享通道", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PlatformGbChannelBo bo) {
        return toAjax(platformGbChannelService.insertByBo(bo));
    }

    /**
     * 修改共享通道关系
     *
     * @param bo 共享关系信息
     * @return 操作结果
     */
    @SaCheckPermission("video:platformGbChannel:edit")
    @Log(title = "平台共享通道", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PlatformGbChannelBo bo) {
        return toAjax(platformGbChannelService.updateByBo(bo));
    }

    /**
     * 删除共享通道关系
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:platformGbChannel:remove")
    @Log(title = "平台共享通道", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(platformGbChannelService.deleteWithValidByIds(List.of(ids), true));
    }
    /** ?????????? */
    @SaCheckPermission("video:platformGbChannel:export")
    @Log(title = "??????", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PlatformGbChannelBo bo, HttpServletResponse response) {
        List<PlatformGbChannelVo> list = platformGbChannelService.queryList(bo);
        ExcelBuilder.of(list, PlatformGbChannelVo.class).sheetName("??????").toResponse(response);
    }

}
