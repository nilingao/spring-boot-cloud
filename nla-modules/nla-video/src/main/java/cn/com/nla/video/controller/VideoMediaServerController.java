package cn.com.nla.video.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.validate.AddGroup;
import cn.com.nla.common.core.validate.EditGroup;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.redis.annotation.RepeatSubmit;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.video.domain.bo.VideoMediaServerBo;
import cn.com.nla.video.domain.vo.VideoMediaServerVo;
import cn.com.nla.video.service.IVideoMediaServerService;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
 * 流媒体服务器节点维护端点，节点在线状态由 ZLMediaKit 心跳回调刷新。
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/video/mediaServer")
public class VideoMediaServerController extends BaseController {

    private final IVideoMediaServerService videoMediaServerService;

    /**
     * 查询流媒体服务器列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 流媒体服务器分页列表
     */
    @SaCheckPermission("video:mediaServer:list")
    @GetMapping("/list")
    public R<PageResult<VideoMediaServerVo>> list(VideoMediaServerBo bo, PageQuery pageQuery) {
        return R.ok(videoMediaServerService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取流媒体服务器详细信息
     *
     * @param id 流媒体编号
     * @return 流媒体服务器详情
     */
    @SaCheckPermission("video:mediaServer:query")
    @GetMapping("/{id}")
    public R<VideoMediaServerVo> getInfo(@NotNull(message = "流媒体编号不能为空") @PathVariable String id) {
        return R.ok(videoMediaServerService.queryById(id));
    }

    /**
     * 新增流媒体服务器
     *
     * @param bo 流媒体服务器信息
     * @return 操作结果
     */
    @SaCheckPermission("video:mediaServer:add")
    @Log(title = "流媒体服务器", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody VideoMediaServerBo bo) {
        return toAjax(videoMediaServerService.insertByBo(bo));
    }

    /**
     * 修改流媒体服务器
     *
     * @param bo 流媒体服务器信息
     * @return 操作结果
     */
    @SaCheckPermission("video:mediaServer:edit")
    @Log(title = "流媒体服务器", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody VideoMediaServerBo bo) {
        return toAjax(videoMediaServerService.updateByBo(bo));
    }

    /**
     * 删除流媒体服务器，节点上仍存在流数据时拒绝删除
     *
     * @param ids 流媒体编号串
     * @return 操作结果
     */
    @SaCheckPermission("video:mediaServer:remove")
    @Log(title = "流媒体服务器", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "流媒体编号不能为空") @PathVariable String[] ids) {
        return toAjax(videoMediaServerService.deleteWithValidByIds(List.of(ids), true));
    }
}
