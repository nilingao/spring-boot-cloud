package cn.com.nla.common.freeswitch.vo.result;

import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * fs 异步请求结果容器。
 * <p>
 * 原基于 spring-web 的 {@code DeferredResult}, 迁移后改为继承 {@link CompletableFuture},
 * 去除本 common 技术封装库对 spring-web 的类型依赖(去 web 化)。Spring MVC 原生支持
 * controller 直接返回 {@link CompletableFuture} 作为异步响应, 语义与 DeferredResult 等价。
 *
 * @author TZY
 */
@Getter
public class FsRestResult<T> extends CompletableFuture<T> {

    /**
     * 超时时间(毫秒)
     */
    private final Long timeoutValue;

    public FsRestResult(@Nullable Long timeoutValue, Supplier<?> timeoutResult) {
        this.timeoutValue = timeoutValue;
        if (timeoutValue != null && timeoutValue > 0 && timeoutResult != null) {
            // 复刻 DeferredResult 超时语义: 到期若仍未完成, 使用 timeoutResult 兜底(惰性求值)
            CompletableFuture.delayedExecutor(timeoutValue, TimeUnit.MILLISECONDS)
                .execute(() -> completeWithTimeout(timeoutResult));
        }
    }

    @SuppressWarnings("unchecked")
    private void completeWithTimeout(Supplier<?> timeoutResult) {
        complete((T) timeoutResult.get());
    }

}
