package cn.com.nla.system.service;

import cn.com.nla.common.core.constant.SystemConstants;
import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.exception.user.UserException;
import cn.com.nla.system.domain.SysSocial;
import cn.com.nla.system.mapper.SysSocialMapper;
import cn.com.nla.system.mapper.SysUserMapper;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 使用 sys_social 保存按 appid 隔离的小程序绑定；不自动注册或重建旧 mini 表。 */
@Service
@RequiredArgsConstructor
public class SysXcxBindingService {
    private final SysSocialMapper socialMapper;
    private final SysUserMapper userMapper;

    public static String source(String appid) {
        return "WECHAT_MINI_PROGRAM:" + appid;
    }

    public static String authId(String appid, String openid) {
        return source(appid) + ":" + openid;
    }

    /** 查询唯一有效绑定；有歧义或损坏的绑定不能用于登录。 */
    public Long findUserId(String appid, String openid) {
        validateIdentity(appid, openid);
        var bindings = findIdentity(appid, openid);
        if (bindings.size() != 1 || bindings.getFirst().getUserId() == null
            || !authId(appid, openid).equals(bindings.getFirst().getAuthId())) {
            throw new ServiceException("小程序账号尚未绑定或绑定异常，请先绑定系统账号");
        }
        return bindings.getFirst().getUserId();
    }

    /** 同一小程序的绑定写入串行：同身份重绑幂等，禁止抢占他人身份或静默换绑。 */
    @Lock4j(keys = "#appid")
    public void bind(Long userId, String appid, String openid, String unionId) {
        validateIdentity(appid, openid);
        if (userId == null) {
            throw new ServiceException("缺少已登录的系统账号");
        }
        var user = userMapper.selectVoById(userId);
        if (user == null) { throw new UserException("user.not.exists", userId); }
        if (SystemConstants.DISABLE.equals(user.getStatus())) { throw new UserException("user.blocked", userId); }
        var identityBindings = findIdentity(appid, openid);
        if (!identityBindings.isEmpty()) {
            if (identityBindings.size() == 1 && userId.equals(identityBindings.getFirst().getUserId())
                && authId(appid, openid).equals(identityBindings.getFirst().getAuthId())) {
                return;
            }
            throw new ServiceException("此小程序身份已绑定其他账号或绑定异常");
        }
        var userBindings = socialMapper.selectList(Wrappers.<SysSocial>lambdaQuery()
            .eq(SysSocial::getUserId, userId).eq(SysSocial::getSource, source(appid)).apply("del_flag = '0'"));
        if (userBindings.stream().anyMatch(binding -> source(appid).equals(binding.getSource()))) {
            throw new ServiceException("当前账号已绑定此小程序，请先解绑");
        }
        var binding = new SysSocial();
        binding.setUserId(userId);
        binding.setSource(source(appid));
        binding.setAuthId(authId(appid, openid));
        binding.setOpenId(openid);
        binding.setUnionId(unionId);
        binding.setUserName(user.getUserName());
        binding.setNickName(user.getNickName());
        // 表中 access_token 非空；小程序 session_key 不作为 OAuth token 持久化。
        binding.setAccessToken("");
        if (socialMapper.insert(binding) != 1) { throw new ServiceException("小程序绑定保存失败"); }
    }

    private List<SysSocial> findIdentity(String appid, String openid) {
        return socialMapper.selectList(Wrappers.<SysSocial>lambdaQuery()
            .eq(SysSocial::getSource, source(appid)).eq(SysSocial::getOpenId, openid).apply("del_flag = '0'"))
            // 默认数据库排序规则可能忽略大小写，openid 必须精确匹配。
            .stream().filter(binding -> source(appid).equals(binding.getSource())
                && openid.equals(binding.getOpenId())).toList();
    }

    private void validateIdentity(String appid, String openid) {
        if (appid == null || !appid.matches("wx[0-9a-f]{16}")
            || openid == null || !openid.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new ServiceException("小程序身份无效");
        }
    }
}
