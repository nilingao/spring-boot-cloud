package cn.com.nla.common.socketio.core.netty.config.server;
import cn.com.nla.common.socketio.core.netty.md.MessageDecoder;
import cn.com.nla.common.socketio.core.netty.md.MessageEncoder;
import cn.com.nla.common.socketio.basic.netty.msg.Message;
import cn.com.nla.common.socketio.basic.netty.msg.MessageFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.concurrent.TimeUnit;

/** Explicit TCP lifecycle, with lazy resource allocation. @author TZY */
public abstract class NettyServer implements AutoCloseable {
    private final int port;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final ChannelGroup connections = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public NettyServer(int port) {
        if (port < 0 || port > 65535) { throw new IllegalArgumentException("Invalid TCP port"); }
        this.port = port;
    }
    public synchronized void startup() throws InterruptedException {
        if (channel != null && channel.isActive()) { return; }
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            channel = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override protected void initChannel(SocketChannel child) {
                        connections.add(child);
                        child.pipeline().addLast(new ReadTimeoutHandler(180), new MessageDecoder(newMessageFactory()),
                            new MessageEncoder(), newNettyHandler());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true)
                .bind(port).sync().channel();
        } catch (Throwable failure) {
            try { close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            if (failure instanceof InterruptedException interrupted) { throw interrupted; }
            if (failure instanceof Error error) { throw error; }
            throw new IllegalStateException("TCP server failed to start", failure);
        }
    }
    public synchronized Channel getChannel() { return channel; }
    @Override public synchronized void close() {
        if (channel != null) { channel.close().syncUninterruptibly(); channel = null; }
        connections.close().awaitUninterruptibly();
        if (workerGroup != null) { workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly(); workerGroup = null; }
        if (bossGroup != null) { bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).syncUninterruptibly(); bossGroup = null; }
    }
    protected abstract SimpleChannelInboundHandler<Message> newNettyHandler();
    protected abstract MessageFactory newMessageFactory();
}
