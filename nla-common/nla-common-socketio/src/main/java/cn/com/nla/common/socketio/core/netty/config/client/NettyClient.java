package cn.com.nla.common.socketio.core.netty.config.client;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

/** Reconnecting TCP client; close interrupts connection and retry waits. @author TZY */
@Slf4j
public abstract class NettyClient implements Runnable, AutoCloseable {
    private final String host;
    private final int port;
    private final int nThreads;
    private final String threadName;
    public Notice offlineNotice;
    public Notice connectedNotice;
    private volatile Channel channel;
    private volatile EventLoopGroup group;
    private volatile boolean closed;
    private volatile Thread thread;
    public NettyClient(String host, int port, int nThreads, String threadName) {
        this(host, port, nThreads, threadName, null, null);
    }
    public NettyClient(String host, int port, int nThreads, String threadName, Notice offlineNotice, Notice connectedNotice) {
        if (host == null || host.isBlank() || port < 1 || port > 65535 || nThreads < 0) {
            throw new IllegalArgumentException("Invalid TCP client address/thread count");
        }
        this.host = host; this.port = port; this.nThreads = nThreads; this.threadName = threadName;
        this.offlineNotice = offlineNotice; this.connectedNotice = connectedNotice;
    }
    public synchronized void startup() throws InterruptedException {
        if (closed) { throw new IllegalStateException("TCP client is closed"); }
        if (thread != null && thread.isAlive()) { return; }
        thread = new Thread(this, threadName);
        thread.start();
    }
    @Override public void run() {
        try {
            while (!closed) {
                runServer();
                if (!closed) { Thread.sleep(1000); }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } finally { closed = true; }
    }
    public void runServer() {
        EventLoopGroup owned = new NioEventLoopGroup(nThreads);
        group = owned;
        boolean connected = false;
        try {
            if (closed) { return; }
            ChannelFuture connection = new Bootstrap().group(owned).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override protected void initChannel(SocketChannel child) { NettyClient.this.initChannel(child); }
                }).connect(host, port);
            channel = connection.channel();
            connection.sync();
            if (closed) { return; }
            connected = true;
            if (connectedNotice != null) { connectedNotice.notice(this); }
            channel.closeFuture().sync();
        } catch (InterruptedException interrupted) {
            closed = true;
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            if (!closed) { log.warn("TCP connection failed: {}:{}", host, port, failure); }
        } finally {
            if (channel != null) { channel.close().syncUninterruptibly(); channel = null; }
            owned.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly();
            group = null;
            if (connected && offlineNotice != null) {
                try { offlineNotice.notice(this); } catch (RuntimeException failure) { log.warn("TCP offline callback failed", failure); }
            }
        }
    }
    @Override public void close() {
        closed = true;
        Channel current = channel;
        if (current != null) { current.close(); }
        Thread runner = thread;
        if (runner != null && runner != Thread.currentThread()) { runner.interrupt(); }
        EventLoopGroup owned = group;
        if (owned != null) { owned.shutdownGracefully(0, 5, TimeUnit.SECONDS); }
        if (runner != null && runner != Thread.currentThread()) {
            try { runner.join(6000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        }
    }
    public Channel getChannel() { return channel; }
    protected abstract void initChannel(SocketChannel channel);
    @FunctionalInterface public interface Notice { void notice(NettyClient client); }
}
