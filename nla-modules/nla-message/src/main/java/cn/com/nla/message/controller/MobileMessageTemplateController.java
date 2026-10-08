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
import cn.com.nla.message.domain.bo.MobileMessageTemplateBo;
import cn.com.nla.message.domain.vo.MobileMessageTemplateVo;
import cn.com.nla.message.service.IMobileMessageTemplateService;
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
 * 短信模板
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms/template")
public class MobileMessageTemplateController extends BaseController {

    private final IMobileMessageTemplateService templateService;

    /**
     * 查询短信模板列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信模板分页结果
     */
    @SaCheckPermission("sms:template:list")
    @GetMapping("/list")
    public R<PageResult<MobileMessageTemplateVo>> list(MobileMessageTemplateBo bo, PageQuery pageQuery) {
        return R.ok(templateService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出短信模板列表
     *
     * @param bo       查询条件
     * @param response HTTP 响应
     */
    @SaCheckPermission("sms:template:export")
    @Log(title = "短信模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(MobileMessageTemplateBo bo, HttpServletResponse response) {
        List<MobileMessageTemplateVo> list = templateService.queryList(bo);
        ExcelBuilder.of(list, MobileMessageTemplateVo.class).sheetName("短信模板").toResponse(response);
    }

    /**
     * 获取短信模板详细信息
     *
     * @param id 主键
     * @return 短信模板详情
     */
    @SaCheckPermission("sms:template:query")
    @GetMapping("/{id}")
    public R<MobileMessageTemplateVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(templateService.queryById(id));
    }

    /**
     * 新增短信模板
     *
     * @param bo 短信模板
     * @return 操作结果
     */
    @SaCheckPermission("sms:template:add")
    @Log(title = "短信模板", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody MobileMessageTemplateBo bo) {
        return toAjax(templateService.insertByBo(bo));
    }

    /**
     * 修改短信模板
     *
     * @param bo 短信模板
     * @return 操作结果
     */
    @SaCheckPermission("sms:template:edit")
    @Log(title = "短信模板", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody MobileMessageTemplateBo bo) {
        return toAjax(templateService.updateByBo(bo));
    }

    /**
     * 删除短信模板
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("sms:template:remove")
    @Log(title = "短信模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(templateService.deleteWithValidByIds(List.of(ids), true));
    }

}
