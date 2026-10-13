package cn.com.nla.callcenter;

import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigRefreshListener;
import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigRefreshPublisher;
import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * 运行时配置刷新链事务语义测试。
 *
 * <p>迁移自旧 manage.system.FsRuntimeConfigRefreshTest：
 * 验证 {@code @TransactionalEventListener(AFTER_COMMIT)} 语义 —— 事务提交后触发
 * 企业运行时缓存刷新，回滚后不触发。最小装配发布器/监听器与内存事务管理器，
 * 不依赖数据库与协议栈；监听器的 freeswitch.enabled 门控仅在自动装配时生效，
 * 此处手动注册绕开条件判断。</p>
 *
 * @author TZY
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FsRuntimeConfigRefreshTest.TestConfig.class)
class FsRuntimeConfigRefreshTest {

    @Autowired
    private TestTransactionService transactionService;

    @Autowired
    private FsRuntimeConfigService runtimeConfigService;

    @BeforeEach
    void resetMock() {
        reset(runtimeConfigService);
    }

    /**
     * 事务提交后刷新对应企业的运行时缓存。
     */
    @Test
    void refreshRunsAfterCommit() {
        transactionService.publishAndCommit(7L);

        verify(runtimeConfigService).refreshCompany(7L);
    }

    /**
     * 事务回滚后不触发刷新。
     */
    @Test
    void refreshDoesNotRunAfterRollback() {
        assertThrows(RuntimeException.class, () -> transactionService.publishAndRollback(7L));

        verify(runtimeConfigService, never()).refreshCompany(anyLong());
    }

    /**
     * 最小测试装配：事件发布器 + 事务提交监听器 + mock 刷新服务 + 事务内发布事件的服务。
     */
    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new NoopTransactionManager();
        }

        @Bean
        public FsRuntimeConfigRefreshPublisher refreshPublisher(ApplicationEventPublisher pub) {
            return new FsRuntimeConfigRefreshPublisher(pub);
        }

        @Bean
        public FsRuntimeConfigRefreshListener refreshListener(FsRuntimeConfigService svc) {
            return new FsRuntimeConfigRefreshListener(svc);
        }

        @Bean
        public FsRuntimeConfigService runtimeConfigService() {
            return mock(FsRuntimeConfigService.class);
        }

        @Bean
        public TestTransactionService testTransactionService(FsRuntimeConfigRefreshPublisher publisher) {
            return new TestTransactionService(publisher);
        }
    }

    /**
     * 事务内发布企业配置变更事件的测试服务。
     */
    static class TestTransactionService {

        private final FsRuntimeConfigRefreshPublisher publisher;

        TestTransactionService(FsRuntimeConfigRefreshPublisher publisher) {
            this.publisher = publisher;
        }

        @Transactional
        public void publishAndCommit(Long companyId) {
            publisher.publishCompanyChanged(companyId);
        }

        @Transactional(rollbackFor = RuntimeException.class)
        public void publishAndRollback(Long companyId) {
            publisher.publishCompanyChanged(companyId);
            throw new RuntimeException("预期回滚");
        }
    }

    /**
     * 内存事务管理器，不访问数据库，用于验证事务同步回调行为。
     */
    static class NoopTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return false;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // no-op
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // no-op: 提交同步回调由 AbstractPlatformTransactionManager 统一触发
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // no-op
        }
    }
}
