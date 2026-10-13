package cn.com.nla.callcenter.config.fs;

import cn.com.nla.callcenter.config.fs.runtime.FsRuntimeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时将企业配置全量装载到 Redis。
 *
 * <p>具体初始化逻辑已提取到 {@link FsRuntimeConfigService}，避免在此类中混入
 * 管理接口缓存刷新语义。仅在 freeswitch.enabled=true 时注册，避免协议组件
 * 缺失导致启动失败。</p>
 *
 * @author TZY
 */
@Slf4j
@RequiredArgsConstructor
@Order(20)
@Component
@ConditionalOnProperty(prefix = "freeswitch", name = "enabled", havingValue = "true")
public class CompanyRunner implements CommandLineRunner {

    private final FsRuntimeConfigService runtimeConfigService;

    /**
     * 应用启动后全量重建企业运行时缓存。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        log.info("[呼叫中心] 启动装载企业运行时配置开始");
        runtimeConfigService.refreshAll();
        log.info("[呼叫中心] 启动装载企业运行时配置完成");
    }
}
