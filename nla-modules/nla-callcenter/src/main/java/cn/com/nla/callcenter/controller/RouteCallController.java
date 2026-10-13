package cn.com.nla.callcenter.controller;

import cn.com.nla.callcenter.domain.bo.RouteCallBo;
import cn.com.nla.callcenter.domain.vo.RouteCallVo;
import cn.com.nla.callcenter.service.IRouteCallService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 外呼路由(字冠路由)维护端点, 变更后联动企业配置刷新。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fs/routeCall")
public class RouteCallController extends BaseController {

    private final IRouteCallService routeCallService;

    /**
     * 查询外呼路由列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 外呼路由分页列表
     */
    @SaCheckPermission("fs:routeCall:list")
    @GetMapping("/list")
    public R<PageResult<RouteCallVo>> list(RouteCallBo bo, PageQuery pageQuery) {
        return R.ok(routeCallService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取外呼路由详细信息
     *
     * @param id 外呼路由编号
     * @return 外呼路由详情
     */
    @SaCheckPermission("fs:routeCall:query")
    @GetMapping("/{id}")
    public R<RouteCallVo> getInfo(@NotNull(message = "外呼路由编号不能为空") @PathVariable Long id) {
        return R.ok(routeCallService.queryById(id));
    }

    /**
     * 新增外呼路由, 校验号码区间与同企业字冠唯一
     *
     * @param bo 外呼路由信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeCall:add")
    @Log(title = "外呼路由", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody RouteCallBo bo) {
        return toAjax(routeCallService.insertByBo(bo));
    }

    /**
     * 修改外呼路由, 校验号码区间与同企业字冠唯一(排除自身)
     *
     * @param bo 外呼路由信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeCall:edit")
    @Log(title = "外呼路由", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody RouteCallBo bo) {
        return toAjax(routeCallService.updateByBo(bo));
    }

    /**
     * 切换外呼路由状态
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeCall:edit")
    @Log(title = "外呼路由", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@Validated(EditGroup.class) @RequestBody RouteCallBo bo) {
        return toAjax(routeCallService.updateStatus(bo));
    }

    /**
     * 删除外呼路由
     *
     * @param ids 外呼路由编号串
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeCall:remove")
    @Log(title = "外呼路由", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "外呼路由编号不能为空") @PathVariable Long[] ids) {
        return toAjax(routeCallService.deleteWithValidByIds(List.of(ids), true));
    }

    /** ???????? */
    @SaCheckPermission("fs:routeCall:export")
    @Log(title = "????", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(RouteCallBo bo, HttpServletResponse response) {
        List<RouteCallVo> list = routeCallService.queryList(bo);
        ExcelBuilder.of(list, RouteCallVo.class).sheetName("????").toResponse(response);
    }

}
