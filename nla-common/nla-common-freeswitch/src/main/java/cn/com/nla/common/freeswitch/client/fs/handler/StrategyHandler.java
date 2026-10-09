package cn.com.nla.common.freeswitch.client.fs.handler;

import cn.com.nla.common.freeswitch.model.MessageModel;
import link.thingscloud.freeswitch.esl.InboundClient;

public abstract class StrategyHandler {
    //esl客户端
    protected final InboundClient inboundClient;

    public StrategyHandler(InboundClient inboundClient){
        this.inboundClient = inboundClient;
    }


    public abstract void handler(MessageModel model);
}
