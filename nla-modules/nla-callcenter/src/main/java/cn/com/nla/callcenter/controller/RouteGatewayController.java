package cn.com.nla.callcenter.controller;

import cn.com.nla.callcenter.domain.bo.RouteGatewayBo;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import cn.com.nla.callcenter.domain.vo.RouteGatewayVo;
import cn.com.nla.callcenter.service.IRouteGatewayService;
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
 * 路由网关(媒体网关)维护端点, 被路由组引用时拒绝删除, 变更后联动企业配置刷新。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fs/routeGateway")
public class RouteGatewayController extends BaseController {

    private final IRouteGatewayService routeGatewayService;

    /**
     * 查询路由网关列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 路由网关分页列表
     */
    @SaCheckPermission("fs:routeGateway:list")
    @GetMapping("/list")
    public R<PageResult<RouteGatewayVo>> list(RouteGatewayBo bo, PageQuery pageQuery) {
        return R.ok(routeGatewayService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取路由网关详细信息
     *
     * @param id 路由网关编号
     * @return 路由网关详情
     */
    @SaCheckPermission("fs:routeGateway:query")
    @GetMapping("/{id}")
    public R<RouteGatewayVo> getInfo(@NotNull(message = "路由网关编号不能为空") @PathVariable Long id) {
        return R.ok(routeGatewayService.queryById(id));
    }

    /**
     * 新增路由网关, 校验名称唯一
     *
     * @param bo 路由网关信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGateway:add")
    @Log(title = "路由网关", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody RouteGatewayBo bo) {
        return toAjax(routeGatewayService.insertByBo(bo));
    }

    /**
     * 修改路由网关, 校验名称唯一(排除自身)
     *
     * @param bo 路由网关信息
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGateway:edit")
    @Log(title = "路由网关", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody RouteGatewayBo bo) {
        return toAjax(routeGatewayService.updateByBo(bo));
    }

    /**
     * 切换路由网关状态
     *
     * @param bo 仅使用 id 与 status 字段
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGateway:edit")
    @Log(title = "路由网关", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@Validated(EditGroup.class) @RequestBody RouteGatewayBo bo) {
        return toAjax(routeGatewayService.updateStatus(bo));
    }

    /**
     * 删除路由网关, 被路由组引用时拒绝删除
     *
     * @param ids 路由网关编号串
     * @return 操作结果
     */
    @SaCheckPermission("fs:routeGateway:remove")
    @Log(title = "路由网关", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "路由网关编号不能为空") @PathVariable Long[] ids) {
        return toAjax(routeGatewayService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 查询路由网关下拉选项
     *
     * @param keyword 名称模糊关键词
     * @param limit   返回条数上限
     * @return 路由网关选项列表
     */
    @SaCheckPermission("fs:routeGateway:list")
    @GetMapping("/optionselect")
    public R<List<FsOptionVo>> optionselect(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer limit) {
        return R.ok(routeGatewayService.queryOptionList(keyword, limit));
    }

    /** ???????? */
    @SaCheckPermission("fs:routeGateway:export")
    @Log(title = "????", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(RouteGatewayBo bo, HttpServletResponse response) {
        List<RouteGatewayVo> list = routeGatewayService.queryList(bo);
        ExcelBuilder.of(list, RouteGatewayVo.class).sheetName("路由网关").toResponse(response);
    }

}
