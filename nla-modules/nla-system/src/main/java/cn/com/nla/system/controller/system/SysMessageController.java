package cn.com.nla.system.controller.system;

import lombok.RequiredArgsConstructor;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.system.domain.vo.SysMessageBoxVo;
import cn.com.nla.system.service.ISysMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息记录控制器
 *
 * @author TZY
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/resource/message")
public class SysMessageController extends BaseController {

    private final ISysMessageService messageService;

    /**
     * 查询当前用户消息盒子数据
     *
     * @return 消息盒子数据
     */
    @GetMapping("/box")
    public R<SysMessageBoxVo> getBox() {
        return R.ok(messageService.queryMessageBox(LoginHelper.getUserId()));
    }
}
