package cn.com.nla.web.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.com.nla.common.core.constant.Constants;
import cn.com.nla.common.core.constant.GlobalConstants;
import cn.com.nla.common.core.constant.SystemConstants;
import cn.com.nla.common.core.enums.LoginType;
import cn.com.nla.common.core.exception.user.CaptchaExpireException;
import cn.com.nla.common.core.exception.user.UserException;
import cn.com.nla.common.core.utils.MessageUtils;
import cn.com.nla.common.core.utils.StringUtils;
import cn.com.nla.common.core.utils.ValidatorUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.common.redis.utils.RedisUtils;
import cn.com.nla.common.satoken.utils.LoginHelper;
import cn.com.nla.system.api.model.EmailLoginBody;
import cn.com.nla.system.api.model.LoginUser;
import cn.com.nla.system.domain.SysUser;
import cn.com.nla.system.domain.vo.SysClientVo;
import cn.com.nla.system.domain.vo.SysUserVo;
import cn.com.nla.system.mapper.SysUserMapper;
import cn.com.nla.web.domain.vo.LoginVo;
import cn.com.nla.web.service.IAuthStrategy;
import cn.com.nla.web.service.SysLoginService;
import org.springframework.stereotype.Service;

/**
 * 邮件认证策略
 *
 * @author TZY
 */
@Slf4j
@Service("email" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class EmailAuthStrategy implements IAuthStrategy {

    private final SysLoginService loginService;
    private final SysUserMapper userMapper;

    /**
     * 执行邮箱验证码登录，并按客户端配置生成访问令牌。
     *
     * @param body   登录请求体
     * @param client 当前客户端配置
     * @return 登录结果
     */
    @Override
    public LoginVo login(String body, SysClientVo client) {
        EmailLoginBody loginBody = JsonUtils.parseObject(body, EmailLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String email = loginBody.getEmail();
        String emailCode = loginBody.getEmailCode();
        SysUserVo user = loadUserByEmail(email);
        loginService.checkLogin(LoginType.EMAIL, user.getUserName(), () -> !validateEmailCode(email, emailCode));
        // 此处可根据登录用户的数据不同 自行创建 loginUser 属性不够用继承扩展就行了
        LoginUser loginUser = loginService.buildLoginUser(user);
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter model = IAuthStrategy.buildLoginParameter(client);
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    /**
     * 校验邮箱验证码是否存在且匹配。
     *
     * @param email     邮箱地址
     * @param emailCode 用户输入的邮箱验证码
     * @return 是否校验通过
     */
    private boolean validateEmailCode(String email, String emailCode) {
        String code = RedisUtils.getCacheObject(GlobalConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(code)) {
            loginService.recordLoginInfo(email, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

    /**
     * 按邮箱加载可登录用户，并校验是否存在或被停用。
     *
     * @param email 邮箱地址
     * @return 用户信息
     */
    private SysUserVo loadUserByEmail(String email) {
        SysUserVo user = userMapper.lambda()
            .eq(SysUser::getEmail, email)
            .voOne();
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", email);
            throw new UserException("user.not.exists", email);
        } else if (SystemConstants.DISABLE.equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", email);
            throw new UserException("user.blocked", email);
        }
        return user;
    }

}
