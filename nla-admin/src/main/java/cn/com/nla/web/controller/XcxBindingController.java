package cn.com.nla.web.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.core.utils.ValidatorUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.system.api.model.XcxBindBody;
import cn.com.nla.system.api.model.XcxLoginUser;
import cn.com.nla.system.api.model.XcxPhoneBody;
import cn.com.nla.system.domain.bo.SysUserBo;
import cn.com.nla.system.service.ISysUserService;
import cn.com.nla.system.service.SysXcxBindingService;
import cn.com.nla.web.config.properties.XcxProperties;
import cn.com.nla.web.service.WechatMiniClient;
import cn.com.nla.web.service.WechatPhoneClient;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 已登录系统账号绑定微信小程序身份及手机号授权，沿用系统客户端访问检查。 */
@RestController
@RequiredArgsConstructor
public class XcxBindingController {
    private final WechatMiniClient wechatMiniClient;
    private final SysXcxBindingService bindingService;
    private final WechatPhoneClient wechatPhoneClient;
    private final XcxProperties xcxProperties;
    private final ISysUserService userService;

    @PostMapping("/auth/xcx/bind")
    public R<Void> bind(@RequestBody XcxBindBody body) {
        StpUtil.checkLogin();
        ValidatorUtils.validate(body);
        var identity = wechatMiniClient.exchange(body.getAppid(), body.getXcxCode(),
            (String) StpUtil.getExtra(LoginHelper.CLIENT_KEY));
        bindingService.bind(LoginHelper.getUserId(), identity.appid(), identity.openid(), identity.unionId());
        return R.ok();
    }

    /**
     * 小程序用户授权手机号，更新当前系统账号的手机号码。
     * <p>
     * 要求当前会话为小程序登录（XcxLoginUser），appid 与会话一致。
     * </p>
     */
    @PostMapping("/auth/xcx/phone")
    public R<Void> phone(@RequestBody XcxPhoneBody body) {
        StpUtil.checkLogin();
        ValidatorUtils.validate(body);
        // 验证会话为小程序登录且 appid 匹配
        XcxLoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || StringUtils.isBlank(loginUser.getAppid())) {
            throw new ServiceException("当前会话不是小程序登录");
        }
        if (!body.getAppid().equals(loginUser.getAppid())) {
            throw new ServiceException("请求 appid 与会话不一致");
        }
        // 验证 app 配置可用
        var app = xcxProperties.getApps() == null ? null : xcxProperties.getApps().get(body.getAppid());
        if (!xcxProperties.isEnabled() || app == null || !app.isEnabled()
            || StringUtils.isBlank(app.getSecret())) {
            throw new ServiceException("当前小程序未配置或已停用");
        }
        // 调用微信 API 获取手机号
        String phoneNumber = wechatPhoneClient.resolvePhoneNumber(body.getAppid(), app, body.getPhoneCode());
        // 唯一性校验
        SysUserBo checkBo = new SysUserBo();
        checkBo.setUserId(loginUser.getUserId());
        checkBo.setPhoneNumber(phoneNumber);
        if (!userService.checkPhoneUnique(checkBo)) {
            throw new ServiceException("该手机号已被其他账号使用");
        }
        // 更新当前用户手机号
        userService.updateUserProfile(checkBo);
        return R.ok();
    }
}
