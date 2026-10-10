package cn.com.nla.web.service;

import cn.com.nla.common.core.constant.SystemConstants;
import cn.com.nla.message.domain.bo.SmsSendBo;
import cn.com.nla.message.sms.SmsConstant;
import cn.com.nla.message.sms.core.SmsSendManager;
import cn.com.nla.message.sms.core.SmsSendResult;
import cn.com.nla.system.domain.SysUser;
import cn.com.nla.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 登录短信发送：检查账号可用后，由表驱动引擎选择渠道与模板。 */
@Service
@RequiredArgsConstructor
public class SmsLoginCodeService {
    private final SysUserMapper userMapper;
    private final SmsSendManager smsSendManager;

    /** 不向缺失或停用的账号发送登录验证码。 */
    public SmsSendResult send(String phoneNumber) {
        var user = userMapper.lambda().eq(SysUser::getPhoneNumber, phoneNumber).voOne();
        if (user == null || SystemConstants.DISABLE.equals(user.getStatus())) {
            return SmsSendResult.fail("账号不存在或已停用");
        }
        var request = new SmsSendBo();
        request.setType(SmsConstant.TYPE_LOGIN);
        request.setMobile(phoneNumber);
        return smsSendManager.smsSend(request);
    }
}
