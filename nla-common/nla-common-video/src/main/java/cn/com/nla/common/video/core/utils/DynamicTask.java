package cn.com.nla.common.video.core.utils;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 动态定时器任务。
 * <p>
 * 提供按 key 管理的循环任务({@link #startCron})与延时任务({@link #startDelay})能力,
 * 用于 Video/SIP 事件订阅、注册心跳、呼叫队列超时等场景。
 * <p>
 * 任务随消费方应用关闭时释放。
 *
 * @author TZY
 */
@Slf4j
@Order(-1)
public class DynamicTask implements AutoCloseable {

    /**
     * 调度线程池，初始化 4 个线程。
     */
    private final ScheduledThreadPoolExecutor executor = ThreadUtil.createScheduledExecutor(4);
    private final Map<String, ScheduledFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Map<String, Runnable> runnableMap = new ConcurrentHashMap<>();

    public DynamicTask() {
        this.startCron("dynamic_task_execute", 5 * 60, this::execute);
    }

    private void execute() {
        if (!futureMap.isEmpty()) {
            for (String key : futureMap.keySet()) {
                ScheduledFuture<?> future = futureMap.get(key);
                if (future != null && (future.isDone() || future.isCancelled())) {
                    futureMap.remove(key);
                    runnableMap.remove(key);
                }
            }
        }
    }

    public ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }

    public void startCron(String key, int cycleForCatalog, Runnable task) {
        startCron(key, cycleForCatalog, cycleForCatalog, task);
    }

    /**
     * 循环执行的任务。
     *
     * @param key             任务ID
     * @param initialDelay    首次延时 秒
     * @param cycleForCatalog 间隔 秒
     * @param task            任务
     */
    public void startCron(String key, int initialDelay, int cycleForCatalog, Runnable task) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        stop(key);
        ScheduledFuture<?> future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                log.warn("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                log.warn("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        // scheduleAtFixedRate 以上一个任务开始时间为基准计时,cycleForCatalog 表示执行的间隔
        future = executor.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                log.error("任务【{}】执行异常", key, exception);
            } finally {
                log.info("任务【{}】 线程容量【运行线程：{}/空闲线程：{}/基本大小：{}】", key, executor.getActiveCount(), executor.getPoolSize(), executor.getCorePoolSize());
            }
        }, initialDelay, cycleForCatalog, TimeUnit.SECONDS);
        futureMap.put(key, future);
        runnableMap.put(key, task);
    }

    /**
     * 延时任务。
     *
     * @param key   任务ID
     * @param delay 延时 秒
     * @param task  任务
     */
    public void startDelay(String key, int delay, Runnable task) {
        if (StrUtil.isBlank(key)) {
            return;
        }
        stop(key);
        ScheduledFuture<?> future = futureMap.get(key);
        if (future != null) {
            if (future.isCancelled()) {
                log.warn("任务【{}】已存在但是关闭状态！！！", key);
            } else {
                log.warn("任务【{}】已存在且已启动！！！", key);
                return;
            }
        }
        future = executor.schedule(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                log.error("任务【{}】执行异常", key, exception);
            } finally {
                log.info("任务【{}】 线程容量【运行线程：{}/空闲线程：{}/基本大小：{}】 ", key, executor.getActiveCount(), executor.getPoolSize(), executor.getCorePoolSize());
            }
        }, delay, TimeUnit.SECONDS);
        futureMap.put(key, future);
        runnableMap.put(key, task);
    }

    /**
     * 强行暂停。
     *
     * @param key 任务ID
     * @return 是否成功取消
     */
    public boolean stop(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        boolean result = false;
        if (isAlive(key)) {
            result = futureMap.get(key).cancel(false);
            futureMap.remove(key);
            runnableMap.remove(key);
        }
        return result;
    }

    public boolean contains(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        return futureMap.get(key) != null;
    }

    public Set<String> getAllKeys() {
        return futureMap.keySet();
    }

    public Runnable get(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        return runnableMap.get(key);
    }

    public boolean isAlive(String key) {
        return futureMap.get(key) != null && !futureMap.get(key).isDone() && !futureMap.get(key).isCancelled();
    }

    @Override
    public void close() {
        executor.shutdownNow();
        futureMap.clear();
        runnableMap.clear();
    }
}

