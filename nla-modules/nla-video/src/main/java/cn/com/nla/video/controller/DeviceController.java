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
import cn.com.nla.video.domain.bo.DeviceBo;
import cn.com.nla.video.domain.vo.DeviceVo;
import cn.com.nla.video.service.IDeviceService;
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
 * 国标设备管理端点，设备注册信息由信令侧写入，此处仅提供人工维护与查询。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/device")
public class DeviceController extends BaseController {

    private final IDeviceService deviceService;

    /**
     * 查询设备列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 设备分页列表
     */
    @SaCheckPermission("video:device:list")
    @GetMapping("/list")
    public R<PageResult<DeviceVo>> list(DeviceBo bo, PageQuery pageQuery) {
        return R.ok(deviceService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取设备详细信息
     *
     * @param id 主键
     * @return 设备详情
     */
    @SaCheckPermission("video:device:query")
    @GetMapping("/{id}")
    public R<DeviceVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(deviceService.queryById(id));
    }

    /**
     * 新增设备
     *
     * @param bo 设备信息
     * @return 操作结果
     */
    @SaCheckPermission("video:device:add")
    @Log(title = "国标设备", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DeviceBo bo) {
        return toAjax(deviceService.insertByBo(bo));
    }

    /**
     * 修改设备
     *
     * @param bo 设备信息
     * @return 操作结果
     */
    @SaCheckPermission("video:device:edit")
    @Log(title = "国标设备", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DeviceBo bo) {
        return toAjax(deviceService.updateByBo(bo));
    }

    /**
     * 删除设备，存在关联通道时拒绝删除
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:device:remove")
    @Log(title = "国标设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(deviceService.deleteWithValidByIds(List.of(ids), true));
    }
}
