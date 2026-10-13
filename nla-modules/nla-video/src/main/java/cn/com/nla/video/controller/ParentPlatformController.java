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
import cn.com.nla.video.domain.bo.ParentPlatformBo;
import cn.com.nla.video.domain.vo.ParentPlatformVo;
import cn.com.nla.video.service.IParentPlatformService;
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
 * 国标上级平台维护端点，平台注册与目录订阅等信令操作由协议接口提供。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/parentPlatform")
public class ParentPlatformController extends BaseController {

    private final IParentPlatformService parentPlatformService;

    /**
     * 查询上级平台列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 上级平台分页列表
     */
    @SaCheckPermission("video:parentPlatform:list")
    @GetMapping("/list")
    public R<PageResult<ParentPlatformVo>> list(ParentPlatformBo bo, PageQuery pageQuery) {
        return R.ok(parentPlatformService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取上级平台详细信息
     *
     * @param id 主键
     * @return 上级平台详情
     */
    @SaCheckPermission("video:parentPlatform:query")
    @GetMapping("/{id}")
    public R<ParentPlatformVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(parentPlatformService.queryById(id));
    }

    /**
     * 新增上级平台
     *
     * @param bo 上级平台信息
     * @return 操作结果
     */
    @SaCheckPermission("video:parentPlatform:add")
    @Log(title = "国标上级平台", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ParentPlatformBo bo) {
        return toAjax(parentPlatformService.insertByBo(bo));
    }

    /**
     * 修改上级平台
     *
     * @param bo 上级平台信息
     * @return 操作结果
     */
    @SaCheckPermission("video:parentPlatform:edit")
    @Log(title = "国标上级平台", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ParentPlatformBo bo) {
        return toAjax(parentPlatformService.updateByBo(bo));
    }

    /**
     * 删除上级平台，存在共享通道或共享流时拒绝删除
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:parentPlatform:remove")
    @Log(title = "国标上级平台", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(parentPlatformService.deleteWithValidByIds(List.of(ids), true));
    }
    /** ???????? */
    @SaCheckPermission("video:parentPlatform:export")
    @Log(title = "????", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ParentPlatformBo bo, HttpServletResponse response) {
        List<ParentPlatformVo> list = parentPlatformService.queryList(bo);
        ExcelBuilder.of(list, ParentPlatformVo.class).sheetName("????").toResponse(response);
    }

}
