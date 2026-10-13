package cn.com.nla.callcenter.controller;

import cn.com.nla.callcenter.domain.bo.GateWayBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.GateWayVo;
import cn.com.nla.callcenter.service.IGateWayService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FS 注册网关维护端点, 密码仅入库不回显。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fs/gateway")
public class FsGatewayController extends BaseController {

    private final IGateWayService gateWayService;

    /**
     * 查询网关列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 网关分页列表
     */
    @SaCheckPermission("fs:gateway:list")
    @GetMapping("/list")
    public R<PageResult<GateWayVo>> list(GateWayBo bo, PageQuery pageQuery) {
        return R.ok(gateWayService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取网关详细信息
     *
     * @param id 网关编号
     * @return 网关详情
     */
    @SaCheckPermission("fs:gateway:query")
    @GetMapping("/{id}")
    public R<GateWayVo> getInfo(@NotNull(message = "网关编号不能为空") @PathVariable Long id) {
        return R.ok(gateWayService.queryById(id));
    }

    /**
     * 新增网关
     *
     * @param bo 网关信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:gateway:add")
    @Log(title = "FS注册网关", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody GateWayBo bo) {
        return toAjax(gateWayService.insertByBo(bo));
    }

    /**
     * 修改网关, 密码留空时保持原值
     *
     * @param bo 网关信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:gateway:edit")
    @Log(title = "FS注册网关", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody GateWayBo bo) {
        return toAjax(gateWayService.updateByBo(bo));
    }

    /**
     * 切换网关选中标记
     *
     * @param bo 仅使用 id 与 selected 字段
     * @return 操作结果
     */
    @SaCheckPermission("fs:gateway:edit")
    @Log(title = "FS注册网关", businessType = BusinessType.UPDATE)
    @PutMapping("/changeSelected")
    public R<Void> changeSelected(@Validated(EditGroup.class) @RequestBody GateWayBo bo) {
        return toAjax(gateWayService.updateSelected(bo));
    }

    /**
     * 删除网关
     *
     * @param ids 网关编号串
     * @return 操作结果
     */
    @SaCheckPermission("fs:gateway:remove")
    @Log(title = "FS注册网关", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "网关编号不能为空") @PathVariable Long[] ids) {
        return toAjax(gateWayService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 查询网关下拉选项
     *
     * @param keyword 名称模糊关键词
     * @param limit   返回条数上限
     * @return 网关选项列表
     */
    @SaCheckPermission("fs:gateway:list")
    @GetMapping("/optionselect")
    public R<List<FsOptionVo>> optionselect(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer limit) {
        return R.ok(gateWayService.queryOptionList(keyword, limit));
    }

    /** ??FS?????? */
    @SaCheckPermission("fs:gateway:export")
    @Log(title = "FS????", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(GateWayBo bo, HttpServletResponse response) {
        List<GateWayVo> list = gateWayService.queryList(bo);
        ExcelBuilder.of(list, GateWayVo.class).sheetName("FS注册网关").toResponse(response);
    }

}
