package cn.com.nla.message.controller;

import cn.com.nla.common.core.domain.PageResult;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.excel.utils.ExcelBuilder;
import cn.com.nla.common.log.annotation.Log;
import cn.com.nla.common.log.enums.BusinessType;
import cn.com.nla.common.mybatis.core.page.PageQuery;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.message.domain.bo.MobileMessageBo;
import cn.com.nla.message.domain.vo.MobileMessageVo;
import cn.com.nla.message.service.IMobileMessageService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.lock.annotation.Lock4j;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 短信发送记录
 *
 * @author TZY
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms/record")
public class MobileMessageController extends BaseController {

    private final IMobileMessageService messageService;

    /**
     * 查询短信发送记录列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 短信发送记录分页结果
     */
    @SaCheckPermission("sms:record:list")
    @GetMapping("/list")
    public R<PageResult<MobileMessageVo>> list(MobileMessageBo bo, PageQuery pageQuery) {
        return R.ok(messageService.queryPageList(bo, pageQuery));
    }

    /**
     * 导出短信发送记录列表
     *
     * @param bo       查询条件
     * @param response HTTP 响应
     */
    @SaCheckPermission("sms:record:export")
    @Log(title = "短信发送记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(MobileMessageBo bo, HttpServletResponse response) {
        List<MobileMessageVo> list = messageService.queryList(bo);
        ExcelBuilder.of(list, MobileMessageVo.class).sheetName("短信发送记录").toResponse(response);
    }

    /**
     * 批量删除短信发送记录
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("sms:record:remove")
    @Log(title = "短信发送记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(messageService.deleteByIds(List.of(ids)));
    }

    /**
     * 清空短信发送记录
     *
     * @return 操作结果
     */
    @SaCheckPermission("sms:record:remove")
    @Log(title = "短信发送记录", businessType = BusinessType.CLEAN)
    @Lock4j
    @DeleteMapping("/clean")
    public R<Void> clean() {
        messageService.clean();
        return R.ok();
    }

}
