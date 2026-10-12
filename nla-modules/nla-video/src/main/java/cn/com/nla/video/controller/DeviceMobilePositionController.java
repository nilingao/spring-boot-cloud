package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.bo.DeviceMobilePositionBo;
import cn.com.nla.video.domain.vo.DeviceMobilePositionVo;
import cn.com.nla.video.service.IDeviceMobilePositionService;
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
 * 设备移动位置轨迹查询端点，轨迹由位置订阅上报写入，管理端只提供查询与清理。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/deviceMobilePosition")
public class DeviceMobilePositionController extends BaseController {

    private final IDeviceMobilePositionService deviceMobilePositionService;

    /**
     * 查询移动位置轨迹列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 轨迹分页列表
     */
    @SaCheckPermission("video:deviceMobilePosition:list")
    @GetMapping("/list")
    public R<PageResult<DeviceMobilePositionVo>> list(DeviceMobilePositionBo bo, PageQuery pageQuery) {
        return R.ok(deviceMobilePositionService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取移动位置轨迹详细信息
     *
     * @param id 主键
     * @return 轨迹详情
     */
    @SaCheckPermission("video:deviceMobilePosition:query")
    @GetMapping("/{id}")
    public R<DeviceMobilePositionVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(deviceMobilePositionService.queryById(id));
    }

    /**
     * 删除移动位置轨迹记录
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:deviceMobilePosition:remove")
    @Log(title = "设备移动位置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(deviceMobilePositionService.deleteWithValidByIds(List.of(ids), true));
    }
}
