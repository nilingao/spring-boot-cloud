package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.bo.DeviceAlarmBo;
import cn.com.nla.video.domain.vo.DeviceAlarmVo;
import cn.com.nla.video.service.IDeviceAlarmService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备报警记录查询端点，报警数据由信令侧上报写入，管理端只提供查询与清理。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/deviceAlarm")
public class DeviceAlarmController extends BaseController {

    private final IDeviceAlarmService deviceAlarmService;

    /**
     * 查询设备报警列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 报警分页列表
     */
    @SaCheckPermission("video:deviceAlarm:list")
    @GetMapping("/list")
    public R<PageResult<DeviceAlarmVo>> list(DeviceAlarmBo bo, PageQuery pageQuery) {
        return R.ok(deviceAlarmService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取设备报警详细信息
     *
     * @param id 主键
     * @return 报警详情
     */
    @SaCheckPermission("video:deviceAlarm:query")
    @GetMapping("/{id}")
    public R<DeviceAlarmVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(deviceAlarmService.queryById(id));
    }

    /**
     * 删除设备报警记录
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceAlarm:remove")
    @Log(title = "设备报警", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(deviceAlarmService.deleteWithValidByIds(List.of(ids), true));
    }
}
