package cn.com.nla.message.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.excel.utils.ExcelBuilder;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.redis.annotation.RepeatSubmit;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.message.domain.bo.SmsConfigBo;
import cn.com.nla.message.domain.vo.SmsConfigVo;
import cn.com.nla.message.service.ISmsConfigService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
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
 * 短信渠道配置
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms/config")
public class SmsConfigController extends BaseController {

    private final ISmsConfigService configService;

    /**
     * 查询短信渠道配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信渠道配置分页结果
     */
    @SaCheckPermission("sms:config:list")
    @GetMapping("/list")
    public R<PageResult<SmsConfigVo>> list(SmsConfigBo bo, PageQuery pageQuery) {
        return R.ok(configService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出短信渠道配置列表
     *
     * @param bo       查询条件
     * @param response HTTP 响应
     */
    @SaCheckPermission("sms:config:export")
    @Log(title = "短信渠道配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SmsConfigBo bo, HttpServletResponse response) {
        List<SmsConfigVo> list = configService.queryList(bo);
        ExcelBuilder.of(list, SmsConfigVo.class).sheetName("短信渠道配置").toResponse(response);
    }

    /**
     * 获取短信渠道配置详细信息
     *
     * @param id 主键
     * @return 短信渠道配置详情
     */
    @SaCheckPermission("sms:config:query")
    @GetMapping("/{id}")
    public R<SmsConfigVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(configService.queryById(id));
    }

    /**
     * 新增短信渠道配置
     *
     * @param bo 短信渠道配置
     * @return 操作结果
     */
    @SaCheckPermission("sms:config:add")
    @Log(title = "短信渠道配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SmsConfigBo bo) {
        return toAjax(configService.insertByBo(bo));
    }

    /**
     * 修改短信渠道配置
     *
     * @param bo 短信渠道配置
     * @return 操作结果
     */
    @SaCheckPermission("sms:config:edit")
    @Log(title = "短信渠道配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SmsConfigBo bo) {
        return toAjax(configService.updateByBo(bo));
    }

    /**
     * 删除短信渠道配置
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("sms:config:remove")
    @Log(title = "短信渠道配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(configService.deleteWithValidByIds(List.of(ids), true));
    }

}
