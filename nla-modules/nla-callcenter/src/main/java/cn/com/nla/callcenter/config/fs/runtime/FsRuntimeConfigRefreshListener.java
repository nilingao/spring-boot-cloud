package cn.com.nla.callcenter.config.fs.runtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 企业呼叫中心配置变更事件监听器。
 *
 * <p>管理域 Service 在事务提交后发布 {@link FsCompanyConfigChangedEvent}，
 * 本监听器定向刷新对应企业的运行时缓存。仅在 freeswitch.enabled=true 时注册，
 * 停用协议栈时事件发布无监听者，管理功能不受影响。</p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class FsRuntimeConfigRefreshListener {

    private final FsRuntimeConfigService runtimeConfigService;

    /**
     * 企业配置变更后刷新运行时缓存。
     *
     * @param event 企业配置变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCompanyChanged(FsCompanyConfigChangedEvent event) {
        log.info("[呼叫中心] 企业配置变更，刷新运行时缓存: companyId={}", event.getCompanyId());
        runtimeConfigService.refreshCompany(event.getCompanyId());
    }
}
