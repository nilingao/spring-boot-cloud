package cn.com.nla.message;

import cn.com.nla.common.core.domain.R;
import cn.com.nla.message.controller.SmsSendController;
import cn.com.nla.message.domain.bo.SmsSendBo;
import cn.com.nla.message.sms.core.SmsSendManager;
import cn.com.nla.message.sms.core.SmsSendResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Verifies the unified SMS send endpoint delegates and preserves the detailed result. */
class SmsSendControllerContractTest {

    @Test
    void sendDelegatesToTableDrivenManager() {
        SmsSendManager manager = mock(SmsSendManager.class);
        SmsSendController controller = new SmsSendController(manager);
        SmsSendBo request = new SmsSendBo();
        request.setType(2);
        request.setMobile("13800138000");
        SmsSendResult result = SmsSendResult.ok("发送成功", 300L, "msg-1");
        when(manager.smsSend(request)).thenReturn(result);

        R<SmsSendResult> response = controller.send(request);

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).isSameAs(result);
        verify(manager).smsSend(request);
    }
}
