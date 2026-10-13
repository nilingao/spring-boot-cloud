package cn.com.nla.callcenter;

import cn.com.nla.callcenter.config.fs.runtime.FsCompanyConfigChangedEvent;
import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigRefreshPublisher;
import cn.com.nla.callcenter.domain.vo.FsOptionVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 呼叫中心通用契约测试。
 *
 * <p>迁移自旧 manage.system.FsCommonContractTest 的可迁移部分：
 * 运行时刷新事件发布器去重语义、事件非空约束与下拉选项视图构造。</p>
 *
 * @author TZY
 */
class FsCommonContractTest {

    /**
     * 批量发布企业配置变更事件时去重并过滤空值。
     */
    @Test
    void publishingCompaniesChangedPublishesDistinctNonNullCompanies() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        FsRuntimeConfigRefreshPublisher publisher = new FsRuntimeConfigRefreshPublisher(eventPublisher);
        ArgumentCaptor<FsCompanyConfigChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(FsCompanyConfigChangedEvent.class);

        publisher.publishCompaniesChanged(Arrays.asList(1L, 1L, null, 2L));

        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        List<Long> companyIds = eventCaptor.getAllValues().stream()
                .map(FsCompanyConfigChangedEvent::getCompanyId)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList(1L, 2L), companyIds);
    }

    /**
     * 企业配置变更事件拒绝空企业编号。
     */
    @Test
    void companyConfigChangedEventRejectsNullCompanyId() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new FsCompanyConfigChangedEvent(null)
        );

        assertEquals("companyId", exception.getMessage());
    }

    /**
     * 下拉选项视图构造器映射全部字段。
     */
    @Test
    void optionVoBuilderMapsAllFields() {
        FsOptionVo option = FsOptionVo.builder()
                .id("1")
                .name("name")
                .code("code")
                .status(1)
                .build();

        assertEquals("1", option.getId());
        assertEquals("name", option.getName());
        assertEquals("code", option.getCode());
        assertEquals(1, option.getStatus());
    }

    /**
     * 单个发布空企业编号时静默忽略，不触发事件。
     */
    @Test
    void publishingSingleNullCompanyIsIgnored() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        FsRuntimeConfigRefreshPublisher publisher = new FsRuntimeConfigRefreshPublisher(eventPublisher);

        publisher.publishCompanyChanged(null);

        verify(eventPublisher, times(0)).publishEvent(Object.class.cast(null));
    }
}
