package cn.com.nla.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.com.nla.common.core.constant.SystemConstants;
import cn.com.nla.common.core.enums.LoginType;
import cn.com.nla.common.core.exception.user.UserException;
import cn.com.nla.common.core.utils.ValidatorUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.system.api.model.XcxLoginBody;
import cn.com.nla.system.api.model.XcxLoginUser;
import cn.com.nla.system.domain.vo.SysClientVo;
import cn.com.nla.system.mapper.SysUserMapper;
import cn.com.nla.system.service.SysXcxBindingService;
import cn.com.nla.web.domain.vo.LoginVo;
import cn.com.nla.web.service.IAuthStrategy;
import cn.com.nla.web.service.SysLoginService;
import cn.com.nla.web.service.WechatMiniClient;
import org.springframework.stereotype.Service;

/**
 * 小程序认证策略
 *
 * @author TZY
 */
@Slf4j
@Service("xcx" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class XcxAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;
    private final WechatMiniClient wechatMiniClient;
    private final SysXcxBindingService bindingService;
    private final SysUserMapper userMapper;

    /**
     * 执行微信小程序登录，并根据 openid 构建小程序用户登录态。
     *
     * @param body   登录请求体
     * @param client 当前客户端配置
     * @return 登录结果
     */
    @Override
    public LoginVo login(String body, SysClientVo client) {
        XcxLoginBody loginBody = JsonUtils.parseObject(body, XcxLoginBody.class);
        ValidatorUtils.validate(loginBody);
        var identity = wechatMiniClient.exchange(loginBody.getAppid(), loginBody.getXcxCode(), client.getClientId());
        Long userId = bindingService.findUserId(identity.appid(), identity.openid());
        var user = userMapper.selectVoById(userId);
        if (user == null) { throw new UserException("user.not.exists", userId); }
        if (SystemConstants.DISABLE.equals(user.getStatus())) { throw new UserException("user.blocked", userId); }
        // 与密码/短信/邮箱共享账号锁定；微信授权成功不能绕过账号锁。
        loginService.checkLogin(LoginType.XCX, user.getUserName(), () -> false);
        XcxLoginUser loginUser = new XcxLoginUser();
        BeanUtil.copyProperties(loginService.buildLoginUser(user), loginUser);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        loginUser.setOpenid(identity.openid());
        loginUser.setAppid(identity.appid());
        // 生成token
        LoginHelper.login(loginUser, IAuthStrategy.buildLoginParameter(client));

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        loginVo.setOpenid(identity.openid());
        return loginVo;
    }

}
