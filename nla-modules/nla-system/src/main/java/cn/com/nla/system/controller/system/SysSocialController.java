package cn.com.nla.system.controller.system;

import lombok.RequiredArgsConstructor;
import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.common.web.core.BaseController;
import cn.com.nla.system.domain.vo.SysSocialVo;
import cn.com.nla.system.service.ISysSocialService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 社会化关系
 *
 * @author TZY
 * @date 2023-06-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/social")
public class SysSocialController extends BaseController {

    private final ISysSocialService socialUserService;

    /**
     * 查询当前登录用户的社会化账号绑定列表。
     *
     * @return 绑定关系列表
     */
    @GetMapping("/list")
    public R<List<SysSocialVo>> list() {
        return R.ok(socialUserService.queryListByUserId(LoginHelper.getUserId()));
    }

}
