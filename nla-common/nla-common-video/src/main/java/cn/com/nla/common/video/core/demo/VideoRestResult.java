package cn.com.nla.common.video.core.demo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Web-independent asynchronous video result, with a lazy timeout fallback.
 * @author TZY
 */
public class VideoRestResult<T> extends CompletableFuture<T> {
    private final Long timeoutValue;

    public VideoRestResult() { this(null, null); }
    public VideoRestResult(Long timeoutValue) { this(timeoutValue, null); }
    public VideoRestResult(Long timeoutValue, Supplier<? extends T> timeoutResult) {
        this.timeoutValue = timeoutValue;
        if (timeoutValue != null && timeoutValue > 0) {
            CompletableFuture.delayedExecutor(timeoutValue, TimeUnit.MILLISECONDS).execute(() -> timeout(timeoutResult));
        }
    }
    public Long getTimeoutValue() { return timeoutValue; }
    private synchronized void timeout(Supplier<? extends T> fallback) {
        if (isDone()) return;
        try {
            if (fallback == null) completeExceptionally(new java.util.concurrent.TimeoutException("Video request timed out"));
            else complete(fallback.get());
        } catch (Throwable failure) { completeExceptionally(failure); }
    }
    @Override
    public synchronized boolean complete(T value) { return super.complete(value); }
    @Override
    public synchronized boolean completeExceptionally(Throwable failure) { return super.completeExceptionally(failure); }
}
