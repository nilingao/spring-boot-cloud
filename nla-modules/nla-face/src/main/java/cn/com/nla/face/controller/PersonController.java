package cn.com.nla.face.controller;

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
import cn.com.nla.face.domain.bo.PersonBo;
import cn.com.nla.face.domain.vo.PersonVo;
import cn.com.nla.face.service.IPersonService;
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
 * 人员信息管理端，人脸特征由服务端按图片地址自动生成。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/face/person")
public class PersonController extends BaseController {

    private final IPersonService personService;

    /**
     * 查询人员列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 人员分页列表
     */
    @SaCheckPermission("face:person:list")
    @GetMapping("/list")
    public R<PageResult<PersonVo>> list(PersonBo bo, PageQuery pageQuery) {
        return R.ok(personService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出人员列表
     *
     * @param bo       查询条件
     * @param response HTTP 响应
     */
    @SaCheckPermission("face:person:export")
    @Log(title = "人员信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PersonBo bo, HttpServletResponse response) {
        List<PersonVo> list = personService.queryList(bo);
        ExcelBuilder.of(list, PersonVo.class).sheetName("人员信息").toResponse(response);
    }

    /**
     * 获取人员详细信息
     *
     * @param id 主键
     * @return 人员详情
     */
    @SaCheckPermission("face:person:query")
    @GetMapping("/{id}")
    public R<PersonVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(personService.queryById(id));
    }

    /**
     * 新增人员（自动提取人脸特征）
     *
     * @param bo 人员信息
     * @return 操作结果
     */
    @SaCheckPermission("face:person:add")
    @Log(title = "人员信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PersonBo bo) {
        return toAjax(personService.insertByBo(bo));
    }

    /**
     * 修改人员（图片地址变更时重新提取特征）
     *
     * @param bo 人员信息
     * @return 操作结果
     */
    @SaCheckPermission("face:person:edit")
    @Log(title = "人员信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PersonBo bo) {
        return toAjax(personService.updateByBo(bo));
    }

    /**
     * 删除人员并同步移除特征缓存
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("face:person:remove")
    @Log(title = "人员信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(personService.deleteWithValidByIds(List.of(ids), true));
    }
}
