package cn.com.nla.web.service.impl;

import cn.com.nla.common.core.exception.ServiceException;
import cn.com.nla.common.core.utils.ValidatorUtils;
import cn.com.nla.common.json.utils.JsonUtils;
import cn.com.nla.system.domain.vo.SysClientVo;
import cn.com.nla.web.domain.model.QrLoginBody;
import cn.com.nla.web.domain.vo.LoginVo;
import cn.com.nla.web.service.IAuthStrategy;
import cn.com.nla.web.service.QrLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("qr" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class QrAuthStrategy implements IAuthStrategy {
    private final QrLoginService qr;

    @Override
    public LoginVo login(String body, SysClientVo client) {
        var request = JsonUtils.parseObject(body, QrLoginBody.class);
        ValidatorUtils.validate(request);
        if (!"qr".equals(request.getGrantType()) || !client.getClientId().equals(request.getClientId())) {
            throw new ServiceException("二维码客户端无效");
        }
        return qr.redeem(request.getScene(), request.getBrowserToken(), client.getClientId());
    }
}
