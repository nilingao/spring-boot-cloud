package cn.com.nla.message.controller;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.message.domain.bo.SmsSendBo;
import cn.com.nla.message.sms.core.SmsSendManager;
import cn.com.nla.message.sms.core.SmsSendResult;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通用短信发送入口。
 *
 * <p>旧单体拆分工程分别暴露 api/app/webapi 三个同构入口；新单体统一收敛为
 * {@code /sms/send}，具体渠道和模板仍由表驱动发送引擎选择。</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/sms")
public class SmsSendController {

    private final SmsSendManager smsSendManager;

    /**
     * 发送短信。
     *
     * @param bo 消息类型与手机号
     * @return 发送结果（含防重发剩余时间）
     */
    @SaCheckPermission("sms:send")
    @PostMapping("/send")
    public R<SmsSendResult> send(@Valid @RequestBody SmsSendBo bo) {
        return R.ok(smsSendManager.smsSend(bo));
    }
}
