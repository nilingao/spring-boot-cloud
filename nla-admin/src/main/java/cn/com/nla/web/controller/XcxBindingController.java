package cn.com.nla.web.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.utils.ValidatorUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.system.api.model.XcxBindBody;
import cn.com.nla.system.service.SysXcxBindingService;
import cn.com.nla.web.service.WechatMiniClient;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 已登录系统账号绑定微信小程序身份，沿用系统客户端访问检查。 */
@RestController
@RequiredArgsConstructor
public class XcxBindingController {
    private final WechatMiniClient wechatMiniClient;
    private final SysXcxBindingService bindingService;

    @PostMapping("/auth/xcx/bind")
    public R<Void> bind(@RequestBody XcxBindBody body) {
        StpUtil.checkLogin();
        ValidatorUtils.validate(body);
        var identity = wechatMiniClient.exchange(body.getAppid(), body.getXcxCode(),
            (String) StpUtil.getExtra(LoginHelper.CLIENT_KEY));
        bindingService.bind(LoginHelper.getUserId(), identity.appid(), identity.openid(), identity.unionId());
        return R.ok();
    }
}
