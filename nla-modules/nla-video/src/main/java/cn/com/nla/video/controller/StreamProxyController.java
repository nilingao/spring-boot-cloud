package cn.com.nla.video.controller;

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
import cn.com.nla.video.domain.bo.StreamProxyBo;
import cn.com.nla.video.domain.vo.StreamProxyVo;
import cn.com.nla.video.service.IStreamProxyService;
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
 * 拉流代理配置维护端点，仅管理配置数据，向流媒体服务器下发代理由协议接口完成。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/streamProxy")
public class StreamProxyController extends BaseController {

    private final IStreamProxyService streamProxyService;

    /**
     * 查询拉流代理列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 拉流代理分页列表
     */
    @SaCheckPermission("video:streamProxy:list")
    @GetMapping("/list")
    public R<PageResult<StreamProxyVo>> list(StreamProxyBo bo, PageQuery pageQuery) {
        return R.ok(streamProxyService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取拉流代理详细信息
     *
     * @param id 主键
     * @return 拉流代理详情
     */
    @SaCheckPermission("video:streamProxy:query")
    @GetMapping("/{id}")
    public R<StreamProxyVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(streamProxyService.queryById(id));
    }

    /**
     * 新增拉流代理
     *
     * @param bo 拉流代理信息
     * @return 操作结果
     */
    @SaCheckPermission("video:streamProxy:add")
    @Log(title = "拉流代理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody StreamProxyBo bo) {
        return toAjax(streamProxyService.insertByBo(bo));
    }

    /**
     * 修改拉流代理
     *
     * @param bo 拉流代理信息
     * @return 操作结果
     */
    @SaCheckPermission("video:streamProxy:edit")
    @Log(title = "拉流代理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody StreamProxyBo bo) {
        return toAjax(streamProxyService.updateByBo(bo));
    }

    /**
     * 删除拉流代理
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:streamProxy:remove")
    @Log(title = "拉流代理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(streamProxyService.deleteWithValidByIds(List.of(ids), true));
    }
    /** 导出拉流代理列表 */
    @SaCheckPermission("video:streamProxy:export")
    @Log(title = "拉流代理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(StreamProxyBo bo, HttpServletResponse response) {
        List<StreamProxyVo> list = streamProxyService.queryList(bo);
        ExcelBuilder.of(list, StreamProxyVo.class).sheetName("拉流代理").toResponse(response);
    }

}
