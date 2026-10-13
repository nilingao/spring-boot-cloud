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
import cn.com.nla.video.domain.bo.GbStreamBo;
import cn.com.nla.video.domain.vo.GbStreamVo;
import cn.com.nla.video.service.IGbStreamService;
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
 * 国标流信息维护端点，记录推流与拉流产生的流与媒体服务器关系。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/gbStream")
public class GbStreamController extends BaseController {

    private final IGbStreamService gbStreamService;

    /**
     * 查询国标流列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 国标流分页列表
     */
    @SaCheckPermission("video:gbStream:list")
    @GetMapping("/list")
    public R<PageResult<GbStreamVo>> list(GbStreamBo bo, PageQuery pageQuery) {
        return R.ok(gbStreamService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取国标流详细信息
     *
     * @param id 主键
     * @return 国标流详情
     */
    @SaCheckPermission("video:gbStream:query")
    @GetMapping("/{id}")
    public R<GbStreamVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(gbStreamService.queryById(id));
    }

    /**
     * 新增国标流
     *
     * @param bo 国标流信息
     * @return 操作结果
     */
    @SaCheckPermission("video:gbStream:add")
    @Log(title = "国标流信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody GbStreamBo bo) {
        return toAjax(gbStreamService.insertByBo(bo));
    }

    /**
     * 修改国标流
     *
     * @param bo 国标流信息
     * @return 操作结果
     */
    @SaCheckPermission("video:gbStream:edit")
    @Log(title = "国标流信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody GbStreamBo bo) {
        return toAjax(gbStreamService.updateByBo(bo));
    }

    /**
     * 删除国标流，存在平台共享关系时拒绝删除
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:gbStream:remove")
    @Log(title = "国标流信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(gbStreamService.deleteWithValidByIds(List.of(ids), true));
    }

    /** 按国标编号查询直播流，供协议侧反查使用。 */
    @SaCheckPermission("video:gbStream:query")
    @GetMapping("/by-gb-id/{gbId}")
    public R<GbStreamVo> getByGbId(@PathVariable String gbId) {
        return R.ok(gbStreamService.queryByGbId(gbId));
    }
    /** ??????? */
    @SaCheckPermission("video:gbStream:export")
    @Log(title = "???", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(GbStreamBo bo, HttpServletResponse response) {
        List<GbStreamVo> list = gbStreamService.queryList(bo);
        ExcelBuilder.of(list, GbStreamVo.class).sheetName("???").toResponse(response);
    }

}
