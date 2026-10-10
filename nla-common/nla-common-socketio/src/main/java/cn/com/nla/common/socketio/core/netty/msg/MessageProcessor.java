package cn.com.nla.common.socketio.core.netty.msg;

import cn.com.nla.common.socketio.basic.netty.biz.Biz;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class MessageProcessor implements Runnable {

    private long submitTime = 0;
    private long beginTime = 0;
    private long endTime = 0;
    private boolean executeSuccess = true;

    public ChannelHandlerContext channelHandlerContext;
    public Object message;
    public Biz biz;
    public Map<String, Object> attributes;

    public MessageProcessor() {
        submitTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        before();
        try {
            //3分钟前的数据不处理
            if(beginTime - submitTime > 3 * 60 * 1000){
                if(log.isInfoEnabled()) {
                    log.info("线程阻塞放弃");
                }
                return;
            }
            biz.doBiz(channelHandlerContext, attributes, message);
        } catch (Exception e) {
            executeSuccess = false;
            log.error("MessageProcessor execute error", e);

        } finally {
            after();
        }
    }

    protected void before() {
        beginTime = System.currentTimeMillis();
    }

    protected void after() {
        endTime = System.currentTimeMillis();
    }
}
