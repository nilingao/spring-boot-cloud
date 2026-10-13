package cn.com.nla.callcenter.config.fs.runtime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

/**
 * 企业呼叫中心配置变更事件发布器。
 * <p>
 * 供管理域 Service 在增删改后发布 {@link FsCompanyConfigChangedEvent},
 * 与协议栈解耦: {@code freeswitch.enabled=false} 时事件仅被发布而无监听者,
 * 管理功能不受影响; 启用后由监听器在事务提交后刷新企业运行时缓存。
 *
 * @author TZY
 */
@Component
public class FsRuntimeConfigRefreshPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 构造事件发布器。
     *
     * @param eventPublisher Spring 事件发布器
     */
    public FsRuntimeConfigRefreshPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发布单个企业配置变更事件。
     *
     * @param companyId 受影响的企业编号, 为空时忽略
     */
    public void publishCompanyChanged(Long companyId) {
        if (companyId == null) {
            return;
        }
        eventPublisher.publishEvent(new FsCompanyConfigChangedEvent(companyId));
    }

    /**
     * 批量发布企业配置变更事件, 自动去重与过滤空值。
     *
     * @param companyIds 受影响的企业编号集合, 为空时忽略
     */
    public void publishCompaniesChanged(Collection<Long> companyIds) {
        if (companyIds == null) {
            return;
        }
        companyIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .forEach(this::publishCompanyChanged);
    }

}
