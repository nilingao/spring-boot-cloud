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
import cn.com.nla.video.domain.bo.StreamPushBo;
import cn.com.nla.video.domain.vo.StreamPushVo;
import cn.com.nla.video.service.IStreamPushService;
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
 * 推流信息管理端点，记录第三方设备推流的配置与流状态。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/streamPush")
public class StreamPushController extends BaseController {

    private final IStreamPushService streamPushService;

    /**
     * 查询推流信息列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 推流分页列表
     */
    @SaCheckPermission("video:streamPush:list")
    @GetMapping("/list")
    public R<PageResult<StreamPushVo>> list(StreamPushBo bo, PageQuery pageQuery) {
        return R.ok(streamPushService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取推流信息详细信息
     *
     * @param id 主键
     * @return 推流详情
     */
    @SaCheckPermission("video:streamPush:query")
    @GetMapping("/{id}")
    public R<StreamPushVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(streamPushService.queryById(id));
    }

    /**
     * 新增推流信息
     *
     * @param bo 推流信息
     * @return 操作结果
     */
    @SaCheckPermission("video:streamPush:add")
    @Log(title = "推流信息", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody StreamPushBo bo) {
        return toAjax(streamPushService.insertByBo(bo));
    }

    /**
     * 修改推流信息
     *
     * @param bo 推流信息
     * @return 操作结果
     */
    @SaCheckPermission("video:streamPush:edit")
    @Log(title = "推流信息", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody StreamPushBo bo) {
        return toAjax(streamPushService.updateByBo(bo));
    }

    /**
     * 删除推流信息，正在推送的流需先由协议接口停止
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("video:streamPush:remove")
    @Log(title = "推流信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(streamPushService.deleteWithValidByIds(List.of(ids), true));
    }
    /** 导出推流配置列表 */
    @SaCheckPermission("video:streamPush:export")
    @Log(title = "推流配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(StreamPushBo bo, HttpServletResponse response) {
        List<StreamPushVo> list = streamPushService.queryList(bo);
        ExcelBuilder.of(list, StreamPushVo.class).sheetName("推流配置").toResponse(response);
    }

}
