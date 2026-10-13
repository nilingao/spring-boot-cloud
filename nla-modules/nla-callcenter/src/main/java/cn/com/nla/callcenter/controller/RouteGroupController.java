package cn.com.nla.callcenter.controller;

import cn.com.nla.callcenter.domain.bo.RouteGroupBo;
import cn.com.nla.callcenter.domain.bo.RouteGroupGatewayEditBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.RouteGroupVo;
import cn.com.nla.callcenter.service.IRouteGroupService;
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
 * 路由组维护端点, 网关绑定以完整列表替换, 变更后联动企业配置刷新。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fs/routeGroup")
public class RouteGroupController extends BaseController {

    private final IRouteGroupService routeGroupService;

    /**
     * 查询路由组列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 路由组分页列表
     */
    @SaCheckPermission("fs:routeGroup:list")
    @GetMapping("/list")
    public R<PageResult<RouteGroupVo>> list(RouteGroupBo bo, PageQuery pageQuery) {
        return R.ok(routeGroupService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取路由组详细信息(含已绑定网关列表)
     *
     * @param id 路由组编号
     * @return 路由组详情
     */
    @SaCheckPermission("fs:routeGroup:query")
    @GetMapping("/{id}")
    public R<RouteGroupVo> getInfo(@NotNull(message = "路由组编号不能为空") @PathVariable Long id) {
        return R.ok(routeGroupService.queryById(id));
    }

    /**
     * 新增路由组
     *
     * @param bo 路由组信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGroup:add")
    @Log(title = "路由组", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody RouteGroupBo bo) {
        return toAjax(routeGroupService.insertByBo(bo));
    }

    /**
     * 修改路由组
     *
     * @param bo 路由组信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGroup:edit")
    @Log(title = "路由组", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody RouteGroupBo bo) {
        return toAjax(routeGroupService.updateByBo(bo));
    }

    /**
     * 替换路由组下的网关绑定, 空列表清空全部绑定
     *
     * @param bo 路由组编号与网关编号列表
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGroup:edit")
    @Log(title = "路由组网关绑定", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/gateways")
    public R<Void> updateGateways(@Validated @RequestBody RouteGroupGatewayEditBo bo) {
        return toAjax(routeGroupService.updateGateways(bo));
    }

    /**
     * 切换路由组状态
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGroup:edit")
    @Log(title = "路由组", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@Validated(EditGroup.class) @RequestBody RouteGroupBo bo) {
        return toAjax(routeGroupService.updateStatus(bo));
    }

    /**
     * 删除路由组, 被外呼路由引用时拒绝删除
     *
     * @param ids 路由组编号串
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGroup:remove")
    @Log(title = "路由组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "路由组编号不能为空") @PathVariable Long[] ids) {
        return toAjax(routeGroupService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 查询路由组下拉选项
     *
     * @param keyword 名称模糊关键词
     * @param limit   返回条数上限
     * @return 路由组选项列表
     */
    @SaCheckPermission("fs:routeGroup:list")
    @GetMapping("/optionselect")
    public R<List<FsOptionVo>> optionselect(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer limit) {
        return R.ok(routeGroupService.queryOptionList(keyword, limit));
    }

    /** ??????? */
    @SaCheckPermission("fs:routeGroup:export")
    @Log(title = "???", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(RouteGroupBo bo, HttpServletResponse response) {
        List<RouteGroupVo> list = routeGroupService.queryList(bo);
        ExcelBuilder.of(list, RouteGroupVo.class).sheetName("路由组").toResponse(response);
    }

}
