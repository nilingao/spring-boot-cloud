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
import cn.com.nla.video.domain.bo.DeviceChannelBo;
import cn.com.nla.video.domain.vo.DeviceChannelVo;
import cn.com.nla.video.service.IDeviceChannelService;
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
 * 国标设备通道管理端点，通道数据由设备目录订阅同步写入。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/deviceChannel")
public class DeviceChannelController extends BaseController {

    private final IDeviceChannelService deviceChannelService;

    /**
     * 查询设备通道列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 通道分页列表
     */
    @SaCheckPermission("video:deviceChannel:list")
    @GetMapping("/list")
    public R<PageResult<DeviceChannelVo>> list(DeviceChannelBo bo, PageQuery pageQuery) {
        return R.ok(deviceChannelService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取设备通道详细信息
     *
     * @param id 主键
     * @return 通道详情
     */
    @SaCheckPermission("video:deviceChannel:query")
    @GetMapping("/{id}")
    public R<DeviceChannelVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(deviceChannelService.queryById(id));
    }

    /**
     * 新增设备通道
     *
     * @param bo 通道信息
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceChannel:add")
    @Log(title = "设备通道", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody DeviceChannelBo bo) {
        return toAjax(deviceChannelService.insertByBo(bo));
    }

    /**
     * 修改设备通道
     *
     * @param bo 通道信息
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceChannel:edit")
    @Log(title = "设备通道", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody DeviceChannelBo bo) {
        return toAjax(deviceChannelService.updateByBo(bo));
    }

    /**
     * 删除指定设备下的全部通道
     *
     * @param deviceId 设备国标编号
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceChannel:remove")
    @Log(title = "设备通道", businessType = BusinessType.DELETE)
    @DeleteMapping("/deviceId/{deviceId}")
    public R<Void> removeByDeviceId(@NotNull(message = "设备国标编号不能为空") @PathVariable String deviceId) {
        return toAjax(deviceChannelService.deleteByDeviceId(deviceId));
    }

    /**
     * 删除设备通道
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceChannel:remove")
    @Log(title = "设备通道", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(deviceChannelService.deleteWithValidByIds(List.of(ids), true));
    }
    /** 导出设备通道列表 */
    @SaCheckPermission("video:deviceChannel:export")
    @Log(title = "设备通道", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(DeviceChannelBo bo, HttpServletResponse response) {
        List<DeviceChannelVo> list = deviceChannelService.queryList(bo);
        ExcelBuilder.of(list, DeviceChannelVo.class).sheetName("设备通道").toResponse(response);
    }

}
