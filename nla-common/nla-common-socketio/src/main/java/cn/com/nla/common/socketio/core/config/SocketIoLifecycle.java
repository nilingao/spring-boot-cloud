package cn.com.nla.common.socketio.core.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.context.SmartLifecycle;

/** 在单例和监听器就绪后监听端口，关闭及绑定失败时释放 Netty 资源。 @author TZY */
public class SocketIoLifecycle implements SmartLifecycle {
    private final SocketIOServer server;
    private final boolean autoStart;
    private boolean running;

    public SocketIoLifecycle(SocketIOServer server, boolean autoStart) {
        this.server = server;
        this.autoStart = autoStart;
    }

    @Override
    public synchronized void start() {
        if (running) { return; }
        try {
            server.start();
            running = true;
        } catch (Throwable failure) {
            try { server.stop(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            if (failure instanceof Error error) { throw error; }
            throw new IllegalStateException("Socket.IO server failed to start", failure);
        }
    }

    @Override
    public synchronized void stop() {
        if (!running) { return; }
        try { server.stop(); } finally { running = false; }
    }

    @Override public synchronized boolean isRunning() { return running; }
    @Override public boolean isAutoStartup() { return autoStart; }
    @Override public int getPhase() { return Integer.MAX_VALUE; }
}
