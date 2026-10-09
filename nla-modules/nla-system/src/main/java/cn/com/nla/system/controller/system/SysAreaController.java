package cn.com.nla.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import lombok.RequiredArgsConstructor;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.system.domain.bo.SysAreaBo;
import cn.com.nla.system.domain.vo.SysAreaVo;
import cn.com.nla.system.service.ISysAreaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政区划
 * <p>
 * 只读参考数据，提供列表、级联树、详情与地址拼接查询，无写入接口。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/area")
public class SysAreaController extends BaseController {

    private final ISysAreaService areaService;

    /**
     * 查询行政区划列表
     *
     * @param bo 查询条件
     * @return 行政区划列表
     */
    @SaCheckPermission("system:area:list")
    @GetMapping("/list")
    public R<List<SysAreaVo>> list(SysAreaBo bo) {
        return R.ok(areaService.selectAreaList(bo));
    }

    /**
     * 查询行政区划树（供前端级联选择）
     *
     * @param bo 查询条件
     * @return 行政区划树列表
     */
    @SaCheckPermission("system:area:list")
    @GetMapping("/tree")
    public R<List<Tree<Long>>> tree(SysAreaBo bo) {
        return R.ok(areaService.selectAreaTree(bo));
    }

    /**
     * 根据地区Id获取详细信息
     *
     * @param areaId 地区Id
     * @return 行政区划详情
     */
    @SaCheckPermission("system:area:query")
    @GetMapping("/{areaId}")
    public R<SysAreaVo> getInfo(@PathVariable Long areaId) {
        return R.ok(areaService.selectAreaById(areaId));
    }

    /**
     * 根据省/市/区地区Id拼接完整地址名称
     *
     * @param provinceId 省地区Id
     * @param cityId     市地区Id
     * @param areaId     区县地区Id
     * @return 拼接后的地址名称
     */
    @SaCheckPermission("system:area:query")
    @GetMapping("/address")
    public R<String> address(@RequestParam(required = false) Long provinceId,
                             @RequestParam(required = false) Long cityId,
                             @RequestParam(required = false) Long areaId) {
        return R.ok(areaService.findAddress(provinceId, cityId, areaId));
    }

}
