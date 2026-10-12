package cn.com.nla.web.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.core.utils.ValidatorUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.system.api.model.LoginUser;
import cn.com.nla.system.api.model.XcxBindBody;
import cn.com.nla.system.api.model.XcxLoginUser;
import cn.com.nla.system.api.model.XcxPhoneBody;
import cn.com.nla.system.api.model.XcxProfileBody;
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

/** 已登录系统账号绑定微信小程序身份、手机号与资料授权，沿用系统客户端访问检查。 */
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
        // 验证会话为小程序登录、appid 匹配且 app 配置可用
        var app = requireXcxApp(body.getAppid());
        // 调用微信 API 获取手机号
        String phoneNumber = wechatPhoneClient.resolvePhoneNumber(body.getAppid(), app, body.getPhoneCode());
        // 唯一性校验
        SysUserBo checkBo = new SysUserBo();
        checkBo.setUserId(LoginHelper.getUserId());
        checkBo.setPhoneNumber(phoneNumber);
        if (!userService.checkPhoneUnique(checkBo)) {
            throw new ServiceException("该手机号已被其他账号使用");
        }
        // 更新当前用户手机号
        userService.updateUserProfile(checkBo);
        return R.ok();
    }

    /**
     * 小程序用户授权资料（昵称/性别/头像），回写当前系统账号；只更新请求中提供的字段。
     * <p>
     * 头像传 OSS 上传后的 ossId（sys_user.avatar 为 Long），不接受微信头像 URL 直存；
     * 与旧 bean_mini.avatar_url 直存 URL 属有意偏离。
     * </p>
     */
    @PostMapping("/auth/xcx/profile")
    public R<Void> profile(@RequestBody XcxProfileBody body) {
        StpUtil.checkLogin();
        ValidatorUtils.validate(body);
        requireXcxApp(body.getAppid());
        if (StringUtils.isBlank(body.getNickName()) && StringUtils.isBlank(body.getGender())
            && body.getAvatar() == null) {
            throw new ServiceException("没有需要更新的资料字段");
        }
        SysUserBo profileBo = new SysUserBo();
        profileBo.setUserId(LoginHelper.getUserId());
        profileBo.setNickName(body.getNickName());
        profileBo.setGender(body.getGender());
        profileBo.setAvatar(body.getAvatar());
        userService.updateUserProfile(profileBo);
        return R.ok();
    }

    /** 会话必须是小程序登录、appid 与会话精确匹配，且该小程序配置可用。 */
    private XcxProperties.App requireXcxApp(String appid) {
        // getLoginUser 为泛型隐式强转，普通会话必须先 instanceof 防御，避免 ClassCastException
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (!(loginUser instanceof XcxLoginUser mini) || StringUtils.isBlank(mini.getAppid())) {
            throw new ServiceException("当前会话不是小程序登录");
        }
        if (!appid.equals(mini.getAppid())) {
            throw new ServiceException("请求 appid 与会话不一致");
        }
        var app = xcxProperties.getApps() == null ? null : xcxProperties.getApps().get(appid);
        if (!xcxProperties.isEnabled() || app == null || !app.isEnabled()
            || StringUtils.isBlank(app.getSecret())) {
            throw new ServiceException("当前小程序未配置或已停用");
        }
        return app;
    }
}
