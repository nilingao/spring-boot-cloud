package cn.com.nla.message.service.impl;

import cn.com.nla.message.sms.core.SmsSendManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SmsServiceImpl 契约测试：验证跨模块出口正确委托 SmsSendManager。
 */
@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    @Mock
    private SmsSendManager smsSendManager;

    @InjectMocks
    private SmsServiceImpl smsService;

    @Test
    void sendNoticeDelegatesToManager() {
        List<String> mobiles = List.of("13800000001", "13800000002");
        when(smsSendManager.sendNotice(mobiles, "审批提醒")).thenReturn(2);

        int result = smsService.sendNotice(mobiles, "审批提醒");

        assertThat(result).isEqualTo(2);
        verify(smsSendManager).sendNotice(mobiles, "审批提醒");
    }

    @Test
    void sendNoticeWithEmptyCollectionReturnsZero() {
        when(smsSendManager.sendNotice(Collections.emptyList(), "x")).thenReturn(0);

        int result = smsService.sendNotice(Collections.emptyList(), "x");

        assertThat(result).isZero();
    }

    @Test
    void sendNoticeWithNullMobilesReturnsZero() {
        when(smsSendManager.sendNotice(null, "content")).thenReturn(0);

        int result = smsService.sendNotice(null, "content");

        assertThat(result).isZero();
        verify(smsSendManager).sendNotice(null, "content");
    }

}
