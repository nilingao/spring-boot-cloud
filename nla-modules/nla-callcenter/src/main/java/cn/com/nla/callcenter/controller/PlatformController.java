package cn.com.nla.callcenter.controller;

import cn.com.nla.callcenter.domain.bo.PlatformBo;
import cn.com.nla.callcenter.domain.vo.PlatformVo;
import cn.com.nla.callcenter.service.IPlatformService;
import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.redis.annotation.RepeatSubmit;
import cn.com.nla.common.web.core.BaseController;
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
 * FreeSWITCH 平台信息维护端点, 在线状态由协议侧心跳维护, 修改不覆盖运行时字段。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fs/platform")
public class PlatformController extends BaseController {

    private final IPlatformService platformService;

    /**
     * 查询平台列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 平台分页列表
     */
    @SaCheckPermission("fs:platform:list")
    @GetMapping("/list")
    public R<PageResult<PlatformVo>> list(PlatformBo bo, PageQuery pageQuery) {
        return R.ok(platformService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取平台详细信息
     *
     * @param id 平台编号
     * @return 平台详情
     */
    @SaCheckPermission("fs:platform:query")
    @GetMapping("/{id}")
    public R<PlatformVo> getInfo(@NotNull(message = "平台编号不能为空") @PathVariable Long id) {
        return R.ok(platformService.queryById(id));
    }

    /**
     * 新增平台, 校验 RTP 端口区间与名称唯一
     *
     * @param bo 平台信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:platform:add")
    @Log(title = "FS平台", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PlatformBo bo) {
        return toAjax(platformService.insertByBo(bo));
    }

    /**
     * 修改平台, 保留运行时在线状态
     *
     * @param bo 平台信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:platform:edit")
    @Log(title = "FS平台", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PlatformBo bo) {
        return toAjax(platformService.updateByBo(bo));
    }

    /**
     * 切换平台启用标记(仅更新 enable 字段)
     *
     * @param bo 仅使用 id 与 status 字段(写入 enable)
     * @return 操作结果
     */
    @SaCheckPermission("fs:platform:edit")
    @Log(title = "FS平台", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@Validated(EditGroup.class) @RequestBody PlatformBo bo) {
        return toAjax(platformService.updateStatus(bo));
    }

    /**
     * 删除平台, 启用中的平台要求先禁用
     *
     * @param ids 平台编号串
     * @return 操作结果
     */
    @SaCheckPermission("fs:platform:remove")
    @Log(title = "FS平台", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "平台编号不能为空") @PathVariable Long[] ids) {
        return toAjax(platformService.deleteWithValidByIds(List.of(ids), true));
    }

}
