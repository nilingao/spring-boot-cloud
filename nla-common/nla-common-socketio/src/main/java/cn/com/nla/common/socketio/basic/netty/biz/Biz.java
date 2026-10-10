package cn.com.nla.common.socketio.basic.netty.biz;

import cn.com.nla.common.socketio.basic.netty.msg.Message;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public abstract class Biz {

    public abstract void doBiz(ChannelHandlerContext context, Map<String, Object> attributes, Object message) throws Exception;

    protected void writeAndFlush(final ChannelHandlerContext context, final Message message) {
        ChannelFuture future = context.writeAndFlush(message);
        if(log.isDebugEnabled()) {
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    log.debug("sent to {}, {}", context.channel().remoteAddress(), message);
                }
            });
        }
    }

    protected void writeAndClose(final ChannelHandlerContext context, final Message message) {
        ChannelFuture future = context.writeAndFlush(message);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(log.isDebugEnabled()) {
                    log.debug("sent to {}, {}", context.channel().remoteAddress(), message);
                }
                context.close();
                if(log.isDebugEnabled()) {
                    log.debug("invoke close()");
                }
            }
        });
    }
}
