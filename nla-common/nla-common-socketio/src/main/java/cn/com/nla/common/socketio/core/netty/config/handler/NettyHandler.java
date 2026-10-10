package cn.com.nla.common.socketio.core.netty.config.handler;


import cn.com.nla.common.socketio.basic.netty.biz.Biz;
import cn.com.nla.common.socketio.basic.netty.factory.BizFactory;
import cn.com.nla.common.socketio.basic.netty.msg.Message;
import cn.com.nla.common.socketio.core.netty.msg.MessageProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
public class NettyHandler extends SimpleChannelInboundHandler<Message> {

    protected BizFactory bizFactory;
    protected ExecutorService executorService;
    protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    public NettyHandler(BizFactory bizFactory, ExecutorService executorService) {
        this.bizFactory = bizFactory;
        this.executorService = executorService;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        if(log.isDebugEnabled()) {
            log.debug("this.instance: {}", this);
            log.debug("ChannelHandlerContext.instance: {}", ctx);
            log.debug("from:{}, recv:{}, ", ctx.channel().remoteAddress(), message);
        }

        Biz biz = bizFactory.create(message.getMsgCode());
        if(biz != null) {
            MessageProcessor processor = new MessageProcessor();
            processor.channelHandlerContext = ctx;
            processor.message = message;
            processor.biz = biz;
            processor.attributes = attributes;
            executorService.submit(processor);
        }
        log.debug("executorService {}", executorService);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 添加channel时
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        Channel incoming = ctx.channel();
        log.debug("SimpleChatClient:" + incoming.remoteAddress() + "通道被添加");
    }

    /**
     * 删除channel时
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.debug("SimpleChatClient:{},通道被删除", channel.remoteAddress());
    }

    /**
     * 服务端监听到客户端不活动
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //服务端接收到客户端掉线通知
        Channel incoming = ctx.channel();
        log.error("SimpleChatClient: {}  掉线", incoming.remoteAddress());
    }

    /**
     * 服务端监听到客户端活动
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        //服务端接收到客户端上线通知
        Channel incoming = ctx.channel();
        log.info("SimpleChatClient:" + incoming.remoteAddress() + "上线");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if(cause instanceof ReadTimeoutException) {
            if(log.isInfoEnabled()) {
                log.info("session timout, {}", ctx.channel().remoteAddress());
            }
        }

        log.error("error", cause);
        ctx.close();
        if(log.isDebugEnabled()) {
            log.debug("invoke close()");
        }
    }
}
